package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EventCriteriaCustomRequest {
    private String name;
    private String description;
    private Double maxScore;
    private Double weight;
    private boolean isActive;
}