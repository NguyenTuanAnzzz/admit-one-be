
package com.an.admit_one.repository;

import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    boolean existsByNameIgnoreCaseAndStartsAt(String name, Instant startsAt);

    @Query("""
    SELECT DISTINCT e
    FROM Event e
    LEFT JOIN FETCH e.tickets
    WHERE e.id = :eventId
""")
    Optional<Event> findByIdWithTickets(@Param("eventId") UUID eventId);

    List<Event> findByStatusInAndStartsAtLessThanEqual(
            List<EventStatus> statuses,
            Instant startsAt
    );

    List<Event> findByStatusAndSaleOpensAtLessThanEqual(
            EventStatus status,
            Instant time
    );
}
