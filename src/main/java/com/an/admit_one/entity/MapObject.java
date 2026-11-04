package com.an.admit_one.entity;

import com.an.admit_one.enums.MapObjectType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

// Thứ trên sơ đồ không bán vé: sân khấu, màn LED, FOH, khối khán đài đóng, chữ ghi chú
@Entity
@Table(name = "map_objects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapObject {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MapObjectType type;

    @Column(length = 50)
    private String label;

    // Cùng cách lưu với Zone: đa giác JSON tương đối với (x, y)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String shape;

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Column(nullable = false)
    private Double rotation = 0.0;
}
