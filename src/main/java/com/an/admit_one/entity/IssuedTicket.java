package com.an.admit_one.entity;

import com.an.admit_one.enums.IssuedTicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issued_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuedTicket {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // Loại vé (VIP, Thường...)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Mã vé cho người đọc: mã đơn + số thứ tự, ví dụ AO1794336283660763-1
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    // Chuỗi bí mật in trong QR. Ngẫu nhiên, không đoán được, nên không ai tự tạo QR giả được
    @Column(name = "qr_secret", nullable = false, unique = true, length = 64)
    private String qrSecret;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedTicketStatus status = IssuedTicketStatus.VALID;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    // Staff đã quét vé này
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by")
    private User checkedInBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = IssuedTicketStatus.VALID;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}