package com.an.admit_one.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {

    @NotBlank(message = "Tên loại vé không được để trống")
    @Size(max = 100, message = "Tên loại vé không được vượt quá 100 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", message = "Giá vé không được âm")
    private BigDecimal price;

    @NotNull(message = "Số lượng vé không được để trống")
    @Positive(message = "Số lượng vé phải lớn hơn 0")
    private Integer quantity;
}