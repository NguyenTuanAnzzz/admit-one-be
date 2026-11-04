package com.an.admit_one.repository;

import com.an.admit_one.entity.MapObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MapObjectRepository extends JpaRepository<MapObject, UUID> {

    List<MapObject> findByEventId(UUID eventId);

    @Modifying
    @Query("delete from MapObject o where o.event.id = :eventId")
    void deleteByEventId(@Param("eventId") UUID eventId);
}
