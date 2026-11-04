package com.an.admit_one.service;

import com.an.admit_one.dto.request.BlockedSeatRequest;
import com.an.admit_one.dto.request.MapObjectRequest;
import com.an.admit_one.dto.request.SeatMapRequest;
import com.an.admit_one.dto.request.TierRequest;
import com.an.admit_one.dto.request.ZoneRequest;
import com.an.admit_one.dto.response.MapObjectResponse;
import com.an.admit_one.dto.response.SeatMapResponse;
import com.an.admit_one.dto.response.SeatResponse;
import com.an.admit_one.dto.response.TierResponse;
import com.an.admit_one.dto.response.ZoneResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.MapObject;
import com.an.admit_one.entity.Seat;
import com.an.admit_one.entity.TicketTier;
import com.an.admit_one.entity.Zone;
import com.an.admit_one.enums.EventStatus;
import com.an.admit_one.enums.SeatLayout;
import com.an.admit_one.enums.SeatStatus;
import com.an.admit_one.enums.SeatingMode;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatMapService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketTierRepository tierRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private MapObjectRepository mapObjectRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private static final int MAX_SEATS_PER_EVENT = 50_000;

    // Khoảng cách giữa tâm hai ghế cạnh nhau, đơn vị toạ độ sơ đồ. FE dùng đúng số này để xem trước
    private static final double SEAT_GAP = 24;

    // Bán kính hàng đầu tiên khi xếp vòng cung
    private static final double ARC_INNER_RADIUS = 300;


    private Set<String> rowLabelsOf(int rowCount) {
        Set<String> labels = new HashSet<>();
        for (int i = 0; i < rowCount; i++) {
            labels.add(rowLabel(i));
        }
        return labels;
    }

    // 0 → "A", 25 → "Z", 26 → "AA", 27 → "AB"
    private String rowLabel(int index) {
        String label = "";
        int n = index;
        do {
            label = (char) ('A' + n % 26) + label;
            n = n / 26 - 1;
        } while (n >= 0);
        return label;
    }

    @Transactional
    public SeatMapResponse save(UUID eventId, SeatMapRequest request){
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new AppException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy sự kiện"
        ));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Chỉ sửa được sơ đồ khi sự kiện còn là bản nháp"
            );
        }

        Set<String> tierKeys = new HashSet<>();

        for (TierRequest tier : request.getTiers()) {
            if (!tierKeys.add(tier.getKey())) {            // add trả false khi key đã có trong Set
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Trùng key hạng vé: " + tier.getKey());
            }
        }

        int totalSeats = 0;
        for (ZoneRequest zone : request.getZones()) {
            if (!tierKeys.contains(zone.getTierKey())) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Khu " + zone.getLabel() + " trỏ tới hạng vé không tồn tại");
            }
            totalSeats += zone.getRowCount() * zone.getSeatsPerRow();
        }

        if (totalSeats > MAX_SEATS_PER_EVENT) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Sơ đồ tối đa " + MAX_SEATS_PER_EVENT + " ghế, hiện có " + totalSeats);
        }
        for (BlockedSeatRequest blocked : request.getBlockedSeats()) {
            if (blocked.getZoneIndex() >= request.getZones().size()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Ghế khoá trỏ tới khu không tồn tại");
            }
            ZoneRequest zone = request.getZones().get(blocked.getZoneIndex());

            if (blocked.getSeatNumber() > zone.getSeatsPerRow()) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Khu " + zone.getLabel() + " chỉ có " + zone.getSeatsPerRow() + " ghế mỗi hàng");
            }
            if (!rowLabelsOf(zone.getRowCount()).contains(blocked.getRowLabel())) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Khu " + zone.getLabel() + " không có hàng " + blocked.getRowLabel());
            }
        }

        // Bước 3: xoá sơ đồ cũ theo thứ tự khoá ngoại: ghế trỏ tới khu, khu trỏ tới hạng vé
        seatRepository.deleteByEventId(eventId);
        zoneRepository.deleteByEventId(eventId);
        mapObjectRepository.deleteByEventId(eventId);
        tierRepository.deleteByEventId(eventId);

        // Bước 4: khung toạ độ của sơ đồ
        event.setMapWidth(request.getMapWidth());
        event.setMapHeight(request.getMapHeight());
        event.setMapBackgroundUrl(request.getMapBackgroundUrl());
        eventRepository.save(event);

        // Bước 5: lưu hạng vé, nhớ key FE đặt ứng với hạng nào để khu tra ở bước 7
        Map<String, TicketTier> tierByKey = new HashMap<>();
        for (int i = 0; i < request.getTiers().size(); i++) {
            TierRequest t = request.getTiers().get(i);

            TicketTier tier = new TicketTier();
            tier.setEvent(event);
            tier.setName(t.getName().trim());
            tier.setPrice(t.getPrice());
            tier.setColor(t.getColor());
            tier.setAreaGroup(t.getAreaGroup());
            tier.setSortOrder(i);
            tierRepository.save(tier);

            tierByKey.put(t.getKey(), tier);
        }

        // Bước 6: gom ghế khoá theo khu, zoneIndex → {"B-7", "B-8"}
        Map<Integer, Set<String>> blockedByZone = new HashMap<>();
        for (BlockedSeatRequest b : request.getBlockedSeats()) {
            blockedByZone
                    .computeIfAbsent(b.getZoneIndex(), k -> new HashSet<>())
                    .add(b.getRowLabel() + "-" + b.getSeatNumber());
        }

        // Bước 7: lưu từng khu rồi sinh ghế cho khu đó. Gom hết ghế lại, lưu một lần ở cuối
        List<Seat> allSeats = new ArrayList<>();
        for (int i = 0; i < request.getZones().size(); i++) {
            ZoneRequest z = request.getZones().get(i);

            Zone zone = new Zone();
            zone.setEvent(event);
            zone.setTier(tierByKey.get(z.getTierKey()));
            zone.setLabel(z.getLabel().trim());
            // List<List<Double>> → chuỗi "[[0.0,0.0],[120.0,0.0],...]" để lưu vào cột TEXT
            zone.setShape(objectMapper.writeValueAsString(z.getShape()));
            zone.setX(z.getX());
            zone.setY(z.getY());
            // FE gửi thẳng null thì giá trị mặc định trong DTO bị ghi đè, nên phòng hờ ở đây
            zone.setRotation(z.getRotation() != null ? z.getRotation() : 0.0);
            zone.setSeatingMode(z.getSeatingMode() != null ? z.getSeatingMode() : SeatingMode.RESERVED);
            zone.setRowCount(z.getRowCount());
            zone.setSeatsPerRow(z.getSeatsPerRow());
            zone.setSeatLayout(z.getSeatLayout() != null ? z.getSeatLayout() : SeatLayout.GRID);
            zoneRepository.save(zone);

            allSeats.addAll(generateSeats(event, zone, blockedByZone.getOrDefault(i, Set.of())));
        }
        seatRepository.saveAll(allSeats);

        // Bước 8: lưu vật thể không bán (sân khấu, LED, FOH...)
        for (MapObjectRequest o : request.getObjects()) {
            MapObject object = new MapObject();
            object.setEvent(event);
            object.setType(o.getType());
            object.setLabel(o.getLabel());
            object.setShape(objectMapper.writeValueAsString(o.getShape()));
            object.setX(o.getX());
            object.setY(o.getY());
            object.setRotation(o.getRotation() != null ? o.getRotation() : 0.0);
            mapObjectRepository.save(object);
        }

        // Bước 9: đọc lại đúng thứ vừa lưu, kèm id thật và số ghế từng khu
        return get(eventId);
    }

    // Sơ đồ của một sự kiện: hạng vé, khu (kèm số ghế), vật thể. Ghế từng khu lấy riêng qua getSeats
    @Transactional(readOnly = true)
    public SeatMapResponse get(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new AppException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy sự kiện"
        ));

        // Đếm ghế của mọi khu bằng một câu GROUP BY, rồi xếp lại: zoneId → (trạng thái → số ghế)
        Map<UUID, Map<SeatStatus, Long>> countsByZone = new HashMap<>();
        for (SeatRepository.ZoneStatusCount c : seatRepository.countByZoneAndStatus(eventId)) {
            countsByZone
                    .computeIfAbsent(c.getZoneId(), k -> new EnumMap<>(SeatStatus.class))
                    .put(c.getStatus(), c.getTotal());
        }

        List<TierResponse> tiers = new ArrayList<>();
        for (TicketTier t : tierRepository.findByEventIdOrderBySortOrderAsc(eventId)) {
            tiers.add(new TierResponse(t.getId(), t.getName(), t.getPrice(), t.getColor(), t.getAreaGroup(), t.getSortOrder()));
        }

        List<ZoneResponse> zones = new ArrayList<>();
        int totalSeats = 0;
        for (Zone zone : zoneRepository.findByEventId(eventId)) {
            Map<SeatStatus, Long> counts = countsByZone.getOrDefault(zone.getId(), Map.of());
            int zoneTotal = 0;
            for (Long n : counts.values()) {
                zoneTotal += n.intValue();
            }
            totalSeats += zoneTotal;

            zones.add(new ZoneResponse(
                    zone.getId(),
                    // Lấy id của hạng vé LAZY không làm Hibernate tải cả hạng vé lên
                    zone.getTier().getId(),
                    zone.getLabel(),
                    readShape(zone.getShape()),
                    zone.getX(),
                    zone.getY(),
                    zone.getRotation(),
                    zone.getSeatingMode().name(),
                    zone.getRowCount(),
                    zone.getSeatsPerRow(),
                    zone.getSeatLayout().name(),
                    zoneTotal,
                    counts.getOrDefault(SeatStatus.AVAILABLE, 0L).intValue(),
                    counts.getOrDefault(SeatStatus.BLOCKED, 0L).intValue()
            ));
        }

        List<MapObjectResponse> objects = new ArrayList<>();
        for (MapObject o : mapObjectRepository.findByEventId(eventId)) {
            objects.add(new MapObjectResponse(
                    o.getId(), o.getType().name(), o.getLabel(), readShape(o.getShape()),
                    o.getX(), o.getY(), o.getRotation()
            ));
        }

        return new SeatMapResponse(
                event.getId(),
                event.getStatus().name(),
                event.getMapWidth(),
                event.getMapHeight(),
                event.getMapBackgroundUrl(),
                tiers,
                zones,
                objects,
                totalSeats
        );
    }

    // Ghế của một khu, xếp theo hàng rồi số ghế. Khu phải thuộc đúng sự kiện trên URL
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(UUID eventId, UUID zoneId) {
        zoneRepository.findByIdAndEventId(zoneId, eventId).orElseThrow(() -> new AppException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy khu"
        ));

        List<SeatResponse> seats = new ArrayList<>();
        for (Seat s : seatRepository.findByZoneIdOrderByRowLabelAscSeatNumberAsc(zoneId)) {
            seats.add(new SeatResponse(s.getId(), s.getRowLabel(), s.getSeatNumber(), s.getX(), s.getY(), s.getStatus().name()));
        }
        return seats;
    }

    // Sinh rowCount x seatsPerRow ghế cho một khu, ghế nằm trong blocked ("B-7") thì khoá luôn
    private List<Seat> generateSeats(Event event, Zone zone, Set<String> blocked) {
        List<Seat> seats = new ArrayList<>();
        for (int row = 0; row < zone.getRowCount(); row++) {
            String label = rowLabel(row);
            for (int col = 0; col < zone.getSeatsPerRow(); col++) {
                int number = col + 1;
                double[] p = zone.getSeatLayout() == SeatLayout.ARC
                        ? arcPosition(row, col, zone.getSeatsPerRow())
                        : gridPosition(row, col);

                Seat seat = new Seat();
                seat.setEvent(event);
                seat.setZone(zone);
                seat.setRowLabel(label);
                seat.setSeatNumber(number);
                seat.setX(p[0]);
                seat.setY(p[1]);
                seat.setStatus(blocked.contains(label + "-" + number) ? SeatStatus.BLOCKED : SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        return seats;
    }

    // Hàng thẳng như rạp phim: mỗi ghế cách nhau SEAT_GAP, toạ độ là tâm ghế
    private double[] gridPosition(int row, int col) {
        return new double[]{
                col * SEAT_GAP + SEAT_GAP / 2,
                row * SEAT_GAP + SEAT_GAP / 2
        };
    }

    // Vòng cung quanh sân khấu: mỗi hàng là một cung tròn, hàng sau bán kính lớn hơn nên ghế giãn ra
    private double[] arcPosition(int row, int col, int seatsPerRow) {
        double radius = ARC_INNER_RADIUS + row * SEAT_GAP;
        // Góc mở của cả hàng (radian), tính theo hàng đầu để ghế hàng đầu cách nhau đúng SEAT_GAP
        double span = (seatsPerRow - 1) * SEAT_GAP / ARC_INNER_RADIUS;
        double angle = seatsPerRow == 1 ? 0 : -span / 2 + col * (span / (seatsPerRow - 1));
        return new double[]{
                radius * Math.sin(angle),
                radius * Math.cos(angle) - ARC_INNER_RADIUS
        };
    }

    // Chuỗi JSON trong cột shape → danh sách điểm. TypeReference để Jackson biết kiểu bên trong List
    private List<List<Double>> readShape(String shape) {
        return objectMapper.readValue(shape, new TypeReference<List<List<Double>>>() {});
    }

}
