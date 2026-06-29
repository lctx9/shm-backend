package com.backend.entity;

import com.backend.entity.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "hackathon_events")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HackathonEvent extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name; // VD: "SEAL Spring 2025"

    @Column(name = "season", length = 20)
    private String season; // SPRING, SUMMER, FALL

    @Column(name = "academic_year", length = 10)
    private String academicYear; // VD: "2025"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    // Coordinator tạo event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Track> tracks = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Round> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventScoringCriteria> scoringCriteriaList = new ArrayList<>();
}