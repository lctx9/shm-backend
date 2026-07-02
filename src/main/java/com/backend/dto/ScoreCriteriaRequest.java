package com.backend.dto;

import java.util.UUID;

public class ScoreCriteriaRequest {
    private UUID criteriaId;
    private Double score;

    // Getters and Setters
    public UUID getCriteriaId() { return criteriaId; }
    public void setCriteriaId(UUID criteriaId) { this.criteriaId = criteriaId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}