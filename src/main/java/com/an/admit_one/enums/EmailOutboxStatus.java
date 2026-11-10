package com.an.admit_one.enums;

public enum EmailOutboxStatus {

    // Chờ gửi (mới tạo, hoặc lần trước gửi lỗi và còn được thử lại)
    PENDING,

    // Đã gửi thành công
    SENT,

    // Thử quá số lần cho phép vẫn lỗi, không gửi nữa
    FAILED
}
