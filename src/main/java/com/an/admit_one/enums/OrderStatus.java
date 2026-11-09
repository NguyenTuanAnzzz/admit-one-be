package com.an.admit_one.enums;

public enum OrderStatus {

    // Đơn hàng đang chờ thanh toán
    PENDING,

    // Đơn hàng đã thanh toán thành công
    PAID,

    // Đơn hàng đã bị huỷ
    CANCELLED,

    // Đơn hàng đã hết hạn thanh toán
    EXPIRED
}
