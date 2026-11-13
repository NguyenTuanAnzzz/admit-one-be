package com.an.admit_one.controller;

import com.an.admit_one.dto.response.OrderStatusCountResponse;
import com.an.admit_one.dto.response.OverviewResponse;
import com.an.admit_one.dto.response.RevenueByDayResponse;
import com.an.admit_one.dto.response.TopEventResponse;
import com.an.admit_one.service.OverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/overview")
@PreAuthorize("hasRole('ADMIN')")
public class OverviewController {

    @Autowired
    private OverviewService overviewService;

    @GetMapping
    public ResponseEntity<OverviewResponse> getOverview() {
        return ResponseEntity.ok(overviewService.getOverview());
    }

    @GetMapping("/revenue-by-day")
    public ResponseEntity<List<RevenueByDayResponse>> getRevenueByDay(@RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(overviewService.getRevenueByDay(days));
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<OrderStatusCountResponse>> getOrdersByStatus() {
        return ResponseEntity.ok(overviewService.getOrdersByStatus());
    }

    @GetMapping("/top-events")
    public ResponseEntity<List<TopEventResponse>> getTopEvents(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(overviewService.getTopEvents(limit));
    }
}