package com.an.admit_one.controller;

import com.an.admit_one.dto.request.ScanTicketRequest;
import com.an.admit_one.dto.response.CheckinEventResponse;
import com.an.admit_one.dto.response.CheckinStatsResponse;
import com.an.admit_one.dto.response.ScanTicketResponse;
import com.an.admit_one.service.CheckinService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkin")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    @GetMapping("/events")
    public ResponseEntity<List<CheckinEventResponse>> listEvents(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(checkinService.listEvents(keyword));
    }

    @GetMapping("/events/{eventId}/stats")
    public ResponseEntity<CheckinStatsResponse> getStats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(checkinService.getStats(eventId));
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanTicketResponse> scan(@Valid @RequestBody ScanTicketRequest request) {
        return ResponseEntity.ok(checkinService.scan(request));
    }
}