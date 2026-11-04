package com.an.admit_one.repository;

import com.an.admit_one.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {

    // Gán sự kiện của nhiều nhân viên một lần (một trang danh sách), join fetch để không tải sự kiện từng dòng một
    @Query("select a from StaffAssignment a join fetch a.event where a.staff.id in :staffIds order by a.event.startsAt")
    List<StaffAssignment> findWithEventByStaffIds(@Param("staffIds") Collection<UUID> staffIds);

    @Modifying
    @Query("delete from StaffAssignment a where a.staff.id = :staffId")
    void deleteByStaffId(@Param("staffId") UUID staffId);
}
