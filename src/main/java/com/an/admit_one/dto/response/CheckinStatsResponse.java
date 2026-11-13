package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckinStatsResponse {
    private long checkedIn;
    private long totalIssued;
}