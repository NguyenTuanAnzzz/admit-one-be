package com.an.admit_one.service;

import com.an.admit_one.dto.request.LoginRequest;
import com.an.admit_one.dto.request.RegisterRequest;
import com.an.admit_one.dto.request.ResendOtpRequest;
import com.an.admit_one.dto.request.VerifyEmailRequest;
import com.an.admit_one.dto.response.AuthResponse;
import com.an.admit_one.dto.response.LoginResponse;
import com.an.admit_one.entity.Otp;
import com.an.admit_one.entity.User;
import com.an.admit_one.enums.OtpType;
import com.an.admit_one.enums.UserRole;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.AuthRepository;

import com.an.admit_one.repository.OtpRepository;
import com.an.admit_one.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;

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

    public LoginResponse login(
            @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail().trim().toLowerCase(Locale.ROOT),
                                request.getPassword()
                        )
                );

        UserPrincipal userPrincipal =
                (UserPrincipal) authentication.getPrincipal();

        if (userPrincipal.getUser().getEmailVerifiedAt() == null) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "Email chưa được xác minh"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(userPrincipal);

        String refreshToken =
                jwtService.generateRefreshToken(userPrincipal);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // true khi chạy HTTPS
                .sameSite("Lax")
                .path("/api/auth/refresh");

        // Ghi nhớ đăng nhập: cookie sống 7 ngày. Không ghi nhớ: cookie phiên, đóng trình duyệt là mất
        if (request.isRememberMe()) {
            cookieBuilder.maxAge(7 * 24 * 60 * 60);
        }

        ResponseCookie refreshCookie = cookieBuilder.build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        return new LoginResponse(accessToken);
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không tồn tại"
            );
        }

        try {
            if (!"REFRESH".equals(jwtService.extractTokenType(refreshToken))) {
                throw new AppException(
                        HttpStatus.UNAUTHORIZED,
                        "Token không phải refresh token"
                );
            }

            String email = jwtService.extractUsername(refreshToken);

            UserPrincipal userPrincipal =
                    (UserPrincipal) customerUserDetailsService
                            .loadUserByUsername(email);

            if (!jwtService.isTokenValid(refreshToken, userPrincipal)) {
                throw new AppException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token không hợp lệ hoặc đã hết hạn"
                );
            }

            String accessToken =
                    jwtService.generateAccessToken(userPrincipal);

            return new LoginResponse(accessToken);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token không hợp lệ hoặc đã hết hạn"
            );
        }
    }

    public void logout(HttpServletResponse response) {
        // Ghi đè cookie cũ bằng cookie rỗng, maxAge 0 để trình duyệt xoá.
        // name và path phải giống hệt lúc login, khác path là trình duyệt coi như cookie khác
        ResponseCookie clearCookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // true khi chạy HTTPS
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                clearCookie.toString()
        );
    }

}
