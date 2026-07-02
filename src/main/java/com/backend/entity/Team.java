package com.backend.entity;

import com.backend.entity.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.*;

@Entity
@Table(name = "teams")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TeamStatus status = TeamStatus.APPROVED;

    @Column(name = "disqualification_reason", columnDefinition = "TEXT")
    private String disqualificationReason;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Submission> submissions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private com.backend.entity.enums.TeamVisibility visibility = com.backend.entity.enums.TeamVisibility.PUBLIC;

    @Column(name = "pin_code", length = 4)
    private String pinCode;
}