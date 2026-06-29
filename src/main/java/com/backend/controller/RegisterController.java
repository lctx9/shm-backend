package com.backend.controller;

import com.backend.dto.RegisterRequest;
import com.backend.entity.User;
import com.backend.entity.Role;
import com.backend.entity.VerificationCode;
import com.backend.entity.enums.StudentType;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import com.backend.repository.RoleRepository;
import com.backend.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder; // Đảm bảo đã import
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/auth")
@CrossOrigin(origins = "*")
public class RegisterController {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private VerificationCodeRepository codeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder; // Thêm biến này vào đây

    // --- Các phương thức gửi OTP ở đây ---

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký!");
        }

        // Logic check OTP...
        VerificationCode activeCode = codeRepository.findFirstByEmailOrderByIdDesc(request.getEmail()).orElse(null);
        if (activeCode == null || !activeCode.getCode().equals(request.getOtpCode())) {
            return ResponseEntity.badRequest().body("Mã OTP không hợp lệ!");
        }

        // Tạo User dùng Builder
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // MÃ HÓA PASSWORD
                .fullName(request.getFullName())
                .studentId(request.getStudentId())
                .build();

        // Xử lý Enum
        if ("true".equals(request.getIsFptStudent())) {
            user.setStudentType(StudentType.FPT);
            user.setUniversityName("FPT University");
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStudentType(StudentType.EXTERNAL);
            user.setUniversityName(request.getUniversityName());
            user.setStatus(UserStatus.PENDING);
        }

        // Gán Role
        Role studentRole = roleRepository.findById(5)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role!"));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("Tạo tài khoản thành công!");
    }
}