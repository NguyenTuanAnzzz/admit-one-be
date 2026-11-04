package com.an.admit_one.entity;

import com.an.admit_one.enums.SeatLayout;
import com.an.admit_one.enums.SeatingMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

// Một khối trên sơ đồ, thuộc một hạng vé. Ghế của khu lưu ở bảng seats, toạ độ ghế tính tương đối với khu
@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Zone {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier tier;

    // Tên riêng của khối, ví dụ "(A)", "Khán đài trái 3"
    @Column(nullable = false, length = 50)
    private String label;

    // Đa giác viền khu dạng JSON [[x,y],...], toạ độ tương đối với (x, y) của khu
    @Column(nullable = false, columnDefinition = "TEXT")
    private String shape;

    // Vị trí và góc xoay của khu trên khung sơ đồ (mapWidth x mapHeight của Event)
    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Column(nullable = false)
    private Double rotation = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatingMode seatingMode = SeatingMode.RESERVED;

    // Số hàng và số ghế mỗi hàng: backend sinh ghế từ hai số này. Đặt rowCount vì ROWS trùng từ khoá SQL
    @Column(nullable = false)
    private Integer rowCount;

    @Column(nullable = false)
    private Integer seatsPerRow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatLayout seatLayout = SeatLayout.GRID;
}
