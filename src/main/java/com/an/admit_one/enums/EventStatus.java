package com.an.admit_one.enums;

public enum EventStatus {

    // Sự kiện đang được tạo, chưa công khai
    DRAFT,

    // Đã công bố, chưa đến giờ mở bán
    UPCOMING,

    // Đang mở bán vé
    ON_SALE,

    // Tất cả các loại vé có thể bán đều đã hết
    SOLD_OUT,

    // Sự kiện đã kết thúc
    ENDED,

    // Sự kiện bị huỷ
    CANCELLED
}