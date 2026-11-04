package com.an.admit_one.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        // Chưa đặt CLOUDINARY_URL: vẫn tạo bean rỗng để BE khởi động được, UploadService sẽ báo lỗi khi gọi upload
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            return new Cloudinary();
        }

        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        // Luôn trả link https
        cloudinary.config.secure = true;
        return cloudinary;
    }
}
