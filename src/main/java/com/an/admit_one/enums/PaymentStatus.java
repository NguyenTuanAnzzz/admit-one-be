package com.an.admit_one.enums;

public enum PaymentStatus {

    // Cổng báo thanh toán thành công
    SUCCESS,

    // Cổng báo thất bại hoặc khách huỷ
    FAILED,

    // Tiền đã về nhưng không còn vé để giữ cho đơn (trả tiền sau khi hết hạn): cần hoàn tiền
    REFUND_REQUIRED
}