package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.*;

@Entity
@Table(name = "tracks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Track extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private HackathonEvent event;

    @Column(name = "name", nullable = false, length = 255)
    private String name; // VD: "AI/ML Track", "FinTech Track"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Số đội tối đa được vào vòng tiếp theo từ track này
    @Column(name = "advancement_slots")
    private Integer advancementSlots;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrackMentorAssignment> mentorAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Team> teams = new ArrayList<>();
}