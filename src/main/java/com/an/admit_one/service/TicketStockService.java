package com.an.admit_one.service;

import com.an.admit_one.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketStockService {
    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketBroadcastService broadcastService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisScript<String> ticketStockScript;

    /**
     * request: ticketId -> số lượng muốn giữ cho loại vé đó trong đơn này.
     * Trả về: "OK" (giữ được hết), "MISS" (Redis không có cache, đã warm-up và thử lại vẫn miss
     * -> bỏ qua fast-path, để Postgres tự quyết), hoặc chỉ số 1-based của ticketId đầu tiên không đủ vé.
     */
    public String tryReserve(UUID eventId, Map<UUID, Integer> request) {
        List<UUID> ticketIds = new ArrayList<>(request.keySet());
        List<String> keys = ticketIds.stream()
                .map(id -> broadcastService.stockKey(eventId, id))
                .collect(Collectors.toList());
        List<String> args = ticketIds.stream()
                .map(id -> String.valueOf(request.get(id)))
                .collect(Collectors.toList());

        String result = redisTemplate.execute(ticketStockScript, keys, args.toArray());

        if ("MISS".equals(result)) {
            for (UUID id : ticketIds) {
                ticketRepository.findById(id).ifPresent(t -> broadcastService.warmUpCache(eventId, t));
            }
            result = redisTemplate.execute(ticketStockScript, keys, args.toArray());
        }
        return result;
    }

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
