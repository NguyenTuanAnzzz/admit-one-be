package com.an.admit_one.repository;

import com.an.admit_one.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;


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
}