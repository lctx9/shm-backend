package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
        name = "team_round_rankings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "round_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeamRoundRanking extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "rank_in_track")
    private Integer rankInTrack;

    @Column(name = "rank_overall")
    private Integer rankOverall;

    @Column(name = "is_advanced", nullable = false)
    @Builder.Default
    private boolean isAdvanced = false; // Đã thăng vòng chưa
}