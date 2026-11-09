package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderItemResponse {

    private UUID ticketId;
    private String ticketName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}