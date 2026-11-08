package com.an.admit_one.controller;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.request.UpdateEventRequest;
import com.an.admit_one.dto.response.EventDetailResponse;
import com.an.admit_one.dto.response.EventResponse;
import com.an.admit_one.dto.response.TicketResponse;
import com.an.admit_one.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    @Autowired
    private EventService eventService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events")
    public ResponseEntity<Page<EventResponse>> getAllEventsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<EventResponse> response = eventService.getAllEventsForAdmin(
                keyword, eventDate, saleDate, page, size
        );

        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/events/{eventId}/publish")
    public ResponseEntity<EventResponse> makeEventPublic(
            @PathVariable UUID eventId
    ) {
        EventResponse response = eventService.makeEventPublic(eventId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(
            @PathVariable UUID eventId
    ) {
        EventDetailResponse response = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/events/{eventId}")
    public ResponseEntity<EventDetailResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventDetailResponse response =
                eventService.updateEvent(eventId, request);

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId
    ) {
        eventService.deleteEvent(eventId);

        return ResponseEntity.noContent().build();
    }

}
