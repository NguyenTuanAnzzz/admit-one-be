package com.an.admit_one.service;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.response.EventResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.Ticket;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

        if (request.getTickets() != null && !request.getTickets().isEmpty()){
            for (TicketRequest tr : request.getTickets()) {
                Ticket ticket = new Ticket();
                ticket.setName(tr.getName());
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
}
