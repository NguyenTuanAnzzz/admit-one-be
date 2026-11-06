package com.an.admit_one.dto.response;

import com.an.admit_one.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private UUID id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer quantity;

    private Integer soldQuantity;

    private TicketStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}