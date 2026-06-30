package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "prizes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Prize extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HackathonEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "prize_name", nullable = false, length = 100)
    private String prizeName; // "Giải Nhất", "Giải Nhì", "Best Innovation"

    @Column(name = "prize_rank")
    private Integer prizeRank; // 1, 2, 3...

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_announced", nullable = false)
    @Builder.Default
    private boolean isAnnounced = false;
}