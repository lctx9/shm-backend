package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Track extends BaseEntity {
    private String name;
    private String description;
    private Integer maxTeams;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private HackathonEvent event;

    @ManyToMany
    @JoinTable(
            name = "track_mentors",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "mentor_id")
    )
    private java.util.Set<User> mentors;
}
