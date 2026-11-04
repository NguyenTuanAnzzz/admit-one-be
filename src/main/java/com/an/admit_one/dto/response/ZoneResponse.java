package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ZoneResponse {
    private UUID id;
    private UUID tierId;
    private String label;
    // Service đọc chuỗi JSON trong cột shape ra lại danh sách điểm [[x, y], ...]
    private List<List<Double>> shape;
    private Double x;
    private Double y;
    private Double rotation;
    private String seatingMode;
    private Integer rowCount;
    private Integer seatsPerRow;
    private String seatLayout;
    // Đếm từ bảng seats: tổng ghế, ghế còn bán được (AVAILABLE), ghế khoá (BLOCKED)
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer blockedSeats;
}
