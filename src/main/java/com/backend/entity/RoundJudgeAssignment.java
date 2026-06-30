package com.backend.entity;

import com.backend.entity.enums.JudgeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
        name = "round_judge_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "judge_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoundJudgeAssignment extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @Enumerated(EnumType.STRING)
    @Column(name = "judge_type", nullable = false, length = 20)
    private JudgeType judgeType; // INTERNAL hoặc GUEST
}