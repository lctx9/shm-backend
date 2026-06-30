package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
        name = "track_mentor_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"track_id", "mentor_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TrackMentorAssignment extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor; // User có role MENTOR
}