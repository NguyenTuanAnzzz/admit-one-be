package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// Một thẻ sự kiện trong danh sách. Khi có Zone thì thêm priceFrom, priceTo, totalSeats (tính từ các khu)
@Getter
@AllArgsConstructor
public class EventSummaryResponse {
    private UUID id;
    private String name;
    private String venue;
    private String address;
    private String posterUrl;
    private Instant startsAt;
    private Instant saleOpensAt;
    private String status;
}
