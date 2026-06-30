package com.backend.entity;

import com.backend.entity.enums.AuditActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    @Column(name = "target_entity", length = 100)
    private String targetEntity; // VD: "Submission", "Team"

    @Column(name = "target_id", columnDefinition = "uuid")
    private UUID targetId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // JSON

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON
}