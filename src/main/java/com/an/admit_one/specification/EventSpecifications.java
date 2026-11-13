package com.an.admit_one.specification;

import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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
    // Lọc theo khoảng ngày diễn
    public static Specification<Event> hasEventDateRange(
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {

            ZoneId zone = ZoneId.systemDefault();

            if (from == null && to == null) {
                return cb.conjunction();
            }

            if (from != null && to != null) {
                Instant start = from.atStartOfDay(zone).toInstant();
                Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

                return cb.and(
                        cb.greaterThanOrEqualTo(root.get("startsAt"), start),
                        cb.lessThan(root.get("startsAt"), end)
                );
            }

            if (from != null) {
                Instant start = from.atStartOfDay(zone).toInstant();

                return cb.greaterThanOrEqualTo(root.get("startsAt"), start);
            }

            Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

            return cb.lessThan(root.get("startsAt"), end);
        };
    }

    // Chỉ lấy sự kiện được phép công khai
    public static Specification<Event> hasPublicStatus() {
        return (root, query, cb) ->
                root.get("status").in(
                        EventStatus.UPCOMING,
                        EventStatus.ON_SALE,
                        EventStatus.SOLD_OUT,
                        EventStatus.ENDED
                );
    }

    // Lọc theo trạng thái Public mà khách yêu cầu
    public static Specification<Event> hasPublicStatus(EventStatus status) {
        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            if (!List.of(
                    EventStatus.UPCOMING,
                    EventStatus.ON_SALE,
                    EventStatus.SOLD_OUT,
                    EventStatus.ENDED
            ).contains(status)) {
                return cb.disjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Event> checkinEligible() {
        return (root, query, cb) -> root.get("status").in(
                EventStatus.ON_SALE, EventStatus.SOLD_OUT, EventStatus.ENDED
        );
    }
}