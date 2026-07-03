package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class TrackResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer advancementSlots;
}