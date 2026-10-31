package com.an.admit_one.dto.response;

import com.an.admit_one.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID id;

    private String fullName;

    private String email;

    private String phone;

    private UserRole role;

    private Instant emailVerifiedAt;
}