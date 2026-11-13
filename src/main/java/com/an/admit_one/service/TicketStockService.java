package com.an.admit_one.service;

import com.an.admit_one.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
@Service
public class TicketStockService {
    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketBroadcastService broadcastService;

    public  int increase(UUID ticketId, int quantity){
        int updated = ticketRepository.increaseSoldQuantity(ticketId, quantity);

        if(updated > 0){
            broadcastAfterCommit(ticketId);
        }
        return updated;
    }

    public int decrease(UUID ticketId, int quantity) {
        int updated = ticketRepository.decreaseSoldQuantity(ticketId, quantity);
        if (updated > 0) {
            broadcastAfterCommit(ticketId);
        }
        return updated;
    }

    private void broadcastAfterCommit(UUID ticketId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            broadcastService.publish(ticketId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcastService.publish(ticketId);
            }
        });
    }
}
