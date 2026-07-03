package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Round extends BaseEntity {
    private String name;
    private int orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private HackathonEvent event;
}