package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderStatusCountResponse {
    private String status;
    private long count;
}