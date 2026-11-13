package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CheckinEventResponse {
    private UUID id;
    private String name;
    private String venue;
    private Instant startsAt;
    private String status;
}