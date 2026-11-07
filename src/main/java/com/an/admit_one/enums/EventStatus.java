package com.an.admit_one.enums;

public enum EventStatus {

    // Sự kiện đang được tạo, chưa công khai cho khách hàng
    DRAFT,

    // Sự kiện đã công bố nhưng chưa đến thời điểm mở bán vé
    UPCOMING,

    // Sự kiện đang trong thời gian mở bán vé
    ON_SALE,

    // Tất cả các loại vé có thể bán đều đã hết
    SOLD_OUT,

    // Sự kiện bị huỷ, không xoá dữ liệu để xử lý hoàn tiền
    CANCELLED
}