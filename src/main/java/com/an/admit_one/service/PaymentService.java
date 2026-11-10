package com.an.admit_one.service;

import com.an.admit_one.entity.IssuedTicket;
import com.an.admit_one.entity.Order;
import com.an.admit_one.entity.OrderItem;
import com.an.admit_one.entity.Payment;
import com.an.admit_one.enums.OrderStatus;
import com.an.admit_one.enums.PaymentStatus;
import com.an.admit_one.repository.IssuedTicketRepository;
import com.an.admit_one.repository.OrderRepository;
import com.an.admit_one.repository.PaymentRepository;
import com.an.admit_one.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentService {

    // Mã đơn trong nội dung: AO + chữ số
    private static final Pattern ORDER_CODE = Pattern.compile("AO\\d+");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private IssuedTicketRepository issuedTicketRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Xử lý một giao dịch tiền vào (webhook payOS gọi tới).
     * txnRef: mã giao dịch của cổng, UNIQUE trong bảng payments.
     * content: chuỗi chứa mã đơn AO..., dùng để tìm đơn.
     * Trả về chuỗi mô tả kết quả để ghi log.
     */
    @Transactional
    public String handleTransfer(String gateway, String txnRef, BigDecimal amount, String content, String rawPayload) {

        // 1. Đã xử lý giao dịch này rồi (cổng gửi lại): bỏ qua
        if (paymentRepository.existsByGatewayTxnRef(txnRef)) {
            return "DUPLICATE";
        }

        // 2. Tìm mã đơn trong nội dung
        Matcher matcher = ORDER_CODE.matcher(content == null ? "" : content.toUpperCase());
        if (!matcher.find()) {
            return "NO_ORDER_CODE";
        }

        Optional<Order> found = orderRepository.findByCode(matcher.group());
        if (found.isEmpty()) {
            return "ORDER_NOT_FOUND";
        }
        Order order = found.get();

        // 3. Ghi giao dịch NGAY. gatewayTxnRef UNIQUE nên nhiều request trùng chạy song song chỉ 1 cái ghi được,
        //    các cái còn lại lỗi trùng khoá và rollback toàn bộ, không đụng tới đơn hay vé
        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .order(order)
                .gateway(gateway)
                .gatewayTxnRef(txnRef)
                .amount(amount)
                .status(PaymentStatus.FAILED)
                .rawPayload(rawPayload)
                .build());

        // 4. Chuyển thiếu tiền: giữ FAILED, đơn không đổi
        if (amount.compareTo(order.getTotalAmount()) < 0) {
            return "AMOUNT_TOO_LOW";
        }

        // 5. Trường hợp thường
        if (order.getStatus() == OrderStatus.PENDING
                && orderRepository.markPaidIf(order.getId(), OrderStatus.PENDING, Instant.now()) == 1) {
            payment.setStatus(PaymentStatus.SUCCESS);
            issueTickets(order);
            return "PAID";
        }


        // 6. Trả tiền sau khi hết hạn hoặc đã huỷ: vé đã trả lại, thử giữ lại vé cho khách
        if (order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.CANCELLED) {
            OrderStatus previous = order.getStatus();
            List<OrderItem> reserved = new ArrayList<>();
            boolean enough = true;

            for (OrderItem item : order.getItems()) {
                if (ticketRepository.increaseSoldQuantity(item.getTicket().getId(), item.getQuantity()) == 1) {
                    reserved.add(item);
                } else {
                    enough = false;
                    break;
                }
            }

            if (enough && orderRepository.markPaidIf(order.getId(), previous, Instant.now()) == 1) {
                issueTickets(order);
                payment.setStatus(PaymentStatus.SUCCESS);
                return "PAID_LATE_SEAT_RESTORED";
            }

            // Không đủ vé: trả lại phần vừa giữ, đánh dấu cần hoàn tiền
            for (OrderItem item : reserved) {
                ticketRepository.decreaseSoldQuantity(item.getTicket().getId(), item.getQuantity());
            }
            payment.setStatus(PaymentStatus.REFUND_REQUIRED);
            return "PAID_LATE_REFUND_REQUIRED";
        }

        // 7. Đơn đã PAID từ giao dịch khác (khách trả 2 lần): lần này cần hoàn tiền
        payment.setStatus(PaymentStatus.REFUND_REQUIRED);
        return "ALREADY_PAID_REFUND_REQUIRED";
    }

    /**
     * Xuất vé cho đơn vừa PAID: mỗi vé một dòng issued_tickets, mỗi dòng một qrSecret ngẫu nhiên.
     * Chạy trong transaction của handleTransfer: lỗi ở đây thì đơn cũng không thành PAID, không có đơn PAID mà thiếu vé.
     */
    private void issueTickets(Order order) {

        // Chốt chặn: đơn đã có vé thì không xuất lần hai
        if (issuedTicketRepository.existsByOrderId(order.getId())) {
            return;
        }

        List<IssuedTicket> tickets = new ArrayList<>();
        int index = 1;

        for (OrderItem item : order.getItems()) {
            for (int i = 0; i < item.getQuantity(); i++) {
                tickets.add(IssuedTicket.builder()
                        .order(order)
                        .orderItem(item)
                        .ticket(item.getTicket())
                        .event(order.getEvent())
                        .code(order.getCode() + "-" + index++)
                        .qrSecret(newQrSecret())
                        .build());
            }
        }

        issuedTicketRepository.saveAll(tickets);
    }

    // 32 byte ngẫu nhiên → chuỗi 43 ký tự, không đoán được nên không ai tự tạo QR giả được
    private String newQrSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}