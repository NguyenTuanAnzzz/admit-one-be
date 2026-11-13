package com.an.admit_one.service;

import com.an.admit_one.dto.request.CreateUserRequest;
import com.an.admit_one.dto.request.UpdateMeRequest;
import com.an.admit_one.dto.response.MeResponse;
import com.an.admit_one.dto.response.UserResponse;
import com.an.admit_one.entity.User;
import com.an.admit_one.enums.UserRole;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.UserRepository;
import com.an.admit_one.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public MeResponse getUser(UserPrincipal principal) {

        User user = principal.getUser();

        return toMeResponse(user);
    }

    public MeResponse updateMe(UserPrincipal principal, UpdateMeRequest request) {
        // Lấy bản mới từ DB để sửa rồi lưu, không sửa thẳng bản principal tải lúc lọc request
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Tài khoản không tồn tại"
                ));

        // Cột phone là unique: số đã thuộc người khác thì báo 409, không để DB ném lỗi thành 500
        String phone = request.getPhone();
        if (phone != null) {
            userRepository.findByPhone(phone).ifPresent(other -> {
                if (!other.getId().equals(user.getId())) {
                    throw new AppException(
                            HttpStatus.CONFLICT,
                            "Số điện thoại đã được đăng ký"
                    );
                }
            });
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);

        userRepository.save(user);

        return toMeResponse(user);
    }

    // Dashboard › Người dùng (admin): liệt kê + tạo/sửa tài khoản cả 3 role (ADMIN, STAFF, CUSTOMER)

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toUserResponse)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    public Page<UserResponse> listUsers(UserRole role, String keyword, int page, int size) {
        String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        Page<User> users = userRepository.searchUsers(
                role, pattern, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return users.map(this::toUserResponse);
    }

    // Admin chỉ tạo được ADMIN hoặc STAFF ở đây - CUSTOMER tự đăng ký qua luồng OTP riêng
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.getRole() == UserRole.CUSTOMER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không tạo tài khoản khách hàng ở đây");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được đăng ký");
        }

        String phone = request.getPhone();
        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();
            if (userRepository.existsByPhone(phone)) {
                throw new AppException(HttpStatus.CONFLICT, "Số điện thoại đã được đăng ký");
            }
        } else {
            phone = null;
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmailVerifiedAt(Instant.now()); // admin tạo trực tiếp, khỏi cần xác minh qua OTP

        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateMeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        String phone = request.getPhone();
        if (phone != null) {
            userRepository.findByPhone(phone).ifPresent(other -> {
                if (!other.getId().equals(user.getId())) {
                    throw new AppException(HttpStatus.CONFLICT, "Số điện thoại đã được đăng ký");
                }
            });
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);

        return toUserResponse(userRepository.save(user));
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole().name());
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole().name(), user.getCreatedAt()
        );
    }
}
