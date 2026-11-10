package com.an.admit_one.repository;

import com.an.admit_one.entity.Order;
import com.an.admit_one.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

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

    @Query(
            value = """
        SELECT o FROM Order o
        JOIN FETCH o.event e
        WHERE o.user.id = :userId
          AND (:status IS NULL OR o.status = :status)
          AND (lower(o.code) LIKE :pattern OR lower(e.name) LIKE :pattern)
        """,
            countQuery = """
        SELECT COUNT(o) FROM Order o
        JOIN o.event e
        WHERE o.user.id = :userId
          AND (:status IS NULL OR o.status = :status)
          AND (lower(o.code) LIKE :pattern OR lower(e.name) LIKE :pattern)
        """
    )
    Page<Order> findMyOrders(@Param("userId") UUID userId,
                             @Param("status") OrderStatus status,
                             @Param("pattern") String pattern,
                             Pageable pageable);
}
