package com.an.admit_one.service;

import com.an.admit_one.dto.response.OrderStatusCountResponse;
import com.an.admit_one.dto.response.OverviewResponse;
import com.an.admit_one.dto.response.RevenueByDayResponse;
import com.an.admit_one.dto.response.TopEventResponse;
import com.an.admit_one.enums.OrderStatus;
import com.an.admit_one.repository.OrderItemRepository;
import com.an.admit_one.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OverviewService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Dashboard › Tổng quan (A-04): doanh thu + vé đã bán tính trên đơn PAID, vé đang giữ tính trên đơn PENDING
    public OverviewResponse getOverview() {
        return new OverviewResponse(
                orderRepository.sumTotalAmountByStatus(OrderStatus.PAID),
                orderItemRepository.sumQuantityByOrderStatus(OrderStatus.PAID),
                orderItemRepository.sumQuantityByOrderStatus(OrderStatus.PENDING)
        );
    }


    public List<RevenueByDayResponse> getRevenueByDay(int days) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Instant from = today.minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Map<String, BigDecimal> byDay = new HashMap<>();
        for (Object[] row : orderRepository.sumRevenueByDay(from)) {
            byDay.put(row[0].toString(), (BigDecimal) row[1]);
        }

        List<RevenueByDayResponse> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            result.add(new RevenueByDayResponse(date, byDay.getOrDefault(date, BigDecimal.ZERO)));
        }
        return result;
    }

    // Số đơn theo từng trạng thái. Điền 0 cho trạng thái chưa có đơn nào, để pie chart luôn đủ 4 phần
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : orderRepository.countOrdersByStatus()) {
            counts.put(((OrderStatus) row[0]).name(), (Long) row[1]);
        }

        List<OrderStatusCountResponse> result = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            result.add(new OrderStatusCountResponse(status.name(), counts.getOrDefault(status.name(), 0L)));
        }
        return result;
    }

    // Top sự kiện theo doanh thu (chỉ tính đơn đã thanh toán)
    public List<TopEventResponse> getTopEvents(int limit) {
        List<TopEventResponse> result = new ArrayList<>();
        for (Object[] row : orderRepository.sumRevenueByEvent(PageRequest.of(0, limit))) {
            result.add(new TopEventResponse((String) row[0], (BigDecimal) row[1]));
        }
        return result;
    }
}