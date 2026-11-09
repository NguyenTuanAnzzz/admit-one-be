package com.an.admit_one.scheduler;

import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.enums.TicketStatus;
import com.an.admit_one.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class EventScheduler {

    @Autowired
    private EventRepository eventRepository;

    // Tự động mở bán vé
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void openSales() {

        List<Event> events = eventRepository
                .findByStatusAndSaleOpensAtLessThanEqual(
                        EventStatus.UPCOMING,
                        Instant.now()
                );

        for (Event event : events) {

            // Nếu sự kiện đã bắt đầu thì không mở bán nữa
            if (!event.getStartsAt().isAfter(Instant.now())) {
                event.setStatus(EventStatus.ENDED);
                continue;
            }

            event.setStatus(EventStatus.ON_SALE);

            event.getTickets().forEach(ticket -> {
                if (ticket.getStatus() == TicketStatus.INACTIVE) {
                    ticket.setStatus(TicketStatus.ACTIVE);
                }
            });
        }
    }

    // Tự động kết thúc sự kiện
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void endEvents() {

        List<Event> events = eventRepository
                .findByStatusInAndStartsAtLessThanEqual(
                        List.of(
                                EventStatus.UPCOMING,
                                EventStatus.ON_SALE,
                                EventStatus.SOLD_OUT
                        ),
                        Instant.now()
                );

        for (Event event : events) {
            event.setStatus(EventStatus.ENDED);
        }
    }
}