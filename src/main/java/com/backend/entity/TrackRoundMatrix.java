package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "track_round_matrix")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TrackRoundMatrix extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne
    @JoinColumn(name = "round_id")
    private Round round;

    private String guidelineUrl;
    private LocalDateTime submissionDeadline;

    @ManyToMany
    @JoinTable(
            name = "matrix_mentors",
            joinColumns = @JoinColumn(name = "matrix_id"),
            inverseJoinColumns = @JoinColumn(name = "mentor_id")
    )
    private Set<User> mentors;

    @ManyToMany
    @JoinTable(
            name = "matrix_judges",
            joinColumns = @JoinColumn(name = "matrix_id"),
            inverseJoinColumns = @JoinColumn(name = "judge_id")
    )
    private Set<User> judges;
}