package com.an.admit_one.dto.response;

import com.an.admit_one.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PublicEventSummaryResponse {

    private UUID id;
    private String name;
    private String venue;
    private String posterUrl;

    private Instant startsAt;
    private Instant saleOpensAt;

    private EventStatus status;

    private BigDecimal priceFrom;
}