package com.backend.controller;

import com.backend.dto.RegisterRequest;
import com.backend.entity.User;
import com.backend.entity.Role;
import com.backend.entity.VerificationCode;
import com.backend.repository.UserRepository;
import com.backend.repository.RoleRepository;
import com.backend.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/auth") // Giữ nguyên Prefix để Frontend không bị đổi URL
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

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(otp);
        verificationCode.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        codeRepository.save(verificationCode);

        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[SEAL SYSTEM] - MÃ XÁC THỰC ĐĂNG KÝ");
            message.setText("Mã OTP của bạn là: " + otp + " (Có hiệu lực trong 5 phút). Không chia sẻ mã này cho ai.");
            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã OTP thành công về mail!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi email: " + e.getMessage());
        }
    }

    // API 2: Xử lý đăng ký tài khoản chính thức
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký trên hệ thống!");
        }

        VerificationCode activeCode = codeRepository.findFirstByEmailOrderByIdDesc(request.getEmail())
                .orElse(null);

        if (activeCode == null) {
            return ResponseEntity.badRequest().body("Email này chưa từng nhận mã xác thực OTP!");
        }

        if (!activeCode.getCode().equals(request.getOtpCode())) {
            return ResponseEntity.badRequest().body("Mã OTP xác thực không chính xác!");
        }

        if (activeCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Mã OTP đã hết hạn sử dụng. Vui lòng bấm gửi lại mã mới!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());

        if ("true".equals(request.getIsFptStudent())) {
            user.setStudentType("FPT");
            user.setUniversityName("FPT University");
            user.setStatus("ACTIVE");
        } else {
            user.setStudentType("EXTERNAL");
            user.setUniversityName(request.getUniversityName());
            user.setStatus("PENDING");
        }

        Role studentRole = roleRepository.findById(5)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy định nghĩa quyền STUDENT (ID=5) trong DB!"));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("Tạo tài khoản thành công! Trạng thái: " + user.getStatus());
    }
}