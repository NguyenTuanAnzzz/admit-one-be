package com.an.admit_one.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ScanTicketRequest {

    @NotNull(message = "Vui lòng chọn sự kiện")
    private UUID eventId;

    @NotBlank(message = "Thiếu mã QR")
    private String qrSecret;
}