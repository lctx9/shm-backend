package com.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_reads", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"notification_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationRead extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
