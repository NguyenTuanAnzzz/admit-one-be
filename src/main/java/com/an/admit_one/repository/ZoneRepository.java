package com.an.admit_one.repository;

import com.an.admit_one.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    List<Zone> findByEventId(UUID eventId);

    // Lấy khu nhưng chỉ khi đúng sự kiện, để /events/{id}/zones/{zoneId} không đọc được khu của sự kiện khác
    Optional<Zone> findByIdAndEventId(UUID id, UUID eventId);

    @Modifying
    @Query("delete from Zone z where z.event.id = :eventId")
    void deleteByEventId(@Param("eventId") UUID eventId);
}
