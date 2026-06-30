package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

// Điểm của từng Judge cho từng tiêu chí - cốt lõi của RBL research
@Entity
@Table(
        name = "judge_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id", "judge_id", "criteria_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class JudgeScore extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

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

    @Column(name = "is_calibration", nullable = false)
    @Builder.Default
    private boolean isCalibration = false; // RBL: đánh dấu điểm hiệu chuẩn
}