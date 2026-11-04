package com.an.admit_one.repository;

import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Điều kiện lọc sự kiện ghép động: tham số nào có thì mới thêm điều kiện đó vào WHERE.
 * Dùng Specification thay vì đặt tên hàm findByStatusAndNameContainingAnd..., vì mọi tham số đều không bắt buộc
 * nên tên hàm phải viết cho mọi tổ hợp.
 * Sự kiện có hai mốc thời gian: giờ diễn (startsAt) và giờ mở bán (saleOpensAt). Mỗi mốc lọc theo một khoảng [from, to].
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> filter(
            EventStatus status,
            String q,
            Instant startsFrom,
            Instant startsTo,
            Instant saleFrom,
            Instant saleTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }

            // Tìm theo tên hoặc địa điểm, không phân biệt hoa thường
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("venue")), pattern, '\\')
                ));
            }

            addRange(conditions, cb, root, "startsAt", startsFrom, startsTo);
            addRange(conditions, cb, root, "saleOpensAt", saleFrom, saleTo);

            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }

    // field >= from và field <= to, đầu nào null thì bỏ đầu đó
    private static void addRange(List<Predicate> conditions, CriteriaBuilder cb, Root<Event> root, String field, Instant from, Instant to) {
        if (from != null) {
            conditions.add(cb.greaterThanOrEqualTo(root.get(field), from));
        }
        if (to != null) {
            conditions.add(cb.lessThanOrEqualTo(root.get(field), to));
        }
    }

    // Người dùng gõ "50%" thì tìm đúng chữ "50%", không để % và _ thành ký tự đại diện của LIKE
    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
