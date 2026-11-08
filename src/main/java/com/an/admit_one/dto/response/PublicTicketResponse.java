package com.an.admit_one.dto.response;

import com.an.admit_one.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PublicTicketResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;

    private Integer available;

    private TicketStatus status;
}