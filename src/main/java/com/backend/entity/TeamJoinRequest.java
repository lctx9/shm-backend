package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_join_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TeamJoinRequest extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String status;

    @Column(name = "type")
    private String type;
}
