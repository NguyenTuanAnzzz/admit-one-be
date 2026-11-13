package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PaymentLogResponse {
    private UUID id;
    private String gateway;
    private String gatewayTxnRef;
    private BigDecimal amount;
    private String status;
    private String rawPayload;
    private Instant createdAt;
}