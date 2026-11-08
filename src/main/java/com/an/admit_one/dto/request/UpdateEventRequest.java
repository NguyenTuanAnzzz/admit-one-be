package com.an.admit_one.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UpdateEventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = 200, message = "Tên sự kiện không được vượt quá 200 ký tự")
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String venue;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 500, message = "Google Maps URL không được vượt quá 500 ký tự")
    private String googleMapsUrl;

    private String description;

    @Size(max = 500, message = "Poster URL không được vượt quá 500 ký tự")
    private String posterUrl;

    @NotNull(message = "Thời gian diễn ra sự kiện không được để trống")
    private Instant startsAt;

    @NotNull(message = "Thời gian mở bán vé không được để trống")
    private Instant saleOpensAt;
}