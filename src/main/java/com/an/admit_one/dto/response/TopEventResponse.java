package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TopEventResponse {
    private String eventName;
    private BigDecimal revenue;
}