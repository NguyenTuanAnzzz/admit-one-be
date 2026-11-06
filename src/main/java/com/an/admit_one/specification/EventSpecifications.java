package com.an.admit_one.specification;

import com.an.admit_one.entity.Event;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class EventSpecifications {

    // Tìm theo tên sự kiện hoặc địa điểm
    public static Specification<Event> search(String keyword) {
        return (root, query, cb) -> {

            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String search = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), search),
                    cb.like(cb.lower(root.get("venue")), search),
                    cb.like(cb.lower(root.get("address")), search)
            );
        };
    }

    // Lọc theo ngày diễn
    public static Specification<Event> hasEventDate(LocalDate eventDate) {
        return (root, query, cb) -> {

            if (eventDate == null) {
                return cb.conjunction();
            }

            ZoneId zone = ZoneId.systemDefault();

            Instant start = eventDate.atStartOfDay(zone).toInstant();
            Instant end = eventDate.plusDays(1).atStartOfDay(zone).toInstant();

            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("startsAt"), start),
                    cb.lessThan(root.get("startsAt"), end)
            );
        };
    }

    // Lọc theo ngày mở bán
    public static Specification<Event> hasSaleDate(LocalDate saleDate) {
        return (root, query, cb) -> {

            if (saleDate == null) {
                return cb.conjunction();
            }

            ZoneId zone = ZoneId.systemDefault();

            Instant start = saleDate.atStartOfDay(zone).toInstant();
            Instant end = saleDate.plusDays(1).atStartOfDay(zone).toInstant();

            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("saleOpensAt"), start),
                    cb.lessThan(root.get("saleOpensAt"), end)
            );
        };
    }
}