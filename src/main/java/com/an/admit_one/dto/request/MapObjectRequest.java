package com.an.admit_one.dto.request;

import com.an.admit_one.enums.MapObjectType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MapObjectRequest {

    @NotNull(message = "Vật thể thiếu loại")
    private MapObjectType type;

    @Size(max = 50, message = "Nhãn vật thể không được vượt quá 50 ký tự")
    private String label;

    // Cùng cách gửi với ZoneRequest.shape
    @NotNull(message = "Vật thể thiếu hình dạng")
    @Size(min = 3, message = "Hình dạng vật thể cần ít nhất 3 điểm")
    private List<@NotNull @Size(min = 2, max = 2, message = "Mỗi điểm gồm đúng 2 số [x, y]") List<@NotNull Double>> shape;

    @NotNull(message = "Vật thể thiếu vị trí x")
    private Double x;

    @NotNull(message = "Vật thể thiếu vị trí y")
    private Double y;

    private Double rotation = 0.0;
}
