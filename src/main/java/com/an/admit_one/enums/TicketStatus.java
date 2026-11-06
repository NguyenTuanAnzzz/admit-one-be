package com.an.admit_one.enums;

public enum TicketStatus {

    // Vé đang hoạt động, có thể được khách hàng mua
    ACTIVE,

    // Vé đã bị vô hiệu hóa, không cho phép khách hàng mua
    INACTIVE,

    // Vé đã được bán hết, không còn vé để mua
    SOLD_OUT
}