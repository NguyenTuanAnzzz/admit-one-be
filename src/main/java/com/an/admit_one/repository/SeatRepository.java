package com.an.admit_one.repository;

import com.an.admit_one.entity.Seat;
import com.an.admit_one.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    // Kết quả đếm ghế theo (khu, trạng thái). Tên hàm get... trùng alias trong câu query bên dưới
    interface ZoneStatusCount {
        UUID getZoneId();

        SeatStatus getStatus();

        Long getTotal();
    }

    List<Seat> findByZoneIdOrderByRowLabelAscSeatNumberAsc(UUID zoneId);

    long countByEventId(UUID eventId);

    // Dùng khi công bố sự kiện: chưa có ghế nào thì không cho công bố
    boolean existsByEventId(UUID eventId);

    // Một câu GROUP BY thay cho đếm từng khu: ra số ghế AVAILABLE, BLOCKED... của mọi khu trong sự kiện
    @Query("""
            select s.zone.id as zoneId, s.status as status, count(s) as total
            from Seat s
            where s.event.id = :eventId
            group by s.zone.id, s.status
            """)
    List<ZoneStatusCount> countByZoneAndStatus(@Param("eventId") UUID eventId);

    // Xoá hàng nghìn ghế bằng một câu DELETE. Phải xoá ghế trước khu (khoá ngoại zone_id)
    @Modifying
    @Query("delete from Seat s where s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") UUID eventId);
}
