package com.backend.service;

import com.backend.dto.CreateExpertRequest;  // ← IMPORT MỚI
import com.backend.dto.UserResponse;
import com.backend.entity.AuditLog;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.entity.enums.AuditActionType;
import com.backend.entity.enums.RoleName;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.AuditLogRepository;
import com.backend.repository.RoleRepository;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.security.SecureRandom;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    // ✅ GIỮ LẠI: Method cũ
    public List<UserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // ✅ METHOD MỚI: Tạo Expert (Judge/Mentor, có thể là Guest)
    @Transactional
    public Map<String, Object> createExpert(CreateExpertRequest request, User createdBy) {
        // 1. Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Họ tên không được để trống!");
        }
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new RuntimeException("Role không được để trống!");
        }

        // 2. Check trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // 3. Validate role
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ! Chỉ chấp nhận JUDGE hoặc MENTOR.");
        }

        if (roleName != RoleName.JUDGE && roleName != RoleName.MENTOR) {
            throw new RuntimeException("Chỉ có thể tạo tài khoản cho JUDGE hoặc MENTOR!");
        }

        // 4. Nếu là Mentor, không cho phép isGuest
        boolean isGuest = request.getIsGuest() != null && request.getIsGuest();
        if (roleName == RoleName.MENTOR && isGuest) {
            throw new RuntimeException("Mentor không thể là Guest! Chỉ Judge mới có thể là Guest.");
        }

        // 5. Sinh password ngẫu nhiên
        String randomPassword = generateRandomPassword();

        // 6. Tạo User
        User expert = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(randomPassword))
                .fullName(request.getFullName())
                .status(UserStatus.ACTIVE)
                .isGuestJudge(isGuest)
                .build();

        // 7. Gán role
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role " + roleName + "!"));
        expert.getRoles().add(role);

        // 8. Save user
        User savedUser = userRepository.save(expert);

        // 9. Gửi email
        sendExpertEmail(request.getEmail(), request.getFullName(), randomPassword, roleName, isGuest);

        // 10. Ghi Audit Log
        AuditLog auditLog = AuditLog.builder()
                .performedBy(createdBy)
                .actionType(AuditActionType.EXPERT_CREATED)
                .targetEntity("User")
                .targetId(savedUser.getId())
                .reason("Tạo tài khoản " + roleName + (isGuest ? " (Guest)" : ""))
                .newValue("{\"email\": \"" + request.getEmail() + "\", \"role\": \"" + roleName + "\", \"isGuest\": " + isGuest + "}")
                .build();
        auditLogRepository.save(auditLog);

        // 11. Trả về response
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("fullName", savedUser.getFullName());
        response.put("role", roleName);
        response.put("isGuest", isGuest);
        response.put("status", savedUser.getStatus());
        response.put("message", "Đã tạo tài khoản và gửi email thông tin đăng nhập!");

        return response;
    }

    // ✅ Helper: Sinh password ngẫu nhiên mạnh
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    // ✅ Helper: Gửi email cho Expert
    private void sendExpertEmail(String email, String fullName, String password, RoleName role, boolean isGuest) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("sealfpt@gmail.com");
            message.setTo(email);
            message.setSubject("[SEAL Hackathon] Tài khoản " + role + (isGuest ? " khách mời" : ""));
            message.setText(
                    "Kính gửi " + fullName + ",\n\n" +
                            "Ban tổ chức SEAL Hackathon đã tạo tài khoản " + role + (isGuest ? " khách mời" : "") + " cho bạn.\n\n" +
                            "Thông tin đăng nhập:\n" +
                            "- Email: " + email + "\n" +
                            "- Password: " + password + "\n" +
                            "- Link đăng nhập: http://localhost:5173/login\n\n" +
                            "Vui lòng đổi password sau khi đăng nhập lần đầu.\n\n" +
                            "Trân trọng,\n" +
                            "Ban tổ chức SEAL Hackathon"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }
    }

    // ✅ GIỮ LẠI: Helper map DTO
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setStatus(user.getStatus());
        response.setStudentId(user.getStudentId());
        response.setUniversityName(user.getUniversityName());
        response.setRoles(user.getRoles());
        return response;
    }
}