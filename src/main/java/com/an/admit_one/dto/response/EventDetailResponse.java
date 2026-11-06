package com.an.admit_one.dto.response;

import com.an.admit_one.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {

    private UUID id;

    private String name;

    private String venue;

    private String address;

    private String googleMapsUrl;

    private String description;

    private String posterUrl;

    private Instant startsAt;

    private Instant saleOpensAt;

    private EventStatus status;

    private List<TicketResponse> tickets;

    private Instant createdAt;

    private Instant updatedAt;
}