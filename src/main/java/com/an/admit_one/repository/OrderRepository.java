package com.an.admit_one.repository;

import com.an.admit_one.entity.Order;
import com.an.admit_one.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByCode(String code);

    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, Instant time);

    @Modifying
    @Query("""
    UPDATE Order o
    SET o.status = :newStatus
    WHERE o.id = :orderId
      AND o.status = :currentStatus
    """)
    int updateStatusIf(@Param("orderId") UUID orderId,
                       @Param("currentStatus") OrderStatus currentStatus,
                       @Param("newStatus") OrderStatus newStatus);
}
