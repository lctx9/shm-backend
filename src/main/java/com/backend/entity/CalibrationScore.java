package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
        name = "calibration_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"calibration_submission_id", "judge_id", "criteria_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalibrationScore extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_submission_id", nullable = false)
    private CalibrationSubmission calibrationSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", nullable = false)
    private EventScoringCriteria criteria;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}