package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "event_scoring_criteria")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EventScoringCriteria extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HackathonEvent event;

    // null nếu là tiêu chí tạo mới không từ template
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ScoringCriteriaTemplate template;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Double weight = 1.0; // Trọng số tiêu chí trong event này

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}