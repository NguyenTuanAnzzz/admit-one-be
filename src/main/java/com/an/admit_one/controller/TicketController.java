package com.an.admit_one.controller;

import com.an.admit_one.dto.request.TicketRequest;
import com.an.admit_one.dto.response.TicketResponse;
import com.an.admit_one.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/events/{eventId}/tickets/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody TicketRequest request
    ) {
        TicketResponse response = ticketService.updateTicket(
                eventId,
                ticketId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/events/{eventId}/tickets")
    public ResponseEntity<TicketResponse> addTicket(
            @PathVariable UUID eventId,
            @Valid @RequestBody TicketRequest request
    ) {
        TicketResponse response = ticketService.addTicket(
                eventId,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}