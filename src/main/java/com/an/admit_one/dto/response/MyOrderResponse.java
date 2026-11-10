package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MyOrderResponse {

    private UUID id;
    private String code;
    private String status;
    private BigDecimal totalAmount;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant paidAt;

    // Thông tin sự kiện để hiện thẻ đơn hàng, không phải gọi thêm API sự kiện
    private UUID eventId;
    private String eventName;
    private String eventPosterUrl;
    private Instant eventStartsAt;
    private String eventVenue;

    private List<OrderItemResponse> items;
}