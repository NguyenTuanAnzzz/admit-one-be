package com.an.admit_one.repository;

import com.an.admit_one.entity.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketTierRepository extends JpaRepository<TicketTier, UUID> {

    List<TicketTier> findByEventIdOrderBySortOrderAsc(UUID eventId);

    // Xoá bằng một câu DELETE, không tải từng dòng lên rồi xoá. Gọi trong @Transactional
    @Modifying
    @Query("delete from TicketTier t where t.event.id = :eventId")
    void deleteByEventId(@Param("eventId") UUID eventId);
}
