package com.an.admit_one.repository;

import com.an.admit_one.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByIdAndEventId(UUID ticketId, UUID eventId);

    List<Ticket> findByEventId(UUID eventId);

    boolean existsByEventIdAndNameIgnoreCase(
            UUID eventId,
            String name
    );

    boolean existsByEventIdAndNameIgnoreCaseAndIdNot(
            UUID eventId,
            String name,
            UUID ticketId
    );

    @Modifying
    @Query("""
        UPDATE Ticket t
        SET t.soldQuantity = t.soldQuantity + :qty,
            t.updatedAt = CURRENT_TIMESTAMP
        WHERE t.id = :ticketId
          AND t.soldQuantity + :qty <= t.quantity
        """)
    int increaseSoldQuantity(@Param("ticketId") UUID ticketId, @Param("qty") int qty);

    @Modifying
    @Query("""
    UPDATE Ticket t
    SET t.soldQuantity = t.soldQuantity - :qty,
        t.updatedAt = CURRENT_TIMESTAMP
    WHERE t.id = :ticketId
      AND t.soldQuantity >= :qty
    """)
    int decreaseSoldQuantity(@Param("ticketId") UUID ticketId, @Param("qty") int qty);
}