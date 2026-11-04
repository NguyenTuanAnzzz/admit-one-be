package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// Trang chi tiết sự kiện và kết quả trả về của API admin. Khi có Zone thì thêm danh sách zones (bảng giá theo khu)
@Getter
@AllArgsConstructor
public class EventDetailResponse {
    private UUID id;
    private String name;
    private String venue;
    private String address;
    private String googleMapsUrl;
    private String description;
    private String posterUrl;
    private Instant startsAt;
    private Instant saleOpensAt;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
