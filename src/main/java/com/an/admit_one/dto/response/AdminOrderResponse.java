package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminOrderResponse {
    private UUID id;
    private String code;
    private String status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant paidAt;

    // Để admin biết đơn của ai, sự kiện nào mà không cần gọi thêm API
    private String customerName;
    private String customerEmail;
    private String eventName;
}