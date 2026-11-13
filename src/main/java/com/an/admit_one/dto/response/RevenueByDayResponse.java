package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RevenueByDayResponse {
    private String date; // "2026-11-14"
    private BigDecimal revenue;
}