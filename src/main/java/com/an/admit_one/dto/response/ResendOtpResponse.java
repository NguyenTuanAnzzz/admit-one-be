package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ResendOtpResponse {
    private String message;
    private Instant otpExpiresAt;
}
