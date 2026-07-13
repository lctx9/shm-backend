package com.backend.config;

import com.backend.entity.Notification;
import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
import com.backend.repository.NotificationRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StaffRoleMigration implements ApplicationRunner {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        jdbcTemplate.execute("""
                ALTER TABLE users ADD CONSTRAINT users_role_check
                CHECK (role IN ('USER', 'STAFF', 'COORDINATOR', 'ADMIN', 'MEMBER', 'LEADER', 'MENTOR', 'JUDGE'))
                """);
        jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_target_role_check");
        jdbcTemplate.execute("""
                ALTER TABLE notifications ADD CONSTRAINT notifications_target_role_check
                CHECK (target_role IS NULL OR target_role IN ('USER', 'STAFF', 'COORDINATOR', 'ADMIN', 'MEMBER', 'LEADER', 'MENTOR', 'JUDGE'))
                """);

        List<User> legacyStaff = userRepository.findByRoleIn(List.of(RoleType.MENTOR, RoleType.JUDGE));
        legacyStaff.forEach(user -> user.setRole(RoleType.STAFF));
        userRepository.saveAll(legacyStaff);

        List<User> legacyParticipants = userRepository.findByRoleIn(List.of(RoleType.MEMBER, RoleType.LEADER));
        legacyParticipants.forEach(user -> user.setRole(RoleType.USER));
        userRepository.saveAll(legacyParticipants);

        List<Notification> legacyNotifications = notificationRepository.findAll().stream()
                .filter(item -> item.getTargetRole() == RoleType.MENTOR || item.getTargetRole() == RoleType.JUDGE)
                .toList();
        legacyNotifications.forEach(item -> item.setTargetRole(RoleType.STAFF));
        notificationRepository.saveAll(legacyNotifications);

        List<Notification> legacyParticipantNotifications = notificationRepository.findAll().stream()
                .filter(item -> item.getTargetRole() == RoleType.MEMBER || item.getTargetRole() == RoleType.LEADER)
                .toList();
        legacyParticipantNotifications.forEach(item -> item.setTargetRole(RoleType.USER));
        notificationRepository.saveAll(legacyParticipantNotifications);
    }
}
