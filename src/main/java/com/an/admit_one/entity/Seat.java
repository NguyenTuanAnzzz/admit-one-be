package com.an.admit_one.entity;

import com.an.admit_one.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

// Giữ cả event_id (dù suy ra được từ zone) để truy vấn ghế theo sự kiện không phải join, và để bảng vé ràng buộc unique (event_id, seat_id)
@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "row_label", "seat_number"}),
        indexes = @Index(name = "idx_seats_event_zone", columnList = "event_id, zone_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    // Hàng A, B, C... và số ghế 1, 2, 3... Ghế hiện cho khách là "B-07"
    @Column(nullable = false, length = 5)
    private String rowLabel;

    @Column(nullable = false)
    private Integer seatNumber;

    // Toạ độ tâm ghế, tương đối với khu: kéo hay xoay khu thì ghế đi theo, không phải tính lại
    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;
}
