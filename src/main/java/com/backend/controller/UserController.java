package com.backend.controller;

import com.backend.dto.request.StaffCreateRequest;
import com.backend.dto.response.AchievementResponse;
import com.backend.dto.response.UserProfileResponse;
import com.backend.entity.Prize;
import com.backend.entity.TeamMember;
import com.backend.entity.User;
import com.backend.entity.enums.AccountStatus;
import com.backend.entity.enums.RoleType;
import com.backend.repository.PrizeRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeamMemberRepository teamMemberRepository;
    private final PrizeRepository prizeRepository;
    private final TrackRoundMatrixRepository matrixRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();
        response.put("result", toProfile(getAuthenticatedUser()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Map<String, Object> response = new HashMap<>();
        response.put("result", toProfile(user));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(@PathVariable RoleType role) {
        Map<String, Object> response = new HashMap<>();
        List<User> users = role == RoleType.STAFF
                ? userRepository.findByRoleIn(List.of(RoleType.STAFF, RoleType.MENTOR, RoleType.JUDGE))
                : userRepository.findByRole(role);
        response.put("result", users.stream().map(this::toProfile).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/assignments")
    public ResponseEntity<Map<String, Object>> getMyAssignments() {
        User user = getAuthenticatedUser();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        List<com.backend.entity.TrackRoundMatrix> activeMentorMatrices = matrixRepository.findDistinctByMentorsId(user.getId()).stream()
                .filter(m -> m.getRound() != null && m.getRound().getEvent() != null)
                .filter(m -> {
                    com.backend.entity.HackathonEvent e = m.getRound().getEvent();
                    boolean endedEarly = Boolean.TRUE.equals(e.getEndedEarly());
                    boolean timeExpired = e.getEventEndDate() != null && now.isAfter(e.getEventEndDate());
                    return !endedEarly && !timeExpired;
                })
                .toList();

        List<com.backend.entity.TrackRoundMatrix> activeJudgeMatrices = matrixRepository.findDistinctByJudgesId(user.getId()).stream()
                .filter(m -> m.getRound() != null && m.getRound().getEvent() != null)
                .filter(m -> {
                    com.backend.entity.HackathonEvent e = m.getRound().getEvent();
                    boolean endedEarly = Boolean.TRUE.equals(e.getEndedEarly());
                    boolean timeExpired = e.getEventEndDate() != null && now.isAfter(e.getEventEndDate());
                    return !endedEarly && !timeExpired;
                })
                .toList();

        Map<String, Object> assignments = new HashMap<>();
        assignments.put("mentor", !activeMentorMatrices.isEmpty());
        assignments.put("judge", !activeJudgeMatrices.isEmpty());
        assignments.put("mentorMatrixIds", activeMentorMatrices.stream().map(m -> m.getId()).toList());
        assignments.put("judgeMatrixIds", activeJudgeMatrices.stream().map(m -> m.getId()).toList());
        Map<String, Object> response = new HashMap<>();
        response.put("result", assignments);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> createStaff(@RequestBody StaffCreateRequest request) {
        boolean allowed = request.getRole() == RoleType.STAFF
                || request.getRole() == RoleType.COORDINATOR
                || request.getRole() == RoleType.ADMIN;
        if (!allowed) {
            throw new RuntimeException("Chỉ được tạo tài khoản STAFF, COORDINATOR hoặc ADMIN tại đây");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(AccountStatus.APPROVED)
                .isFptStudent(false)
                .universityName("FPT University")
                .build();
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("result", toProfile(user));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();
        if (body.containsKey("avatarUrl")) {
            user.setAvatarUrl(body.get("avatarUrl"));
        }
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("result", toProfile(user));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("result", "Cập nhật mật khẩu thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/achievements")
    public ResponseEntity<Map<String, Object>> getMyAchievements() {
        User user = getAuthenticatedUser();
        return achievementResponseFor(user);
    }

    @GetMapping("/{id}/achievements")
    public ResponseEntity<Map<String, Object>> getUserAchievements(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return achievementResponseFor(user);
    }

    private ResponseEntity<Map<String, Object>> achievementResponseFor(User user) {
        List<AchievementResponse> achievements = teamMemberRepository.findAllByUser(user).stream()
                .flatMap(member -> prizeRepository.findByTeamId(member.getTeam().getId()).stream())
                .map(this::toAchievement)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("result", achievements);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("result", users.stream().map(this::toProfile).toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User actor = getAuthenticatedUser();
        if (id.equals(actor.getId())) {
            throw new RuntimeException("Bạn không thể tự khóa tài khoản đang đăng nhập");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        boolean coordinator = actor.getRole() == RoleType.COORDINATOR;
        if (coordinator && user.getRole() != RoleType.USER) {
            throw new RuntimeException("Coordinator chỉ được cập nhật trạng thái tài khoản sinh viên");
        }

        user.setStatus(AccountStatus.valueOf(body.get("status")));
        user.setRejectionReason(body.get("reason"));
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("result", "Cập nhật trạng thái thành công");
        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private UserProfileResponse toProfile(User user) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
        boolean isStaffOrAdmin = currentUser != null && (
                currentUser.getRole() == RoleType.ADMIN || currentUser.getRole() == RoleType.COORDINATOR
        );
        boolean isSelf = currentUser != null && currentUser.getId().equals(user.getId());
        String cardUrl = (isStaffOrAdmin || isSelf) ? user.getStudentCardUrl() : null;

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentId(user.getStudentId())
                .fptStudent(user.isFptStudent())
                .universityName(user.getUniversityName())
                .avatarUrl(user.getAvatarUrl())
                .studentCardUrl(cardUrl)
                .rejectionReason(user.getRejectionReason())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private AchievementResponse toAchievement(Prize prize) {
        return AchievementResponse.builder()
                .id(prize.getId())
                .prizeName(prize.getName())
                .eventName(prize.getEvent() == null ? null : prize.getEvent().getName())
                .eventYear(prize.getEvent() == null ? null : prize.getEvent().getYear())
                .teamName(prize.getTeam() == null ? null : prize.getTeam().getName())
                .description(prize.getDescription())
                .build();
    }
}
