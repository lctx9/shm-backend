package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Submission extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "matrix_id")
    private TrackRoundMatrix matrix;

    private String fileUrl;

    private boolean isFlagged;
    private String flagReason;
}