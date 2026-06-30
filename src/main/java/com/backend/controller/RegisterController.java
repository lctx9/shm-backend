package com.backend.controller;

import com.backend.dto.RegisterRequest;
import com.backend.entity.User;
import com.backend.entity.Role;
import com.backend.entity.VerificationCode;
import com.backend.entity.enums.RoleName;
import com.backend.entity.enums.StudentType;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import com.backend.repository.RoleRepository;
import com.backend.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/auth") // Giữ nguyên prefix cũ
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
    private PasswordEncoder passwordEncoder; // Thêm để mã hóa mật khẩu

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(otp);
        verificationCode.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        codeRepository.save(verificationCode);

        // Giữ nguyên logic gửi mail ban đầu của bạn
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[SEAL SYSTEM] - MÃ XÁC THỰC");
            message.setText("Mã OTP của bạn là: " + otp);
            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã OTP thành công về mail!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi email: " + e.getMessage());
        }
    }

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<?> register(
            @ModelAttribute RegisterRequest request,
            @RequestParam("studentCard") MultipartFile studentCardFile
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại!");
        }

        // Giữ nguyên cách khởi tạo đối tượng new User() ban đầu của bạn
        User user = new User();
        user.setEmail(request.getEmail());

        // CHỈ THAY ĐỔI DÒNG NÀY: Mã hóa mật khẩu trần trước khi lưu
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());

        // Thay đổi String thành Enum ở bên trong logic gán giá trị
        if ("true".equals(request.getIsFptStudent())) {
            user.setStudentType(StudentType.FPT);
            user.setUniversityName("FPT University");
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStudentType(StudentType.EXTERNAL);
            user.setUniversityName(request.getUniversityName());
            user.setStatus(UserStatus.PENDING);
        }

        // Giữ nguyên logic Log file ảnh thẻ của bạn
        try {
            System.out.println(">>> Đã nhận file ảnh thẻ thành công: " + studentCardFile.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý file hình ảnh: " + e.getMessage());
        }

        // Sửa từ tìm ID cứng sang tìm theo Enum Name để tránh lỗi DB
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy định nghĩa quyền STUDENT trong DB!"));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("Tạo tài khoản thành công!");
    }
}