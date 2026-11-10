package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// Một tấm vé đã xuất của khách. qrSecret chỉ trả cho chủ đơn: FE vẽ QR từ chuỗi này, Staff quét để check-in
@Getter
@AllArgsConstructor
public class IssuedTicketResponse {

    private UUID id;
    private String code;
    private String ticketName;
    private String status;
    private String qrSecret;
    private Instant checkedInAt;
}
