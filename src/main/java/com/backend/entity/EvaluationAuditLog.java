package com.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_audit_logs")
public class EvaluationAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID judgeId;
    private UUID submissionId;
    private UUID criteriaId;
    private Double oldScore;
    private Double newScore;
    private String actionType; // "CREATE" hoặc "UPDATE"
    private LocalDateTime timestamp;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJudgeId() { return judgeId; }
    public void setJudgeId(UUID judgeId) { this.judgeId = judgeId; }
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public UUID getCriteriaId() { return criteriaId; }
    public void setCriteriaId(UUID criteriaId) { this.criteriaId = criteriaId; }
    public Double getOldScore() { return oldScore; }
    public void setOldScore(Double oldScore) { this.oldScore = oldScore; }
    public Double getNewScore() { return newScore; }
    public void setNewScore(Double newScore) { this.newScore = newScore; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}