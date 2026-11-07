package com.an.admit_one.service;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.response.EventDetailResponse;
import com.an.admit_one.dto.response.EventResponse;
import com.an.admit_one.dto.response.TicketResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.Ticket;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.enums.TicketStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import com.an.admit_one.specification.EventSpecifications;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public EventResponse createEvent(EventRequest request) {
        if (eventRepository.existsByNameIgnoreCaseAndStartsAt(
                request.getName(),
                request.getStartsAt()
        )) {
            throw new AppException(HttpStatus.CONFLICT, "Sự kiện đã tồn tại");
        }

        if (!request.getSaleOpensAt().isBefore(request.getStartsAt())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian mở bán vé phải trước thời gian bắt đầu sự kiện"
            );
        }

        if (!request.getStartsAt().isAfter(Instant.now())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu sự kiện phải ở tương lai"
            );
        }

        Event event = new Event();

        event.setName(request.getName());
        event.setVenue(request.getVenue());
        event.setAddress(request.getAddress());
        event.setGoogleMapsUrl(request.getGoogleMapsUrl());
        event.setDescription(request.getDescription());
        event.setPosterUrl(request.getPosterUrl());
        event.setStartsAt(request.getStartsAt());
        event.setSaleOpensAt(request.getSaleOpensAt());
        event.setStatus(EventStatus.DRAFT);
        List<Ticket> ticketList = new ArrayList<>();
        Set<String> ticketNames = new HashSet<>();

        if (request.getTickets() != null && !request.getTickets().isEmpty()) {

            for (TicketRequest tr : request.getTickets()) {
                String ticketName = tr.getName().trim().toLowerCase();
                if (!ticketNames.add(ticketName)) {
                    throw new AppException(
                            HttpStatus.CONFLICT,
                            "Tên loại vé bị trùng: " + tr.getName()
                    );
                }
                Ticket ticket = new Ticket();
                ticket.setName(tr.getName().trim());
                ticket.setDescription(tr.getDescription());
                ticket.setPrice(tr.getPrice());
                ticket.setQuantity(tr.getQuantity());
                ticket.setEvent(event);
                ticket.setStatus(TicketStatus.INACTIVE);
                ticketList.add(ticket);
            }
            event.setTickets(ticketList);
        }

        Event savedEvent = eventRepository.save(event);

        return new EventResponse(
                savedEvent.getId(),
                savedEvent.getName(),
                savedEvent.getVenue(),
                savedEvent.getAddress(),
                savedEvent.getGoogleMapsUrl(),
                savedEvent.getDescription(),
                savedEvent.getPosterUrl(),
                savedEvent.getStartsAt(),
                savedEvent.getSaleOpensAt(),
                savedEvent.getStatus(),
                savedEvent.getCreatedAt(),
                savedEvent.getUpdatedAt()
        );


    }

    public Page<EventResponse> getAllEventsForAdmin(
            String keyword,
            LocalDate eventDate,
            LocalDate saleDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Specification<Event> spec = Specification
                .where(EventSpecifications.search(keyword))
                .and(EventSpecifications.hasEventDate(eventDate))
                .and(EventSpecifications.hasSaleDate(saleDate));

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        return eventPage.map(event -> new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getAddress(),
                event.getGoogleMapsUrl(),
                event.getDescription(),
                event.getPosterUrl(),
                event.getStartsAt(),
                event.getSaleOpensAt(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        ));
    }

    @Transactional
    public EventResponse makeEventPublic(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện"
                ));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể công bố sự kiện ở trạng thái DRAFT"
            );
        }
        event.setStatus(EventStatus.UPCOMING);
        // Giờ mở bán phải chưa tới, vì công bố chỉ đưa sự kiện sang UPCOMING
        if (!event.getSaleOpensAt().isAfter(Instant.now())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Giờ mở bán đã qua hoặc đang diễn ra, hãy sửa giờ mở bán ở tương lai trước khi công bố"
            );
        }

        // Giờ diễn phải sau giờ mở bán (kéo theo giờ diễn ở tương lai)
        if (!event.getStartsAt().isAfter(event.getSaleOpensAt())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Giờ diễn phải sau giờ mở bán"
            );
        }

        // Phải có ít nhất 1 loại vé
        if (event.getTickets() == null || event.getTickets().isEmpty()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Sự kiện cần có ít nhất 1 loại vé trước khi công bố"
            );
        }

        // Nếu muốn ép "ít nhất 1 vé ACTIVE" thì kiểm tra sau bước kích hoạt bên dưới
        event.setStatus(EventStatus.UPCOMING);

        for (Ticket ticket : event.getTickets()) {
            ticket.setStatus(TicketStatus.INACTIVE);
        }

        eventRepository.save(event);

        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getAddress(),
                event.getGoogleMapsUrl(),
                event.getDescription(),
                event.getPosterUrl(),
                event.getStartsAt(),
                event.getSaleOpensAt(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );


    }

    public EventDetailResponse getEventDetail(UUID eventId) {
        Event event = eventRepository.findByIdWithTickets(eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện"
                ));

        List<TicketResponse> ticketResponseList = new ArrayList<>();
        if (event.getTickets() != null && !event.getTickets().isEmpty()) {
            for (Ticket ticket : event.getTickets()) {
                TicketResponse ticketResponse = new TicketResponse(
                        ticket.getId(),
                        ticket.getName(),
                        ticket.getDescription(),
                        ticket.getPrice(),
                        ticket.getQuantity(),
                        ticket.getSoldQuantity(),
                        ticket.getStatus(),
                        ticket.getCreatedAt(),
                        ticket.getUpdatedAt()
                );
                ticketResponseList.add(ticketResponse);
            }
        }
        return new EventDetailResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getAddress(),
                event.getGoogleMapsUrl(),
                event.getDescription(),
                event.getPosterUrl(),
                event.getStartsAt(),
                event.getSaleOpensAt(),
                event.getStatus(),
                ticketResponseList,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    @Transactional
    public EventDetailResponse updateEvent(UUID eventId, @Valid EventRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện"
                ));

        Instant now = Instant.now();

        // Không cho chỉnh sửa sự kiện đã huỷ hoặc đã diễn ra
        if (event.getStatus() == EventStatus.CANCELLED
                || !event.getStartsAt().isAfter(now)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Sự kiện đã kết thúc hoặc đã huỷ, không thể chỉnh sửa"
            );
        }

        // Kiểm tra thời gian sự kiện mới
        if (request.getStartsAt() == null
                || !request.getStartsAt().isAfter(now)) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian diễn ra sự kiện phải ở tương lai"
            );
        }

        if (request.getSaleOpensAt() == null
                || !request.getSaleOpensAt().isBefore(request.getStartsAt())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian mở bán phải trước thời gian diễn ra sự kiện"
            );
        }

        if (event.getStatus() == EventStatus.DRAFT) {

            // DRAFT: Cho phép sửa toàn bộ thông tin Event
            event.setName(request.getName());
            event.setDescription(request.getDescription());
            event.setPosterUrl(request.getPosterUrl());
            event.setVenue(request.getVenue());
            event.setAddress(request.getAddress());
            event.setGoogleMapsUrl(request.getGoogleMapsUrl());
            event.setStartsAt(request.getStartsAt());
            event.setSaleOpensAt(request.getSaleOpensAt());

        } else if (event.getStatus() == EventStatus.UPCOMING) {

            // UPCOMING: Chưa đến giờ mở bán thì được thay đổi
            if (!event.getSaleOpensAt().isAfter(now)) {
                throw new AppException(
                        HttpStatus.CONFLICT,
                        "Sự kiện đã đến giờ mở bán, không thể chỉnh sửa"
                );
            }

            // Giờ mở bán mới phải ở tương lai
            if (!request.getSaleOpensAt().isAfter(now)) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Giờ mở bán mới phải ở tương lai"
                );
            }

            event.setName(request.getName());
            event.setDescription(request.getDescription());
            event.setPosterUrl(request.getPosterUrl());
            event.setVenue(request.getVenue());
            event.setAddress(request.getAddress());
            event.setGoogleMapsUrl(request.getGoogleMapsUrl());
            event.setStartsAt(request.getStartsAt());
            event.setSaleOpensAt(request.getSaleOpensAt());

        } else if (event.getStatus() == EventStatus.ON_SALE
                || event.getStatus() == EventStatus.SOLD_OUT) {

            // Không cho thay đổi giờ mở bán sau khi đã mở bán
            if (!Objects.equals(
                    event.getSaleOpensAt(),
                    request.getSaleOpensAt())) {

                throw new AppException(
                        HttpStatus.CONFLICT,
                        "Sự kiện đã mở bán, không thể thay đổi giờ mở bán"
                );
            }

            // ON_SALE / SOLD_OUT: Cho sửa thông tin Event
            event.setName(request.getName());
            event.setDescription(request.getDescription());
            event.setPosterUrl(request.getPosterUrl());
            event.setVenue(request.getVenue());
            event.setAddress(request.getAddress());
            event.setGoogleMapsUrl(request.getGoogleMapsUrl());
            event.setStartsAt(request.getStartsAt());
        }

        eventRepository.save(event);

        return new EventDetailResponse(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getAddress(),
                event.getGoogleMapsUrl(),
                event.getDescription(),
                event.getPosterUrl(),
                event.getStartsAt(),
                event.getSaleOpensAt(),
                event.getStatus(),
                event.getTickets().stream()
                        .map(ticket -> new TicketResponse(
                                ticket.getId(),
                                ticket.getName(),
                                ticket.getDescription(),
                                ticket.getPrice(),
                                ticket.getQuantity(),
                                ticket.getSoldQuantity(),
                                ticket.getStatus(),
                                ticket.getCreatedAt(),
                                ticket.getUpdatedAt()
                        ))
                        .toList(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }


}
