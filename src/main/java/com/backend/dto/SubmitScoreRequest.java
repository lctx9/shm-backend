package com.backend.dto;

import java.util.List;
import java.util.UUID;

public class SubmitScoreRequest {
    private UUID submissionId;
    private List<ScoreCriteriaRequest> criteriaScores;

    // Getters and Setters
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public List<ScoreCriteriaRequest> getCriteriaScores() { return criteriaScores; }
    public void setCriteriaScores(List<ScoreCriteriaRequest> criteriaScores) { this.criteriaScores = criteriaScores; }
}