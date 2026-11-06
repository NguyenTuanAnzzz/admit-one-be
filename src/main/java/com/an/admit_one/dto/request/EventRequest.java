
package com.an.admit_one.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = 200, message = "Tên sự kiện không được vượt quá 200 ký tự")
    private String name;

    @NotBlank(message = "Địa điểm tổ chức không được để trống")
    @Size(max = 255, message = "Địa điểm không được vượt quá 255 ký tự")
    private String venue;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 500, message = "Link Google Maps không được vượt quá 500 ký tự")
    private String googleMapsUrl;

    private String description;

    @Size(max = 500, message = "Link poster không được vượt quá 500 ký tự")
    private String posterUrl;

    @NotNull(message = "Thời gian diễn ra sự kiện không được để trống")
    private Instant startsAt;

    @NotNull(message = "Thời gian mở bán vé không được để trống")
    private Instant saleOpensAt;

    @NotEmpty(message = "Sự kiện phải có ít nhất một loại vé")
    @Valid
    private List<TicketRequest> tickets;
}
