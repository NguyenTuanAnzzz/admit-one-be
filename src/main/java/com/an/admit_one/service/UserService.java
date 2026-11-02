package com.an.admit_one.service;

import com.an.admit_one.dto.request.UpdateMeRequest;
import com.an.admit_one.dto.response.MeResponse;
import com.an.admit_one.entity.User;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.UserRepository;
import com.an.admit_one.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole().name());
    }
}
