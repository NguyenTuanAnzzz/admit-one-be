package com.an.admit_one.service;

import com.an.admit_one.entity.Ticket;
import com.an.admit_one.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TicketBroadcastService {
    private static final String SEQ_KEY_PREFIX = "ticket-seq:event:";

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public long currentSeq(UUID eventId) {
        String raw = redisTemplate.opsForValue().get(SEQ_KEY_PREFIX + eventId);
        return raw == null ? 0L : Long.parseLong(raw);
    }

    public void publish(UUID ticketId){
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if(ticket == null){
            return;
        }

        UUID eventId = ticket.getEvent().getId();
        long seq = redisTemplate.opsForValue().increment(SEQ_KEY_PREFIX + eventId);
        int available = Math.max(0, ticket.getQuantity() - ticket.getSoldQuantity());
        var message = new TicketUpdateMessage(
                eventId,
                seq,
                System.currentTimeMillis(),
                java.util.List.of(new TicketUpdateMessage.TicketChange(ticketId, available))
        );
        messagingTemplate.convertAndSend("/topic/events/" + eventId + "/tickets", message);
    }
}
