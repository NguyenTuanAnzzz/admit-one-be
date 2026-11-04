package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

// GET /api/admin/events/{id}/seatmap (và trả về sau khi PUT). Ghế từng khu tải riêng qua SeatResponse
@Getter
@AllArgsConstructor
public class SeatMapResponse {
    private UUID eventId;
    private String eventStatus;
    private Integer mapWidth;
    private Integer mapHeight;
    private String mapBackgroundUrl;
    private List<TierResponse> tiers;
    private List<ZoneResponse> zones;
    private List<MapObjectResponse> objects;
    // Tổng số ghế của mọi khu, gồm cả ghế khoá
    private Integer totalSeats;
}
