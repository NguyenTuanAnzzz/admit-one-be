package com.an.admit_one.controller;

import com.an.admit_one.dto.response.UploadResponse;
import com.an.admit_one.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    // Admin tải poster lên Cloudinary trước, lấy url điền vào posterUrl khi tạo hoặc sửa sự kiện
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/uploads/poster")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {

        UploadResponse response = uploadService.uploadPoster(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
 