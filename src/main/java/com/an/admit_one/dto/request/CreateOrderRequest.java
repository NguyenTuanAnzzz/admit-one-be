package com.an.admit_one.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Vui lòng chọn sự kiện")
    private UUID eventId;

    @NotEmpty(message = "Đơn hàng phải có ít nhất một loại vé")
    @Valid
    private List<CreateOrderItemRequest> items;
}