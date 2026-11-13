package com.an.admit_one.repository;

import com.an.admit_one.entity.IssuedTicket;
import com.an.admit_one.enums.IssuedTicketStatus;
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
public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, UUID> {

    long countByEventId(UUID eventId);
    long countByEventIdAndStatus(UUID eventId, IssuedTicketStatus status);

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

    @Modifying
    @Query(value = """
    UPDATE issued_tickets
    SET status = 'CHECKED_IN', checked_in_at = :now, checked_in_by = :staffId
    WHERE id = :id AND status = 'VALID'
    """, nativeQuery = true)
    int checkInIfValid(@Param("id") UUID id, @Param("now") Instant now, @Param("staffId") UUID staffId);
}