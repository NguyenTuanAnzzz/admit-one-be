package com.an.admit_one.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// Dùng chung cho tạo (POST) và sửa (PUT) sự kiện. Không có status: sự kiện mới luôn là DRAFT, đổi trạng thái qua PATCH riêng
@Getter
@Setter
public class EventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = 200, message = "Tên sự kiện không được vượt quá 200 ký tự")
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String venue;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    // Chỉ nhận link Google Maps (maps.app.goo.gl, goo.gl/maps, google.com/maps...), để không lưu link lạ vào trang khách
    @Size(max = 500, message = "Link Google Maps không được vượt quá 500 ký tự")
    @Pattern(
            regexp = "^https://(maps\\.app\\.goo\\.gl/|goo\\.gl/maps/|(www\\.)?google\\.com(\\.[a-z]{2})?/maps|maps\\.google\\.com(\\.[a-z]{2})?/).*$",
            message = "Link Google Maps không hợp lệ"
    )
    private String googleMapsUrl;

    private String description;

    @Size(max = 500, message = "Link ảnh không được vượt quá 500 ký tự")
    private String posterUrl;

    @NotNull(message = "Giờ diễn không được để trống")
    @Future(message = "Giờ diễn phải ở tương lai")
    private Instant startsAt;

    // Phải trước startsAt: kiểm tra trong service vì cần so sánh hai trường
    @NotNull(message = "Giờ mở bán không được để trống")
    private Instant saleOpensAt;
}
