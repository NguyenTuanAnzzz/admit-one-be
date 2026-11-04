package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

// Một ghế trong khu, dùng cho GET ghế theo khu. x, y tương đối với khu
@Getter
@AllArgsConstructor
public class SeatResponse {
    private UUID id;
    private String rowLabel;
    private Integer seatNumber;
    private Double x;
    private Double y;
    private String status;
}
