package com.an.admit_one.service;

import com.an.admit_one.dto.request.RegisterRequest;
import com.an.admit_one.dto.request.ResendOtpRequest;
import com.an.admit_one.dto.request.VerifyEmailRequest;
import com.an.admit_one.dto.response.AuthResponse;
import com.an.admit_one.entity.Otp;
import com.an.admit_one.entity.User;
import com.an.admit_one.enums.OtpType;
import com.an.admit_one.enums.UserRole;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.AuthRepository;

import com.an.admit_one.repository.OtpRepository;
import jakarta.validation.Valid;
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


        User user = authRepository.findByEmail(email).orElse(null);

        if (user != null && user.getEmailVerifiedAt() != null) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Email đã được đăng ký"
            );
        }

        if (user == null) {
            user = new User();
        }

        String phone = request.getPhone();

        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();

            User phoneOwner = authRepository.findByPhone(phone).orElse(null);

            if (phoneOwner != null && !phoneOwner.getId().equals(user.getId())) {
                if (phoneOwner.getEmailVerifiedAt() != null) {
                    throw new AppException(
                            HttpStatus.CONFLICT,
                            "Số điện thoại đã được đăng ký"
                    );
                }

                phoneOwner.setPhone(null);
                authRepository.saveAndFlush(phoneOwner);
            }
        } else {
            phone = null;
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(UserRole.CUSTOMER);

        User savedUser = authRepository.save(user);

        otpRepository.deleteAllByUserIdAndTypeAndUsedFalse(
                savedUser.getId(),
                OtpType.VERIFY_EMAIL
        );

        String otpCode = generateOtp();

        Otp otp = new Otp();
        otp.setUserId(savedUser.getId());
        otp.setCode(otpCode);
        otp.setType(OtpType.VERIFY_EMAIL);
        otp.setExpiresAt(Instant.now().plusSeconds(EmailService.OTP_VALID_MINUTES * 60L));
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
                null,
                otp.getExpiresAt()
        );
    }

    @Transactional
    public void verifyEmail(@Valid VerifyEmailRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);
        String code = request.getCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Email không tồn tại"
                ));

        if (user.getEmailVerifiedAt() != null) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Email đã được xác minh"
            );
        }

        Otp otp = otpRepository
                .findByUserIdAndCodeAndTypeAndUsedFalse(
                        user.getId(),
                        code,
                        OtpType.VERIFY_EMAIL
                )
                .orElseThrow(() -> new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Mã xác minh không đúng"
                ));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Mã xác minh đã hết hạn"
            );
        }

        otpRepository.delete(otp);

        user.setEmailVerifiedAt(Instant.now());
        authRepository.save(user);
    }

    @Transactional
    public Instant resendOtp(@Valid ResendOtpRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Email không tồn tại"
                ));

        if (user.getEmailVerifiedAt() != null) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Email đã được xác minh"
            );
        }
        if (otpRepository.existsByUserIdAndTypeAndUsedFalseAndExpiresAtAfter(
                user.getId(),
                OtpType.VERIFY_EMAIL,
                Instant.now()
        )) {
            throw new AppException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Mã cũ vẫn còn hiệu lực, vui lòng chờ mã hết hạn rồi gửi lại"
            );
        }

        otpRepository.deleteAllByUserIdAndTypeAndUsedFalse(
                user.getId(),
                OtpType.VERIFY_EMAIL
        );

        String otpCode = generateOtp();

        Otp otp = new Otp();
        otp.setUserId(user.getId());
        otp.setCode(otpCode);
        otp.setType(OtpType.VERIFY_EMAIL);
        otp.setExpiresAt(Instant.now().plusSeconds(EmailService.OTP_VALID_MINUTES * 60L));
        otp.setUsed(false);

        otpRepository.save(otp);

        emailService.sendOtpEmail(
                user.getEmail(),
                otpCode,
                OtpType.VERIFY_EMAIL
        );

        return otp.getExpiresAt();
    }
}