package com.backend.controller;

import com.backend.entity.SystemActivity;
import com.backend.entity.SystemSetting;
import com.backend.entity.User;
import com.backend.entity.enums.AccountStatus;
import com.backend.entity.enums.RoleType;
import com.backend.repository.*;
import com.backend.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
public class AdminController {
    private static final Map<String, String> DEFAULT_SETTINGS = Map.of(
            "systemName", "SEAL Hackathon Management System",
            "supportEmail", "sealfpt@gmail.com",
            "maintenanceMode", "false",
            "registrationEnabled", "true",
            "sessionTimeoutMinutes", "120"
    );

    private final UserRepository userRepository;
    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final TrackRoundMatrixRepository matrixRepository;
    private final SystemSettingRepository settingRepository;
    private final SystemActivityRepository activityRepository;
    private final DatabaseBackupService backupService;
    private final DataSource dataSource;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Long> roles = new LinkedHashMap<>();
        for (RoleType role : List.of(RoleType.ADMIN, RoleType.COORDINATOR, RoleType.STAFF, RoleType.USER)) {
            roles.put(role.name(), userRepository.countByRole(role));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", userRepository.count());
        result.put("pendingUsers", userRepository.countByStatus(AccountStatus.PENDING));
        result.put("totalEvents", eventRepository.count());
        result.put("activeEvents", eventRepository.countByIsActiveTrue());
        result.put("totalTeams", teamRepository.count());
        result.put("totalSubmissions", submissionRepository.count());
        result.put("pendingSubmissions", submissionRepository.countByIsGradedFalse());
        result.put("staffAssignments", matrixRepository.findAll().stream()
                .mapToLong(matrix -> matrix.getMentors().size() + matrix.getJudges().size()).sum());
        result.put("roles", roles);
        result.put("recentActivities", activityRepository.findTop20ByOrderByCreatedAtDesc());
        return response(result);
    }

    @GetMapping("/users")
    public Map<String, Object> users() {
        return response(userRepository.findAll().stream().map(this::userView).toList());
    }

    @PutMapping("/users/{id}/role")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User target = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        RoleType newRole = RoleType.valueOf(body.getOrDefault("role", "").toUpperCase(Locale.ROOT));
        if (newRole == RoleType.MENTOR || newRole == RoleType.JUDGE || newRole == RoleType.MEMBER || newRole == RoleType.LEADER) {
            throw new RuntimeException("Chỉ dùng các role ADMIN, COORDINATOR, STAFF hoặc USER");
        }
        User actor = actor();
        if (target.getId().equals(actor.getId()) && newRole != RoleType.ADMIN) {
            throw new RuntimeException("Bạn không thể tự hạ quyền tài khoản đang đăng nhập");
        }
        if (target.getRole() == RoleType.ADMIN && newRole != RoleType.ADMIN && userRepository.countByRole(RoleType.ADMIN) <= 1) {
            throw new RuntimeException("Hệ thống phải còn ít nhất một Admin");
        }
        RoleType oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);
        log("ROLE_CHANGED", target.getEmail() + ": " + oldRole + " → " + newRole);
        return response(userView(target));
    }

    @PutMapping("/users/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User target = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        if (target.getId().equals(actor().getId())) {
            throw new RuntimeException("Bạn không thể khóa tài khoản đang đăng nhập");
        }
        AccountStatus status = AccountStatus.valueOf(body.getOrDefault("status", "").toUpperCase(Locale.ROOT));
        target.setStatus(status);
        target.setRejectionReason(body.get("reason"));
        userRepository.save(target);
        log("ACCOUNT_STATUS_CHANGED", target.getEmail() + " → " + status);
        return response(userView(target));
    }

    @GetMapping("/monitoring")
    public Map<String, Object> monitoring() {
        Runtime runtime = Runtime.getRuntime();
        boolean databaseOnline;
        try (Connection connection = dataSource.getConnection()) {
            databaseOnline = connection.isValid(2);
        } catch (Exception ignored) {
            databaseOnline = false;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", databaseOnline ? "HEALTHY" : "DEGRADED");
        result.put("database", databaseOnline ? "CONNECTED" : "DISCONNECTED");
        result.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        result.put("usedMemoryBytes", runtime.totalMemory() - runtime.freeMemory());
        result.put("maxMemoryBytes", runtime.maxMemory());
        result.put("processors", runtime.availableProcessors());
        result.put("checkedAt", Instant.now());
        result.put("records", Map.of("users", userRepository.count(), "events", eventRepository.count(),
                "teams", teamRepository.count(), "submissions", submissionRepository.count()));
        result.put("recentActivities", activityRepository.findTop20ByOrderByCreatedAtDesc());
        return response(result);
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        Map<String, String> result = new LinkedHashMap<>(DEFAULT_SETTINGS);
        settingRepository.findAll().forEach(setting -> result.put(setting.getSettingKey(), setting.getSettingValue()));
        return response(result);
    }

    @PutMapping("/settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, String> values) {
        values.forEach((key, value) -> {
            if (!DEFAULT_SETTINGS.containsKey(key)) return;
            SystemSetting setting = settingRepository.findBySettingKey(key).orElseGet(SystemSetting::new);
            setting.setSettingKey(key);
            setting.setSettingValue(value == null ? "" : value.trim());
            settingRepository.save(setting);
        });
        log("SETTINGS_UPDATED", "Đã cập nhật " + values.keySet());
        return settings();
    }

    @GetMapping("/backups")
    public Map<String, Object> backups() { return response(backupService.listBackups()); }

    @PostMapping("/backups")
    public Map<String, Object> createBackup() {
        Map<String, Object> backup = backupService.createBackup();
        log("BACKUP_CREATED", String.valueOf(backup.get("fileName")));
        return response(backup);
    }

    @PostMapping("/backups/{fileName}/restore")
    public Map<String, Object> restore(@PathVariable String fileName) {
        backupService.createBackup();
        return response(backupService.restore(fileName));
    }

    private User actor() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy Admin"));
    }

    private void log(String action, String detail) {
        activityRepository.save(SystemActivity.builder().actorEmail(actor().getEmail()).action(action).detail(detail).build());
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId()); view.put("fullName", user.getFullName()); view.put("email", user.getEmail());
        view.put("studentId", user.getStudentId()); view.put("universityName", user.getUniversityName());
        view.put("avatarUrl", user.getAvatarUrl()); view.put("role", user.getRole());
        view.put("status", user.getStatus()); view.put("createdAt", user.getCreatedAt());
        return view;
    }

    private Map<String, Object> response(Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", result);
        return response;
    }
}
