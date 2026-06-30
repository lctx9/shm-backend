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
    private PasswordEncoder passwordEncoder; // Tiêm bộ mã hóa mật khẩu

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        VerificationCode code = new VerificationCode();
        code.setEmail(email);
        code.setCode(otp);
        code.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        codeRepository.save(code);

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

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<?> register(
            @ModelAttribute RegisterRequest request,
            @RequestParam("studentCard") MultipartFile studentCardFile
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký trên hệ thống!");
        }

        // Kiểm tra logic OTP công tâm từ Database
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

        // Kiểm tra file ảnh thẻ sinh viên gửi từ FE lên
        if (studentCardFile == null || studentCardFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng tải lên hình ảnh thẻ sinh viên để BTC xác thực!");
        }

        // Tạo Entity User và băm mật khẩu bằng PasswordEncoder
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa chuẩn BCrypt chống lỗi đăng nhập
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());

        // Xử lý Enum chuẩn xác
        if ("true".equals(request.getIsFptStudent())) {
            user.setStudentType(StudentType.FPT);
            user.setUniversityName("FPT University");
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStudentType(StudentType.EXTERNAL);
            user.setUniversityName(request.getUniversityName());
            user.setStatus(UserStatus.PENDING);
        }

        // Xử lý file ảnh (Log ra console kiểm tra tạm thời)
        try {
            System.out.println(">>> Đã nhận file ảnh thẻ thành công: " + studentCardFile.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý file hình ảnh: " + e.getMessage());
        }

        // Tìm Role bằng Name truyền vào Enum chuẩn xác
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("Quyền STUDENT chưa được thiết lập trong DB!"));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("Tạo tài khoản thành công! Trạng thái hiện tại: " + user.getStatus());
    }
}