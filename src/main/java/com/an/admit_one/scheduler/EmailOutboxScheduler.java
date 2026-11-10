package com.an.admit_one.scheduler;

import com.an.admit_one.entity.EmailOutbox;
import com.an.admit_one.enums.EmailOutboxStatus;
import com.an.admit_one.repository.EmailOutboxRepository;
import com.an.admit_one.service.EmailOutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmailOutboxScheduler {

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private EmailOutboxService emailOutboxService;

    // Mỗi 10 giây: lấy tối đa 20 email đã tới giờ gửi rồi gửi từng cái
    @Scheduled(fixedDelay = 10_000)
    public void sendPending() {
        for (EmailOutbox outbox : emailOutboxRepository
                .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(EmailOutboxStatus.PENDING, Instant.now())) {
            emailOutboxService.sendOne(outbox.getId());
        }
    }
}