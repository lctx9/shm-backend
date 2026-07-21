package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rule_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class RuleTemplate extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
