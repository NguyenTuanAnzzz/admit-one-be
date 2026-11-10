package com.an.admit_one.repository;

import com.an.admit_one.entity.IssuedTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<IssuedTicket> findByOrderIdOrderByCode(UUID orderId);

    // Vé của một đơn, tải luôn loại vé để lấy tên (tránh mỗi vé một câu SQL)
    @Query("""
        SELECT it FROM IssuedTicket it
        JOIN FETCH it.ticket
        WHERE it.order.id = :orderId
        ORDER BY it.createdAt, it.code
        """)
    List<IssuedTicket> findWithTicketByOrderId(@Param("orderId") UUID orderId);

    // Dùng khi Staff quét QR
    Optional<IssuedTicket> findByQrSecret(String qrSecret);
}