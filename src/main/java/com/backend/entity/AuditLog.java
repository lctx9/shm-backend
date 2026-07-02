package com.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID actorId;        // Người thực hiện hành động (Judge, Coordinator, Admin)
    private String actionType;   // "SCORING", "UPDATE_SCORE", "DISQUALIFY_TEAM", "REJECT_SUBMISSION", "ADVANCEMENT"

    private UUID eventId;        // Liên kết lọc theo Event
    private UUID teamId;         // Liên kết lọc theo Team
    private UUID submissionId;   // Liên kết lọc theo Submission

    @Column(length = 1000)
    private String description;  // Mô tả chi tiết (ví dụ: "Điểm cũ: 7.0 -> Điểm mới: 8.5", hoặc lý do loại)
    private LocalDateTime timestamp;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}