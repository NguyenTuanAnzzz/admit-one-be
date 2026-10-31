package com.an.admit_one.repository;

import com.an.admit_one.entity.Otp;
import com.an.admit_one.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findByUserIdAndCodeAndTypeAndUsedFalse(
            UUID userId,
            String code,
            OtpType type
    );
}