package com.an.admit_one.service;

import com.an.admit_one.dto.request.EventRequest;
import com.an.admit_one.dto.response.EventDetailResponse;
import com.an.admit_one.dto.response.EventPageResponse;
import com.an.admit_one.dto.response.EventSummaryResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import com.an.admit_one.repository.EventSpecifications;
import com.an.admit_one.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    public EventDetailResponse create(EventRequest request) {
        validateTimes(request);

        Event event = new Event();
        applyRequest(event, request);
        eventRepository.save(event);

        return toDetail(event);
    }

    // Chi tiết một sự kiện cho admin (có cả bản nháp), để điền sẵn form sửa
    public EventDetailResponse getForAdmin(UUID id) {
        return toDetail(findOrThrow(id));
    }

    // Chi tiết cho khách. Bản nháp trả 404 như không tồn tại; sự kiện đã huỷ vẫn cho xem để khách đã mua vé biết
    public EventDetailResponse getForPublic(UUID id) {
        Event event = findOrThrow(id);
        if (event.getStatus() == EventStatus.DRAFT) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện");
        }
        return toDetail(event);
    }

    // Sửa thông tin sự kiện. Sửa được ở mọi trạng thái (đổi mô tả, poster khi đang bán là bình thường).
    // Không đụng tới status: đổi trạng thái có API riêng. Không đụng tới sơ đồ ghế: lưu qua /seatmap
    public EventDetailResponse update(UUID id, EventRequest request) {
        validateTimes(request);

        Event event = findOrThrow(id);
        applyRequest(event, request);
        eventRepository.save(event);

        return toDetail(event);
    }

    /**
     * Các bước chuyển trạng thái hợp lệ. Không cho quay về DRAFT: chỉ DRAFT mới dựng lại được sơ đồ ghế,
     * quay về được thì có thể sinh lại ghế trong khi khách đã giữ hoặc mua ghế cũ. CANCELLED là trạng thái cuối.
     */
    private static final Map<EventStatus, Set<EventStatus>> TRANSITIONS = Map.of(
            EventStatus.DRAFT, Set.of(EventStatus.UPCOMING, EventStatus.ON_SALE),
            EventStatus.UPCOMING, Set.of(EventStatus.ON_SALE, EventStatus.CANCELLED),
            EventStatus.ON_SALE, Set.of(EventStatus.SOLD_OUT, EventStatus.CANCELLED),
            EventStatus.SOLD_OUT, Set.of(EventStatus.ON_SALE, EventStatus.CANCELLED),
            EventStatus.CANCELLED, Set.of()
    );

    private static final Map<EventStatus, String> STATUS_LABEL = Map.of(
            EventStatus.DRAFT, "Bản nháp",
            EventStatus.UPCOMING, "Sắp mở bán",
            EventStatus.ON_SALE, "Đang bán",
            EventStatus.SOLD_OUT, "Hết vé",
            EventStatus.CANCELLED, "Đã huỷ"
    );

    /**
     * Đổi trạng thái: công bố (DRAFT → UPCOMING/ON_SALE), mở bán, huỷ...
     * Rời DRAFT hoặc mở bán thì phải có sơ đồ ghế và giờ diễn chưa qua.
     */
    public EventDetailResponse changeStatus(UUID id, EventStatus target) {
        Event event = findOrThrow(id);
        EventStatus current = event.getStatus();

        if (current == target) {
            return toDetail(event);
        }

        if (!TRANSITIONS.get(current).contains(target)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Không thể chuyển từ " + STATUS_LABEL.get(current) + " sang " + STATUS_LABEL.get(target)
            );
        }

        boolean publishing = current == EventStatus.DRAFT;
        if (publishing || target == EventStatus.ON_SALE) {
            if (!seatRepository.existsByEventId(id)) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Sự kiện chưa có sơ đồ ghế, không công bố hay mở bán được"
                );
            }
            if (!event.getStartsAt().isAfter(Instant.now())) {
                throw new AppException(
                        HttpStatus.BAD_REQUEST,
                        "Sự kiện đã diễn ra, không công bố hay mở bán được"
                );
            }
        }

        event.setStatus(target);
        eventRepository.save(event);

        return toDetail(event);
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new AppException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy sự kiện"
        ));
    }

    // So sánh hai trường nên không dùng annotation được, kiểm tra ở đây. Dùng chung cho tạo và sửa
    private void validateTimes(EventRequest request) {
        if (!request.getSaleOpensAt().isBefore(request.getStartsAt())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Giờ mở bán phải trước giờ diễn"
            );
        }
    }

    // Chép dữ liệu từ request sang entity. Tạo và sửa dùng chung để hai bên không lệch nhau khi thêm trường mới
    private void applyRequest(Event event, EventRequest request) {
        event.setName(request.getName().trim());
        event.setVenue(request.getVenue().trim());
        event.setAddress(request.getAddress());
        event.setGoogleMapsUrl(request.getGoogleMapsUrl());
        event.setDescription(request.getDescription());
        event.setPosterUrl(request.getPosterUrl());
        event.setStartsAt(request.getStartsAt());
        event.setSaleOpensAt(request.getSaleOpensAt());
    }

    // Cách sắp xếp được phép, khoá là giá trị ?sort= trên URL. Chỉ nhận trong danh sách này để không ai sắp theo cột lạ
    private static final Map<String, Sort> SORTS = Map.of(
            "startsAt", Sort.by(Sort.Direction.ASC, "startsAt"),
            "-startsAt", Sort.by(Sort.Direction.DESC, "startsAt"),
            "createdAt", Sort.by(Sort.Direction.ASC, "createdAt"),
            "-createdAt", Sort.by(Sort.Direction.DESC, "createdAt"),
            "name", Sort.by(Sort.Direction.ASC, "name")
    );

    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Danh sách cho admin, có cả DRAFT và CANCELLED. Mọi tham số lọc đều không bắt buộc.
     * Danh sách cho khách sau này dùng lại EventSpecifications nhưng phải loại DRAFT và CANCELLED.
     * startsFrom/startsTo lọc theo giờ diễn, saleFrom/saleTo lọc theo giờ mở bán. FE chọn một ngày thì gửi đầu và cuối ngày đó.
     */
    public EventPageResponse searchForAdmin(
            EventStatus status, String q,
            Instant startsFrom, Instant startsTo,
            Instant saleFrom, Instant saleTo,
            int page, int size, String sort
    ) {
        validateSearch(q, startsFrom, startsTo, saleFrom, saleTo);

        return searchPage(
                EventSpecifications.filter(status, q, startsFrom, startsTo, saleFrom, saleTo),
                page, size, sort,
                List.of(EventStatus.values()),
                eventRepository.countByStatus()
        );
    }

    /**
     * Danh sách cho khách (không cần đăng nhập): chỉ UPCOMING, ON_SALE, SOLD_OUT.
     * Gửi ?status=DRAFT hay CANCELLED thì vẫn bị điều kiện visibleToCustomers chặn lại, ra danh sách rỗng.
     */
    public EventPageResponse searchForPublic(
            EventStatus status, String q,
            Instant startsFrom, Instant startsTo,
            int page, int size, String sort
    ) {
        validateSearch(q, startsFrom, startsTo, null, null);

        return searchPage(
                EventSpecifications.filter(status, q, startsFrom, startsTo, null, null)
                        .and(EventSpecifications.visibleToCustomers()),
                page, size, sort,
                EventSpecifications.PUBLIC_STATUSES,
                eventRepository.countByStatusIn(EventSpecifications.PUBLIC_STATUSES)
        );
    }

    private void validateSearch(String q, Instant startsFrom, Instant startsTo, Instant saleFrom, Instant saleTo) {
        if (startsFrom != null && startsTo != null && startsFrom.isAfter(startsTo)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng ngày diễn không hợp lệ");
        }
        if (saleFrom != null && saleTo != null && saleFrom.isAfter(saleTo)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng ngày mở bán không hợp lệ");
        }
        if (q != null && q.length() > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Từ khoá tìm kiếm tối đa 100 ký tự");
        }
    }

    // Phần chung của danh sách admin và khách: phân trang, sắp xếp, đếm theo trạng thái
    private EventPageResponse searchPage(
            Specification<Event> spec,
            int page, int size, String sort,
            List<EventStatus> countedStatuses,
            List<EventRepository.StatusCount> counts
    ) {
        // Kẹp page, size vào khoảng hợp lệ thay vì báo lỗi: gõ tay ?size=1000 trên URL thì vẫn chỉ lấy 50
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Sort safeSort = SORTS.getOrDefault(sort, SORTS.get("startsAt"));

        Page<Event> result = eventRepository.findAll(spec, PageRequest.of(safePage, safeSize, safeSort));

        List<EventSummaryResponse> items = new ArrayList<>();
        for (Event e : result.getContent()) {
            items.add(toSummary(e));
        }

        // Đủ các trạng thái được đếm, trạng thái chưa có sự kiện nào thì là 0, FE không phải kiểm tra thiếu khoá
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (EventStatus s : countedStatuses) {
            statusCounts.put(s.name(), 0L);
        }
        for (EventRepository.StatusCount c : counts) {
            statusCounts.put(c.getStatus().name(), c.getTotal());
        }

        return new EventPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                statusCounts
        );
    }

    private EventSummaryResponse toSummary(Event e) {
        return new EventSummaryResponse(
                e.getId(),
                e.getName(),
                e.getVenue(),
                e.getAddress(),
                e.getPosterUrl(),
                e.getStartsAt(),
                e.getSaleOpensAt(),
                e.getStatus().name()
        );
    }

    private EventDetailResponse toDetail(Event e) {
        return new EventDetailResponse(
                e.getId(),
                e.getName(),
                e.getVenue(),
                e.getAddress(),
                e.getGoogleMapsUrl(),
                e.getDescription(),
                e.getPosterUrl(),
                e.getStartsAt(),
                e.getSaleOpensAt(),
                e.getStatus().name(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
