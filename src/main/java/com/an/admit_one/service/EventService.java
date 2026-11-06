package com.an.admit_one.service;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.response.EventResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.Ticket;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import com.an.admit_one.specification.EventSpecifications;
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

        if (request.getTickets() != null && !request.getTickets().isEmpty()){

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

    public EventResponse publishEvent(UUID eventId) {
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
        event.setStatus(EventStatus.PUBLISHED);

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
}
