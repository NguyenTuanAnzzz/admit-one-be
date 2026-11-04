package com.an.admit_one.service;

import com.an.admit_one.dto.response.UploadResponse;
import com.an.admit_one.exception.AppException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Service
public class UploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // Trùng spring.servlet.multipart.max-file-size, kiểm tra lại ở đây để báo lỗi tiếng Việt rõ ràng
    // 10MB: đúng giới hạn một ảnh của gói Cloudinary miễn phí, lớn hơn thì Cloudinary từ chối
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private static final String POSTER_FOLDER = "admit-one/posters";

    @Autowired
    private Cloudinary cloudinary;

    public UploadResponse uploadPoster(MultipartFile file) {
        if (cloudinary.config.cloudName == null) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình Cloudinary (biến môi trường CLOUDINARY_URL)"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ảnh"
            );
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ nhận ảnh JPG, PNG hoặc WebP"
            );
        }

        if (file.getSize() > MAX_SIZE) {
            throw new AppException(
                    HttpStatus.CONTENT_TOO_LARGE,
                    "Ảnh tối đa 10MB"
            );
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", POSTER_FOLDER,
                            "resource_type", "image"
                    )
            );

            return new UploadResponse(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id")
            );

        } catch (Exception e) {
            throw new AppException(
                    HttpStatus.BAD_GATEWAY,
                    "Tải ảnh lên chưa thành công. Vui lòng thử lại."
            );
        }
    }
}
