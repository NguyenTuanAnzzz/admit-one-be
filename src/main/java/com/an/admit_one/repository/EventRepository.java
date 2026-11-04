package com.an.admit_one.repository;

import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// JpaSpecificationExecutor: thêm findAll(Specification, Pageable) để lọc động kèm phân trang (xem EventSpecifications)
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    // Số sự kiện theo từng trạng thái, cho các nút lọc ở trang admin (ví dụ "Bản nháp 3")
    interface StatusCount {
        EventStatus getStatus();

        Long getTotal();
    }

    // Mọi sự kiện, kể cả bản nháp, sự kiện gần nhất lên đầu
    List<Event> findAllByOrderByStartsAtAsc();

    @Query("select e.status as status, count(e) as total from Event e group by e.status")
    List<StatusCount> countByStatus();

    // Giống countByStatus nhưng chỉ đếm các trạng thái khách được thấy
    @Query("select e.status as status, count(e) as total from Event e where e.status in :statuses group by e.status")
    List<StatusCount> countByStatusIn(@Param("statuses") List<EventStatus> statuses);
}
