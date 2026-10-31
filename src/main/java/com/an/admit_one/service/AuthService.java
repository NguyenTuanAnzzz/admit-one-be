package com.an.admit_one.service;

import com.an.admit_one.dto.request.RegisterRequest;
import com.an.admit_one.dto.response.AuthResponse;
import com.an.admit_one.entity.Otp;
import com.an.admit_one.entity.User;
import com.an.admit_one.enums.OtpType;
import com.an.admit_one.enums.UserRole;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.AuthRepository;

import com.an.admit_one.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpRepository otpRepository;

    private String generateOtp() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();

        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            otp.append(chars.charAt(random.nextInt(chars.length())));
        }

        return otp.toString();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (authRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Email đã được đăng ký"
            );
        }
        String phone = request.getPhone();

        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();

            if (authRepository.existsByPhone(phone)) {
                throw new AppException(
                        HttpStatus.CONFLICT,
                        "Số điện thoại đã được đăng ký"
                );
            }
        } else {
            phone = null;
        }

        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(UserRole.CUSTOMER);

        User savedUser = authRepository.save(user);

        String otpCode = generateOtp();

        Otp otp = new Otp();
        otp.setUserId(savedUser.getId());
        otp.setCode(otpCode);
        otp.setType(OtpType.VERIFY_EMAIL);
        otp.setExpiresAt(Instant.now().plusSeconds(15 * 60));
        otp.setUsed(false);

        otpRepository.save(otp);

        try {
            emailService.sendOtpEmail(
                    savedUser.getEmail(),
                    otpCode,
                    OtpType.VERIFY_EMAIL
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole(),
                null
        );
    }
}