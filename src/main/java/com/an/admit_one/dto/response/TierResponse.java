package com.an.admit_one.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TierResponse {
    private UUID id;
    private String name;
    private Long price;
    private String color;
    private String areaGroup;
    private Integer sortOrder;
}
