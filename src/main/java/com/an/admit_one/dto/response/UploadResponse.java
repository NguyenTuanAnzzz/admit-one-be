package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// url: điền vào posterUrl khi tạo/sửa sự kiện. publicId: dùng để xoá ảnh trên Cloudinary sau này
@Getter
@AllArgsConstructor
public class UploadResponse {
    private String url;
    private String publicId;
}
