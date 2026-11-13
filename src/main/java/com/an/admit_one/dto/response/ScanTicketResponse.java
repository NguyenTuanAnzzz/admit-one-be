package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ScanTicketResponse {
    // OK | ALREADY_CHECKED_IN | VOID | WRONG_EVENT | NOT_FOUND
    private String result;
    private String ticketCode;
    private String ticketName;
    private String customerName;
    private Instant checkedInAt;
}