package com.an.admit_one.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TierRequest {

    // Khoá tạm do FE đặt (hạng vé chưa có id lúc gửi), ZoneRequest.tierKey trỏ vào khoá này
    @NotBlank(message = "Hạng vé thiếu key")
    @Size(max = 50, message = "Key hạng vé không được vượt quá 50 ký tự")
    private String key;

    @NotBlank(message = "Tên hạng vé không được để trống")
    @Size(max = 100, message = "Tên hạng vé không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Giá vé không được để trống")
    @Min(value = 0, message = "Giá vé không được âm")
    private Long price;

    @NotBlank(message = "Màu hạng vé không được để trống")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu phải là mã hex, ví dụ #F5B8E8")
    private String color;

    @Size(max = 50, message = "Nhóm khu vực không được vượt quá 50 ký tự")
    private String areaGroup;
}
