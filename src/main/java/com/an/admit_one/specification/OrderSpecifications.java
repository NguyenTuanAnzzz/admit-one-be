package com.an.admit_one.specification;

import com.an.admit_one.entity.Order;
import com.an.admit_one.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class OrderSpecifications {

    // Chỉ lấy đơn của một người
    public static Specification<Order> belongsTo(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // Lọc theo trạng thái đơn
    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }

    // Tìm theo mã đơn hoặc tên sự kiện
    public static Specification<Order> search(String keyword) {
        return (root, query, cb) -> {

            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String search = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("code")), search),
                    cb.like(cb.lower(root.get("event").get("name")), search)
            );
        };
    }

    // Tải luôn sự kiện trong cùng câu SQL, tránh mỗi đơn lại query thêm một lần
    public static Specification<Order> fetchEvent() {
        return (root, query, cb) -> {

            // Câu COUNT (đếm tổng để phân trang) không được fetch, nếu fetch Hibernate báo lỗi
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("event");
            }

            return cb.conjunction();
        };
    }
}