package com.an.admit_one.repository;

import com.an.admit_one.entity.OrderItem;
import com.an.admit_one.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("""
    SELECT COALESCE(SUM(i.quantity), 0)
    FROM OrderItem i
    WHERE i.order.user.id = :userId
      AND i.order.event.id = :eventId
      AND i.order.status IN :statuses
    """)
    long sumQuantityByUserAndEvent(@Param("userId") UUID userId,
                                   @Param("eventId") UUID eventId,
                                   @Param("statuses") List<OrderStatus> statuses);
}
