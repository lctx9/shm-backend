package com.backend.controller;

import com.backend.dto.request.StaffCreateRequest;
import com.backend.dto.response.UserProfileResponse;
import com.backend.entity.User;
import com.backend.entity.enums.AccountStatus;
import com.backend.entity.enums.RoleType;
import com.backend.repository.UserRepository;
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

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();
        response.put("result", toProfile(getAuthenticatedUser()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(@PathVariable RoleType role) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", userRepository.findByRole(role).stream().map(this::toProfile).toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> createStaff(@RequestBody StaffCreateRequest request) {
        if (request.getRole() != RoleType.MENTOR && request.getRole() != RoleType.JUDGE && request.getRole() != RoleType.COORDINATOR) {
            throw new RuntimeException("Chỉ được tạo tài khoản MENTOR, JUDGE hoặc COORDINATOR tại đây");
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

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("result", users);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        user.setStatus(AccountStatus.valueOf(body.get("status")));
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
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentId(user.getStudentId())
                .fptStudent(user.isFptStudent())
                .universityName(user.getUniversityName())
                .avatarUrl(user.getAvatarUrl())
                .studentCardUrl(user.getStudentCardUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
