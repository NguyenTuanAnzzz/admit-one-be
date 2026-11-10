package com.an.admit_one.service;

import com.an.admit_one.entity.EmailOutbox;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.Order;
import com.an.admit_one.enums.EmailOutboxStatus;
import com.an.admit_one.repository.EmailOutboxRepository;
import com.an.admit_one.repository.IssuedTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class EmailOutboxService {

    // Thử tối đa 5 lần, quá thì chuyển FAILED
    private static final int MAX_ATTEMPTS = 5;

    private static final DateTimeFormatter EVENT_TIME =
            DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private IssuedTicketRepository issuedTicketRepository;

    @Autowired
    private EmailService emailService;

    // Gửi một email trong outbox. Mỗi dòng một transaction riêng: dòng này lỗi không ảnh hưởng dòng khác
    @Transactional
    public void sendOne(UUID outboxId) {
        EmailOutbox outbox = emailOutboxRepository.findById(outboxId).orElse(null);

        // Đã được xử lý ở lượt trước: bỏ qua
        if (outbox == null || outbox.getStatus() != EmailOutboxStatus.PENDING) {
            return;
        }

        try {
            Order order = outbox.getOrder();
            Event event = order.getEvent();

            List<EmailService.TicketLine> tickets = issuedTicketRepository.findWithTicketByOrderId(order.getId())
                    .stream()
                    .map(t -> new EmailService.TicketLine(t.getCode(), t.getTicket().getName(), t.getQrSecret()))
                    .toList();

            emailService.sendTicketsEmail(
                    outbox.getToEmail(),
                    order.getCode(),
                    event.getName(),
                    EVENT_TIME.format(event.getStartsAt()),
                    event.getVenue(),
                    tickets
            );

            outbox.setStatus(EmailOutboxStatus.SENT);
            outbox.setSentAt(Instant.now());
            outbox.setLastError(null);

        } catch (Exception e) {
            // Gửi lỗi (Gmail sập, sai mật khẩu mail...): ghi lỗi, hẹn lần sau
            int attempts = outbox.getAttempts() + 1;
            outbox.setAttempts(attempts);
            outbox.setLastError(e.getMessage());

            if (attempts >= MAX_ATTEMPTS) {
                outbox.setStatus(EmailOutboxStatus.FAILED);
            } else {
                // Lần lỗi sau chờ lâu hơn: 1, 2, 4, 8 phút
                outbox.setNextAttemptAt(Instant.now().plus(1L << (attempts - 1), ChronoUnit.MINUTES));
            }
        }
    }
}