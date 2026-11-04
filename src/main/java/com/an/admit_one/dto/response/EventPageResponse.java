package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Một trang danh sách sự kiện. Không trả thẳng Page của Spring: cấu trúc JSON của Page không cố định giữa các bản Spring,
 * FE dựa vào đây thì dễ vỡ khi nâng cấp.
 * page bắt đầu từ 0. statusCounts đếm trên mọi sự kiện (không theo bộ lọc) để các nút lọc trạng thái luôn có số.
 */
@Getter
@AllArgsConstructor
public class EventPageResponse {
    private List<EventSummaryResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private Map<String, Long> statusCounts;
}
