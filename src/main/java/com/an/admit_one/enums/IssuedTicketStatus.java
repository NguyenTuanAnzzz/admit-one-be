package com.an.admit_one.enums;

public enum IssuedTicketStatus {

    // Vé hợp lệ, chưa vào cửa
    VALID,

    // Đã quét QR vào cửa, không dùng lại được
    CHECKED_IN,

    // Vé bị huỷ (hoàn tiền, sự kiện huỷ)
    VOID
}