package com.an.admit_one.repository;

import com.an.admit_one.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByGatewayTxnRef(String gatewayTxnRef);
}