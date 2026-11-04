package com.an.admit_one.entity;

import com.an.admit_one.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 255)
    private String venue;

    @Column(length = 255)
    private String address;

    // Link chỉ đường Google Maps tới địa điểm, trang chi tiết hiện nút "Chỉ đường"
    @Column(length = 500)
    private String googleMapsUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String posterUrl;

    @Column(nullable = false)
    private Instant startsAt;

    // Giờ mở bán: trang chi tiết đếm ngược tới mốc này (C-03)
    @Column(nullable = false)
    private Instant saleOpensAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    // Khung toạ độ của sơ đồ ghế, ví dụ 1200 x 800. Mọi x, y của khu và vật thể tính theo khung này. Null khi chưa có sơ đồ
    @Column
    private Integer mapWidth;

    @Column
    private Integer mapHeight;

    // Ảnh mặt bằng làm nền để admin đặt khu cho khớp, khách không thấy
    @Column(length = 500)
    private String mapBackgroundUrl;

    // Giới hạn vé mỗi tài khoản cho sự kiện này (spec 5.6). columnDefinition có default để thêm cột vào bảng đã có dữ liệu không lỗi
    @Column(nullable = false, columnDefinition = "integer not null default 4")
    private Integer maxTicketsPerUser = 4;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
