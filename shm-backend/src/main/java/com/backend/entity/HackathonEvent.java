package com.backend.entity;

import com.backend.entity.enums.Season;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class HackathonEvent extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Season season;

    private int year;

    private LocalDateTime regStartDate;
    private LocalDateTime regEndDate;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;

    private boolean isActive = true;
}