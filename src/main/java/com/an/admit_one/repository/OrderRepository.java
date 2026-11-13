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

import java.math.BigDecimal;
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

    @Query("""
    SELECT o FROM Order o
    JOIN FETCH o.event
    WHERE o.id = :orderId
      AND o.user.id = :userId
    """)
    Optional<Order> findMyOrder(@Param("orderId") UUID orderId, @Param("userId") UUID userId);

    @Modifying
    @Query("""
    UPDATE Order o
    SET o.status = com.an.admit_one.enums.OrderStatus.PAID,
        o.paidAt = :paidAt
    WHERE o.id = :orderId
      AND o.status = :currentStatus
    """)
    int markPaidIf(@Param("orderId") UUID orderId,
                   @Param("currentStatus") OrderStatus currentStatus,
                   @Param("paidAt") Instant paidAt);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    @Query(value = """
        SELECT CAST(paid_at AS date) AS day, COALESCE(SUM(total_amount), 0) AS revenue
        FROM orders
        WHERE status = 'PAID' AND paid_at >= :from
        GROUP BY day
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> sumRevenueByDay(@Param("from") Instant from);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("""
        SELECT o.event.name, COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.status = 'PAID'
        GROUP BY o.event.name
        ORDER BY SUM(o.totalAmount) DESC
        """)
    List<Object[]> sumRevenueByEvent(Pageable pageable);
}
