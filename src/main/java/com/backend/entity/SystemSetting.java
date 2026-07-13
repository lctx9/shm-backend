package com.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SystemSetting extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String settingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;
}
