package com.an.admit_one.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

// Staff được gán soát vé sự kiện nào (spec mục 8: staff_assignments). Đặc tả 3.2: Staff chỉ check-in được sự kiện được gán
@Entity
@Table(
        name = "staff_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "event_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
