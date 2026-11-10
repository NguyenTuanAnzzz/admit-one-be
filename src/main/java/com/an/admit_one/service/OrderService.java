package com.an.admit_one.service;

import com.an.admit_one.dto.request.CreateOrderItemRequest;
import com.an.admit_one.dto.request.CreateOrderRequest;
import com.an.admit_one.dto.response.CreateOrderResponse;
import com.an.admit_one.dto.response.MyOrderResponse;
import com.an.admit_one.dto.response.OrderItemResponse;
import com.an.admit_one.entity.*;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.enums.OrderStatus;
import com.an.admit_one.enums.TicketStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.*;
import com.an.admit_one.security.UserPrincipal;
import com.an.admit_one.specification.OrderSpecifications;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private String generateOrderCode() {
        return "AO" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    @Transactional
    public CreateOrderResponse createOrder(@Valid CreateOrderRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        // Khoá dòng user: hai đơn của cùng một người phải chờ nhau, không đếm trùng số vé đã giữ
        User user = userRepository.findByIdForUpdate(principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));

        if (event.getStatus() != EventStatus.ON_SALE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Sự kiện chưa mở bán hoặc đã ngừng bán");
        }

        Set<UUID> ticketIds = new HashSet<>();
        int totalQuantity = 0;
        for (CreateOrderItemRequest item : request.getItems()) {
            if (!ticketIds.add(item.getTicketId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mỗi loại vé chỉ được chọn một lần");
            }
            totalQuantity += item.getQuantity();
        }

        if (totalQuantity > 4) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mỗi đơn tối đa 4 vé");
        }

        long alreadyHeld = orderItemRepository.sumQuantityByUserAndEvent(
                user.getId(),
                event.getId(),
                List.of(OrderStatus.PENDING, OrderStatus.PAID)
        );

        if (alreadyHeld + totalQuantity > 4) {
            long canBuy = Math.max(0, 4 - alreadyHeld);
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Bạn đã có " + alreadyHeld + " vé cho sự kiện này, chỉ đặt thêm được " + canBuy + " vé");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for(CreateOrderItemRequest item: request.getItems()){
            Ticket ticket = ticketRepository.findByIdAndEventId(item.getTicketId(), event.getId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy loại vé"));

            if (ticket.getStatus() != TicketStatus.ACTIVE) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Loại vé " + ticket.getName() + " không còn mở bán");
            }
            int remaining = ticket.getQuantity() - ticket.getSoldQuantity();

            if (item.getQuantity() > remaining) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Loại vé " + ticket.getName() + " chỉ còn " + remaining + " vé");
            }

            int updated = ticketRepository.increaseSoldQuantity(ticket.getId(), item.getQuantity());
            if (updated == 0) {
                throw new AppException(HttpStatus.CONFLICT, "Loại vé " + ticket.getName() + " vừa hết, vui lòng chọn lại");
            }

            BigDecimal subtotal = ticket.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .ticket(ticket)
                    .quantity(item.getQuantity())
                    .unitPrice(ticket.getPrice())
                    .build());
        }

        Order order = Order.builder()
                .code(generateOrderCode())
                .user(user)
                .event(event)
                .totalAmount(totalAmount)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        orderRepository.save(order);

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getTicket().getId(),
                        i.getTicket().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();

        return new CreateOrderResponse(
                order.getId(),
                order.getCode(),
                event.getId(),
                event.getName(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getExpiresAt(),
                itemResponses
        );
    }


    public Page<MyOrderResponse> getMyOrders(String keyword, OrderStatus status, int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Specification<Order> spec = Specification
                .where(OrderSpecifications.belongsTo(principal.getUser().getId()))
                .and(OrderSpecifications.hasStatus(status))
                .and(OrderSpecifications.search(keyword))
                .and(OrderSpecifications.fetchEvent());

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        return orderPage.map(order -> new MyOrderResponse(
                order.getId(),
                order.getCode(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getEvent().getId(),
                order.getEvent().getName(),
                order.getEvent().getPosterUrl(),
                order.getEvent().getStartsAt(),
                order.getEvent().getVenue(),
                order.getItems().stream()
                        .map(i -> new OrderItemResponse(
                                i.getTicket().getId(),
                                i.getTicket().getName(),
                                i.getQuantity(),
                                i.getUnitPrice(),
                                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                        ))
                        .toList()
        ));
    }
}
