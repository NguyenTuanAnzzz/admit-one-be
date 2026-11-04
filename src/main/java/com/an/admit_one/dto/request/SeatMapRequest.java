package com.an.admit_one.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// PUT /api/admin/events/{id}/seatmap: gửi cả sơ đồ một lần, backend xoá sơ đồ cũ rồi lưu sơ đồ này (chỉ khi sự kiện còn DRAFT)
@Getter
@Setter
public class SeatMapRequest {

    @NotNull(message = "Chiều rộng sơ đồ không được để trống")
    @Min(value = 200, message = "Chiều rộng sơ đồ tối thiểu 200")
    @Max(value = 5000, message = "Chiều rộng sơ đồ tối đa 5000")
    private Integer mapWidth;

    @NotNull(message = "Chiều cao sơ đồ không được để trống")
    @Min(value = 200, message = "Chiều cao sơ đồ tối thiểu 200")
    @Max(value = 5000, message = "Chiều cao sơ đồ tối đa 5000")
    private Integer mapHeight;

    @Size(max = 500, message = "Link ảnh nền không được vượt quá 500 ký tự")
    private String mapBackgroundUrl;

    // @Valid để kiểm tra luôn từng phần tử bên trong danh sách
    @NotEmpty(message = "Sơ đồ cần ít nhất một hạng vé")
    @Valid
    private List<TierRequest> tiers = new ArrayList<>();

    @NotEmpty(message = "Sơ đồ cần ít nhất một khu")
    @Valid
    private List<ZoneRequest> zones = new ArrayList<>();

    @Valid
    private List<MapObjectRequest> objects = new ArrayList<>();

    @Valid
    private List<BlockedSeatRequest> blockedSeats = new ArrayList<>();
}
