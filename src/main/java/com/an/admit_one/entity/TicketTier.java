package com.an.admit_one.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

// Hạng vé: giá và màu. Nhiều khu dùng chung một hạng (Đại Cao Thủ (A), (B), (C) cùng 799.000đ), đổi giá chỉ sửa một chỗ
@Entity
@Table(name = "ticket_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketTier {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    // Tiền VND, dùng Long không dùng double để không sai số khi cộng tiền
    @Column(nullable = false)
    private Long price;

    // Mã màu hex, ví dụ #F5B8E8. Tô khu trên sơ đồ và ô chú thích
    @Column(nullable = false, length = 7)
    private String color;

    // Nhóm trên chú thích, ví dụ "Lòng sân", "Khán đài". Không bắt buộc
    @Column(length = 50)
    private String areaGroup;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
