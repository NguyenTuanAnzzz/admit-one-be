package com.an.admit_one.service;

import java.util.List;
import java.util.UUID;

/** Tin realtime gửi qua WebSocket tới /topic/events/{id}/tickets. */
public record TicketUpdateMessage(
        UUID eventId,
        long seq,
        long ts,
        List<TicketChange> changes
) {
    public record TicketChange(UUID ticketId, int available) {
    }
}