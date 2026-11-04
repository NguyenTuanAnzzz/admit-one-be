package com.an.admit_one.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Ghế khách mời, ghế kỹ thuật: sinh ra với trạng thái BLOCKED. Ghế chưa có id lúc gửi nên chỉ ra bằng khu + hàng + số
@Getter
@Setter
public class BlockedSeatRequest {

    // Vị trí của khu trong SeatMapRequest.zones, bắt đầu từ 0
    @NotNull(message = "Ghế khoá thiếu khu")
    @Min(value = 0, message = "Chỉ số khu không hợp lệ")
    private Integer zoneIndex;

    @NotBlank(message = "Ghế khoá thiếu hàng")
    @Size(max = 5, message = "Tên hàng không được vượt quá 5 ký tự")
    private String rowLabel;

    @NotNull(message = "Ghế khoá thiếu số ghế")
    @Min(value = 1, message = "Số ghế bắt đầu từ 1")
    private Integer seatNumber;
}
