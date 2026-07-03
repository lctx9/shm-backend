package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Prize extends BaseEntity {

    private String name;
    private String description;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private HackathonEvent event;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}