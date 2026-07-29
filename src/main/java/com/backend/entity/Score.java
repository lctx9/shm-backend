package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scores_submission_judge",
                columnNames = {"submission_id", "judge_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Score extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne
    @JoinColumn(name = "judge_id")
    private User judge;

    private Double scoreValue;

    @Column(columnDefinition = "TEXT")
    private String criteriaScoresJson;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
