package com.an.admit_one.service;

import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.response.TicketResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.Ticket;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.enums.TicketStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import com.an.admit_one.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Transactional
    public TicketResponse updateTicket(
            UUID eventId,
            UUID ticketId,
            TicketRequest request
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện"
                ));

        Ticket ticket = ticketRepository.findByIdAndEventId(ticketId, eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy vé trong sự kiện này"
                ));

        Instant now = Instant.now();

        boolean saleStarted = event.getStatus() == EventStatus.ON_SALE
                || event.getStatus() == EventStatus.SOLD_OUT
                || (event.getStatus() != EventStatus.DRAFT
                && event.getSaleOpensAt() != null
                && !now.isBefore(event.getSaleOpensAt()));

        if (event.getStatus() == EventStatus.CANCELLED || saleStarted) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Sự kiện đã mở bán hoặc đã huỷ, không thể sửa loại vé"
            );
        }

        if (ticket.getStatus() != TicketStatus.INACTIVE) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể cập nhật vé đang ở trạng thái INACTIVE"
            );
        }

        if (event.getStatus() != EventStatus.DRAFT
                && request.getQuantity() < ticket.getSoldQuantity()) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Số lượng vé không được nhỏ hơn số vé đã bán"
            );
        }

        // Chuẩn hóa tên vé
        String ticketName = request.getName().trim();

        // Kiểm tra trùng tên, loại trừ chính vé đang cập nhật
        if (ticketRepository.existsByEventIdAndNameIgnoreCaseAndIdNot(
                eventId,
                ticketName,
                ticketId
        )) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Tên loại vé đã tồn tại trong sự kiện này"
            );
        }

        ticket.setName(ticketName);
        ticket.setDescription(request.getDescription());
        ticket.setPrice(request.getPrice());
        ticket.setQuantity(request.getQuantity());

        ticketRepository.save(ticket);

        return new TicketResponse(
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
    }


    @Transactional
    public TicketResponse addTicket(UUID eventId, TicketRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sự kiện"
                ));

        if (event.getStatus() != EventStatus.DRAFT
                && (event.getStatus() != EventStatus.UPCOMING
                || !Instant.now().isBefore(event.getSaleOpensAt()))) {

            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Không thể thêm vé sau khi sự kiện mở bán"
            );
        }

        // Chuẩn hóa tên vé
        String ticketName = request.getName().trim();

        // Kiểm tra trùng tên trong cùng sự kiện
        if (ticketRepository.existsByEventIdAndNameIgnoreCase(
                eventId,
                ticketName
        )) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Tên loại vé đã tồn tại trong sự kiện này"
            );
        }

        Ticket ticket = new Ticket();

        ticket.setName(ticketName);
        ticket.setDescription(request.getDescription());
        ticket.setPrice(request.getPrice());
        ticket.setQuantity(request.getQuantity());
        ticket.setSoldQuantity(0);
        ticket.setStatus(TicketStatus.INACTIVE);

        event.getTickets().add(ticket);
        ticket.setEvent(event);

        ticketRepository.save(ticket);

        return new TicketResponse(
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
    }

    @Transactional
    public void deleteTicket(UUID eventId, UUID ticketId) {

        Ticket ticket = ticketRepository.findByIdAndEventId(ticketId, eventId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy vé trong sự kiện"
                ));

        if (ticket.getEvent().getStatus() != EventStatus.DRAFT) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Chỉ được xóa vé khi sự kiện đang ở trạng thái DRAFT"
            );
        }

        if (ticket.getSoldQuantity() > 0) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa loại vé đã có vé được bán"
            );
        }

        ticketRepository.delete(ticket);
    }
}