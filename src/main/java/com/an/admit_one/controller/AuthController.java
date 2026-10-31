package com.an.admit_one.controller;

import com.an.admit_one.dto.request.RegisterRequest;
import com.an.admit_one.dto.request.ResendOtpRequest;
import com.an.admit_one.dto.request.VerifyEmailRequest;
import com.an.admit_one.dto.response.AuthResponse;
import com.an.admit_one.dto.response.MessageResponse;
import com.an.admit_one.dto.response.ResendOtpResponse;
import com.an.admit_one.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        authService.verifyEmail(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new MessageResponse("Xác minh email thành công"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ResendOtpResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request
    ) {
        Instant otpExpiresAt = authService.resendOtp(request);

        return ResponseEntity.ok(
                new ResendOtpResponse("Đã gửi lại mã xác minh", otpExpiresAt)
        );
    }
}