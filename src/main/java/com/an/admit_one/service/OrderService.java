package com.an.admit_one.service;

import com.an.admit_one.dto.request.CreateOrderItemRequest;
import com.an.admit_one.dto.request.CreateOrderRequest;
import com.an.admit_one.dto.response.*;
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
import org.springframework.beans.factory.annotation.Value;
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
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.exception.APIException;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.Transaction;
import org.springframework.dao.DataIntegrityViolationException;

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
    private TicketStockService ticketStockService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PayOS payOS;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IssuedTicketRepository issuedTicketRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${app.fe-url}")
    private String feUrl;

    private String generateOrderCode() {
        return "AO" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    @Transactional
    public CreateOrderResponse createOrder(@Valid CreateOrderRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Tối đa 5 lần tạo đơn / 60 giây / tài khoản, chặn trước khi chạm DB
        rateLimitService.check("rl:create-order:" + principal.getUser().getId(), 5, 60);

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

        // Bài 5.1 - fast path: kiểm tra + giữ tạm trên Redis trước khi chạm Postgres.
        // Đoạn Postgres phía dưới vẫn giữ nguyên làm lớp chốt cuối cùng (nguồn sự thật).
        Map<UUID, Integer> reserveRequest = new LinkedHashMap<>();
        for (CreateOrderItemRequest item : request.getItems()) {
            reserveRequest.put(item.getTicketId(), item.getQuantity());
        }

        String reserveResult = ticketStockService.tryReserve(event.getId(), reserveRequest);
        if (!"OK".equals(reserveResult) && !"MISS".equals(reserveResult)) {
            int badIndex = Integer.parseInt(reserveResult) - 1;
            UUID badTicketId = new ArrayList<>(reserveRequest.keySet()).get(badIndex);
            Ticket badTicket = ticketRepository.findById(badTicketId).orElse(null);
            String name = badTicket != null ? badTicket.getName() : "đã chọn";
            throw new AppException(HttpStatus.CONFLICT, "Loại vé " + name + " vừa hết, vui lòng chọn lại");
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

            int updated = ticketStockService.increase(ticket.getId(), item.getQuantity());
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
                itemResponses,
                Math.max(0, Instant.now().until(order.getExpiresAt(), ChronoUnit.SECONDS))
        );
    }

    @Transactional(readOnly = true)
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
                        .toList(),
                Math.max(0, Instant.now().until(order.getExpiresAt(), ChronoUnit.SECONDS))
        ));
    }

    @Transactional(readOnly = true)
    public MyOrderResponse getMyOrder(UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Order order = orderRepository.findMyOrder(orderId, principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return new MyOrderResponse(
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
                        .toList(),
                Math.max(0, Instant.now().until(order.getExpiresAt(), ChronoUnit.SECONDS))
        );
    }

    public Page<AdminOrderResponse> getAdminOrders(String keyword, OrderStatus status, int page, int size) {
        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatus(status))
                .and(OrderSpecifications.search(keyword))
                .and(OrderSpecifications.fetchEvent());

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        return orderPage.map(order -> new AdminOrderResponse(
                order.getId(),
                order.getCode(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getEvent().getName()
        ));
    }


    public List<PaymentLogResponse> getOrderPayments(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }

        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(p -> new PaymentLogResponse(
                        p.getId(),
                        p.getGateway(),
                        p.getGatewayTxnRef(),
                        p.getAmount(),
                        p.getStatus().name(),
                        p.getRawPayload(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public MessageResponse cancelMyOrder(UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Order order = orderRepository.findMyOrder(orderId, principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ huỷ được đơn đang giữ vé");
        }

        // Đổi trạng thái trước: trả về 0 nghĩa là đơn vừa hết hạn hoặc vừa thanh toán ở chỗ khác
        int updated = orderRepository.updateStatusIf(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED);
        if (updated == 0) {
            throw new AppException(HttpStatus.CONFLICT, "Đơn vừa được cập nhật, vui lòng tải lại trang");
        }

        // Trả lại vé
        for (OrderItem item : order.getItems()) {
            ticketStockService.decrease(item.getTicket().getId(), item.getQuantity());
        }

        return new MessageResponse("Đã huỷ đơn, vé đã được trả lại");
    }

    @Transactional
    public Map<String, String> startPayment(UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Order order = orderRepository.findMyOrder(orderId, principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        long remainingSeconds = Math.max(0, Instant.now().until(order.getExpiresAt(), ChronoUnit.SECONDS));
        if (order.getStatus() != OrderStatus.PENDING || remainingSeconds == 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn không còn chờ thanh toán");
        }

        // Đã tạo link rồi: trả lại link cũ
        if (order.getCheckoutUrl() != null) {
            return Map.of("checkoutUrl", order.getCheckoutUrl());
        }

        // payOS cần orderCode là số: lấy phần số trong mã đơn (AO1794296491047238 → 1794296491047238)
        long payosOrderCode = Long.parseLong(order.getCode().substring(2));
        // Trang kết quả thanh toán của FE. payOS tự gắn thêm code, id, status, orderCode vào sau
        String back = feUrl + "/orders/" + order.getId() + "/payment";

        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(payosOrderCode)
                .amount(order.getTotalAmount().longValue())
                // payOS giới hạn mô tả ngắn: chỉ ghi mã đơn
                .description(order.getCode())
                .returnUrl(back + "?result=success")
                .cancelUrl(back + "?result=cancel")
                // Link hết hạn cùng lúc với đơn giữ vé
                .expiredAt(order.getExpiresAt().getEpochSecond())
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);

        order.setCheckoutUrl(response.getCheckoutUrl());
        return Map.of("checkoutUrl", response.getCheckoutUrl());
    }

    // Vé đã xuất của một đơn (vé có QR). Chỉ chủ đơn xem được (findMyOrder lọc theo user). Đơn chưa PAID thì danh sách rỗng
    @Transactional(readOnly = true)
    public List<IssuedTicketResponse> getMyTickets(UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Order order = orderRepository.findMyOrder(orderId, principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return issuedTicketRepository.findWithTicketByOrderId(order.getId()).stream()
                .map(t -> new IssuedTicketResponse(
                        t.getId(),
                        t.getCode(),
                        t.getTicket().getName(),
                        t.getStatus().name(),
                        t.getQrSecret(),
                        t.getCheckedInAt()
                ))
                .toList();
    }

    /**
     * Đối soát với payOS: hỏi payOS link của đơn đã có giao dịch nào chưa, có thì ghi nhận như webhook.
     * Dùng khi webhook bị lỡ (ngrok tắt, mạng lỗi...). Mã giao dịch "PAYOS-" + reference giống hệt webhook,
     * nên webhook và đối soát cùng về một giao dịch thì vẫn chỉ ghi 1 lần (cột UNIQUE gateway_txn_ref).
     *
     * Không đặt @Transactional ở đây: mỗi lần gọi paymentService.handleTransfer chạy transaction riêng,
     * một giao dịch bị trùng (rollback) không kéo theo giao dịch khác.
     */
    public MessageResponse syncPayment(UUID orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Order order = orderRepository.findMyOrder(orderId, principal.getUser().getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // Đã trả rồi, hoặc chưa từng tạo link payOS: không có gì để hỏi
        if (order.getStatus() == OrderStatus.PAID || order.getCheckoutUrl() == null) {
            return new MessageResponse(order.getStatus().name());
        }

        long payosOrderCode = Long.parseLong(order.getCode().substring(2));
        PaymentLink link;
        try {
            link = payOS.paymentRequests().get(payosOrderCode);
        } catch (APIException e) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Không kiểm tra được thanh toán: " + e.getMessage());
        }

        if (link.getTransactions() != null) {
            for (Transaction tx : link.getTransactions()) {
                try {
                    paymentService.handleTransfer(
                            "PAYOS",
                            "PAYOS-" + tx.getReference(),
                            BigDecimal.valueOf(tx.getAmount()),
                            order.getCode(),
                            tx.toString()
                    );
                } catch (DataIntegrityViolationException e) {
                    // Webhook vừa ghi nhận cùng giao dịch này: bỏ qua
                }
            }
        }

        return new MessageResponse(String.valueOf(link.getStatus()));
    }
}
