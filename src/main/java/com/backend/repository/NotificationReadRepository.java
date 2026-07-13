package com.backend.repository;

import com.backend.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    List<NotificationRead> findByUserIdAndNotificationIdIn(Long userId, Collection<Long> notificationIds);
    boolean existsByUserIdAndNotificationId(Long userId, Long notificationId);
}
