package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MeResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
}