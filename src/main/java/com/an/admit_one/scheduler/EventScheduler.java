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

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void openSales() {

        List<Event> events = eventRepository
                .findByStatusAndSaleOpensAtLessThanEqual(
                        EventStatus.UPCOMING,
                        Instant.now()
                );

        for (Event event : events) {

            event.setStatus(EventStatus.ON_SALE);

            event.getTickets().forEach(ticket -> {
                if (ticket.getStatus() == TicketStatus.INACTIVE) {
                    ticket.setStatus(TicketStatus.ACTIVE);
                }
            });
        }
    }
}