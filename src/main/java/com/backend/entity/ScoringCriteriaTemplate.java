package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "scoring_criteria_templates")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScoringCriteriaTemplate extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name; // VD: "Technical Implementation"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    // THÊM MỚI: Trọng số của tiêu chí chấm điểm (VD: 0.3, 0.4) phục vụ tính điểm hệ số
    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Double weight = 1.0;

    // THÊM MỚI: Liên kết tiêu chí này thuộc về Vòng thi (Round) nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}