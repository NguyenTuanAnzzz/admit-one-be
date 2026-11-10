package com.an.admit_one.entity;

import com.an.admit_one.enums.EmailOutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Hàng đợi email (outbox): ghi "cần gửi email" trong cùng transaction với việc xuất vé,
 * scheduler gửi sau khi transaction đã commit.
 * - Transaction rollback thì dòng này rollback theo, không có email nào cho vé không tồn tại.
 * - UNIQUE (order_id, type): mỗi đơn chỉ có 1 email vé, webhook về bao nhiêu lần cũng không gửi trùng.
 */
@Entity
@Table(
        name = "email_outbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_outbox_order_type", columnNames = {"order_id", "type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutbox {

    // Loại email: vé điện tử sau khi thanh toán
    public static final String TYPE_TICKETS = "TICKETS";

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
    private String type;

    // Gửi tới đâu: lưu lại lúc tạo, khách đổi email sau đó cũng không ảnh hưởng email đang chờ
    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailOutboxStatus status = EmailOutboxStatus.PENDING;

    // Số lần đã thử gửi
    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;

    // Chưa tới giờ này thì chưa gửi (lần lỗi sau chờ lâu hơn lần trước)
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    // Lỗi của lần gửi gần nhất, để tra khi email không tới
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (status == null) {
            status = EmailOutboxStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
    }
}
