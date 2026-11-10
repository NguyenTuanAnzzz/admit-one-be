package com.an.admit_one.scheduler;

import com.an.admit_one.entity.Order;
import com.an.admit_one.entity.OrderItem;
import com.an.admit_one.enums.OrderStatus;
import com.an.admit_one.repository.OrderRepository;
import com.an.admit_one.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OrderScheduler {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    // Mỗi 30 giây: huỷ đơn PENDING đã quá hạn giữ vé, trả vé lại cho người khác mua
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void expireOrders() {

        List<Order> orders = orderRepository
                .findByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());

        for (Order order : orders) {

            // Chỉ đơn còn PENDING mới đổi được. Trả về 0 nghĩa là đơn đã được xử lý ở chỗ khác
            int updated = orderRepository.updateStatusIf(
                    order.getId(),
                    OrderStatus.PENDING,
                    OrderStatus.EXPIRED
            );

            if (updated == 0) {
                continue;
            }

            // Trả lại từng loại vé trong đơn
            for (OrderItem item : order.getItems()) {
                ticketRepository.decreaseSoldQuantity(
                        item.getTicket().getId(),
                        item.getQuantity()
                );
            }
        }
    }
}