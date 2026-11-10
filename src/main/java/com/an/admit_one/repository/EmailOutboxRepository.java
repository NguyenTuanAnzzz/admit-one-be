package com.an.admit_one.repository;

import com.an.admit_one.entity.EmailOutbox;
import com.an.admit_one.enums.EmailOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    boolean existsByOrderIdAndType(UUID orderId, String type);

    // Scheduler lấy tối đa 20 email đã tới giờ gửi, cũ nhất trước
    List<EmailOutbox> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            EmailOutboxStatus status,
            Instant now
    );
}
