package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CreateOrderResponse {

    private UUID id;
    private String code;
    private UUID eventId;
    private String eventName;
    private BigDecimal totalAmount;
    private String status;
    private Instant expiresAt;
    private List<OrderItemResponse> items;

    // Số giây còn giữ vé, tính bằng giờ server lúc trả response. FE đếm ngược từ số này, không so expiresAt với giờ máy khách
    private long remainingSeconds;
}