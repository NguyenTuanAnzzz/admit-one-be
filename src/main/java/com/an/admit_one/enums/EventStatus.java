package com.an.admit_one.enums;

public enum EventStatus {

    // Sự kiện đang được tạo, chưa công khai cho khách hàng
    DRAFT,

    // Sự kiện đã được công khai
    PUBLISHED,

    // Tất cả các loại vé có thể bán đều đã hết
    SOLD_OUT,

    // Sự kiện bị huỷ, không xoá dữ liệu để xử lý hoàn tiền
    CANCELLED
}