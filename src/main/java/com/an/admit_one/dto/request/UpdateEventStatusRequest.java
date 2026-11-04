package com.an.admit_one.dto.request;

import com.an.admit_one.enums.EventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEventStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private EventStatus status;
}
