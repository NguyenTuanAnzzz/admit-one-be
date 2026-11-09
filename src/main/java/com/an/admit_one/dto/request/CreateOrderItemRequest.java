package com.an.admit_one.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateOrderItemRequest {

    @NotNull(message = "Vui lòng chọn loại vé")
    private UUID ticketId;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 1, message = "Số lượng vé phải lớn hơn 0")
    private Integer quantity;
}