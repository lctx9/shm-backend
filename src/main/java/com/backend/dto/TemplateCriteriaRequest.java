package com.backend.dto;

import lombok.Data;

@Data
public class TemplateCriteriaRequest {
    private String name;
    private String description;
    private Double maxScore;
    private Double defaultWeight;
}