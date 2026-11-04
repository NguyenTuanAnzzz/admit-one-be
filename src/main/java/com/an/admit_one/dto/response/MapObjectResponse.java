package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MapObjectResponse {
    private UUID id;
    private String type;
    private String label;
    private List<List<Double>> shape;
    private Double x;
    private Double y;
    private Double rotation;
}
