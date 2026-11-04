package com.an.admit_one.controller;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.request.SeatMapRequest;
import com.an.admit_one.dto.request.UpdateEventStatusRequest;
import com.an.admit_one.dto.response.EventDetailResponse;
import com.an.admit_one.dto.response.EventPageResponse;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.dto.response.SeatMapResponse;
import com.an.admit_one.dto.response.SeatResponse;
import com.an.admit_one.service.EventService;
import com.an.admit_one.service.SeatMapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private SeatMapService seatMapService;

    // ===== Khách: không cần đăng nhập, không có @PreAuthorize. Chỉ trả sự kiện đã công bố =====

    /**
     * Danh sách sự kiện cho trang /events. Không có bản nháp và sự kiện đã huỷ.
     * ?status=ON_SALE&q=concert&startsFrom=...&startsTo=...&page=0&size=12&sort=startsAt
     */
    @GetMapping("/events")
    public ResponseEntity<EventPageResponse> searchForPublic(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant startsFrom,
            @RequestParam(required = false) Instant startsTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "startsAt") String sort
    ) {
        return ResponseEntity.ok(eventService.searchForPublic(status, q, startsFrom, startsTo, page, size, sort));
    }

    // Chi tiết sự kiện. Bản nháp 404, đã huỷ vẫn xem được
    @GetMapping("/events/{id}")
    public ResponseEntity<EventDetailResponse> getForPublic(@PathVariable UUID id) {

        return ResponseEntity.ok(eventService.getForPublic(id));
    }

    // Sơ đồ: hạng vé, khu kèm số ghế còn trống, vật thể. Không có ảnh nền
    @GetMapping("/events/{id}/seatmap")
    public ResponseEntity<SeatMapResponse> getSeatMapForPublic(@PathVariable UUID id) {

        return ResponseEntity.ok(seatMapService.getForPublic(id));
    }

    // Ghế của một khu kèm trạng thái, để khách phóng vào khu chọn ghế
    @GetMapping("/events/{id}/zones/{zoneId}/seats")
    public ResponseEntity<List<SeatResponse>> getZoneSeatsForPublic(
            @PathVariable UUID id,
            @PathVariable UUID zoneId
    ) {
        return ResponseEntity.ok(seatMapService.getSeatsForPublic(id, zoneId));
    }

    // ===== Admin: hàm nào cũng phải có @PreAuthorize =====

    /**
     * Mọi sự kiện kể cả bản nháp, cho trang Dashboard › Sự kiện. Mọi tham số đều không bắt buộc.
     * Ví dụ lọc sự kiện diễn ngày 20/12 (giờ Hà Nội):
     * ?startsFrom=2026-12-19T17:00:00Z&startsTo=2026-12-20T16:59:59Z&page=0&size=10&sort=-startsAt
     * Ngày mở bán lọc tương tự bằng saleFrom, saleTo.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events")
    public ResponseEntity<EventPageResponse> searchForAdmin(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant startsFrom,
            @RequestParam(required = false) Instant startsTo,
            @RequestParam(required = false) Instant saleFrom,
            @RequestParam(required = false) Instant saleTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startsAt") String sort

    ) {
        return ResponseEntity.ok(eventService.searchForAdmin(status, q, startsFrom, startsTo, saleFrom, saleTo, page, size, sort));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/events")
    public ResponseEntity<EventDetailResponse> create(@Valid @RequestBody EventRequest request) {

        EventDetailResponse response = eventService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Chi tiết để điền sẵn form sửa
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events/{id}")
    public ResponseEntity<EventDetailResponse> getForAdmin(@PathVariable UUID id) {

        return ResponseEntity.ok(eventService.getForAdmin(id));
    }

    // Sửa thông tin, body giống hệt lúc tạo
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/events/{id}")
    public ResponseEntity<EventDetailResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request
    ) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    // Đổi trạng thái: công bố, mở bán, huỷ. Bước chuyển không hợp lệ thì 409
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/events/{id}/status")
    public ResponseEntity<EventDetailResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventStatusRequest request
    ) {
        return ResponseEntity.ok(eventService.changeStatus(id, request.getStatus()));
    }

    // Lưu cả sơ đồ: xoá sơ đồ cũ rồi lưu sơ đồ gửi lên. Chỉ khi sự kiện còn DRAFT
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/events/{id}/seatmap")
    public ResponseEntity<SeatMapResponse> saveSeatMap(
            @PathVariable UUID id,
            @Valid @RequestBody SeatMapRequest request
    ) {
        SeatMapResponse response = seatMapService.save(id, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events/{id}/seatmap")
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable UUID id) {

        return ResponseEntity.ok(seatMapService.get(id));
    }

    // Ghế của một khu, cho trình dựng sơ đồ phóng vào khu
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/events/{id}/zones/{zoneId}/seats")
    public ResponseEntity<List<SeatResponse>> getZoneSeats(
            @PathVariable UUID id,
            @PathVariable UUID zoneId
    ) {
        return ResponseEntity.ok(seatMapService.getSeats(id, zoneId));
    }
}
