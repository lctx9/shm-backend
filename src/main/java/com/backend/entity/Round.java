package com.backend.entity;

import com.backend.entity.enums.RoundType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "rounds")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Round extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HackathonEvent event;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // VD: "Vòng Sơ Khảo", "Vòng Chung Kết"

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 20)
    private RoundType roundType;

    @Column(name = "round_order", nullable = false)
    private Integer roundOrder; // Thứ tự vòng: 1, 2, 3...

    @Column(name = "submission_deadline")
    private LocalDateTime submissionDeadline;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // Top N đội mỗi track được thăng vòng
    @Column(name = "top_n_advancement")
    private Integer topNAdvancement;

    @Column(name = "is_calibration_round", nullable = false)
    @Builder.Default
    private boolean isCalibrationRound = false; // RBL: vòng hiệu chuẩn

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoundJudgeAssignment> judgeAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Submission> submissions = new ArrayList<>();
}