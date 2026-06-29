package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.*;

// Bài mẫu dùng trong vòng hiệu chuẩn - phục vụ RBL research
@Entity
@Table(name = "calibration_submissions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalibrationSubmission extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_url", length = 512)
    private String referenceUrl;

    @Column(name = "expected_score_notes", columnDefinition = "TEXT")
    private String expectedScoreNotes; // Ghi chú điểm chuẩn để so sánh

    @OneToMany(mappedBy = "calibrationSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalibrationScore> calibrationScores = new ArrayList<>();
}