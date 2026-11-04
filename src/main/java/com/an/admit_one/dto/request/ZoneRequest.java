package com.an.admit_one.dto.request;

import com.an.admit_one.enums.SeatLayout;
import com.an.admit_one.enums.SeatingMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ZoneRequest {

    // Trỏ tới TierRequest.key của hạng vé mà khu này thuộc về
    @NotBlank(message = "Khu thiếu hạng vé")
    private String tierKey;

    @NotBlank(message = "Tên khu không được để trống")
    @Size(max = 50, message = "Tên khu không được vượt quá 50 ký tự")
    private String label;

    // Đa giác viền khu: ít nhất 3 điểm, mỗi điểm [x, y], tương đối với (x, y) của khu. Service đổi sang chuỗi JSON để lưu
    @NotNull(message = "Khu thiếu hình dạng")
    @Size(min = 3, message = "Hình dạng khu cần ít nhất 3 điểm")
    private List<@NotNull @Size(min = 2, max = 2, message = "Mỗi điểm gồm đúng 2 số [x, y]") List<@NotNull Double>> shape;

    @NotNull(message = "Khu thiếu vị trí x")
    private Double x;

    @NotNull(message = "Khu thiếu vị trí y")
    private Double y;

    // Không gửi thì coi như 0 độ
    private Double rotation = 0.0;

    // Không gửi thì mặc định chọn từng ghế
    private SeatingMode seatingMode = SeatingMode.RESERVED;

    // Tối đa 50 hàng x 100 ghế = 5.000 ghế một khu, chặn gửi nhầm số quá lớn làm treo backend
    @NotNull(message = "Khu thiếu số hàng")
    @Min(value = 1, message = "Khu cần ít nhất 1 hàng")
    @Max(value = 50, message = "Khu tối đa 50 hàng")
    private Integer rowCount;

    @NotNull(message = "Khu thiếu số ghế mỗi hàng")
    @Min(value = 1, message = "Mỗi hàng cần ít nhất 1 ghế")
    @Max(value = 100, message = "Mỗi hàng tối đa 100 ghế")
    private Integer seatsPerRow;

    private SeatLayout seatLayout = SeatLayout.GRID;
}
