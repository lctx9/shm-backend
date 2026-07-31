package com.backend.repository;

import com.backend.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    List<NotificationRead> findByUserIdAndNotificationIdIn(Long userId, Collection<Long> notificationIds);
    boolean existsByUserIdAndNotificationId(Long userId, Long notificationId);
    java.util.Optional<NotificationRead> findByUserIdAndNotificationId(Long userId, Long notificationId);

    @Modifying
    @Query(value = """
            INSERT INTO notification_reads (notification_id, user_id, dismissed, created_at, updated_at)
            VALUES (:notificationId, :userId, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (notification_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int markAsReadIfAbsent(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId
    );
}

