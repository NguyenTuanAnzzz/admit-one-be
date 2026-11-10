package com.an.admit_one.entity;

import com.an.admit_one.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // VNPAY, MOCK...
    @Column(nullable = false, length = 20)
    private String gateway;

    // Mã giao dịch của cổng. UNIQUE: webhook gửi trùng 50 lần thì chỉ lưu được 1 dòng
    @Column(name = "gateway_txn_ref", nullable = false, unique = true, length = 100)
    private String gatewayTxnRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // Toàn bộ dữ liệu cổng gửi về, để tra lại khi có khiếu nại
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}