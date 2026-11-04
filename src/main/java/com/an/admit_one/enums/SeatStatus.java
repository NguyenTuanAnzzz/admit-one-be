package com.an.admit_one.enums;

// Máy trạng thái ghế (spec mục 6): AVAILABLE → HELD → SOLD, HELD → AVAILABLE khi hết hạn hoặc huỷ
public enum SeatStatus {
    AVAILABLE,
    HELD,
    SOLD,
    // Admin khoá: ghế khách mời, ghế kỹ thuật (A-05)
    BLOCKED
}
