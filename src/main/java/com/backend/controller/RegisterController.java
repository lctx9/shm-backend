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
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/public/auth")
@CrossOrigin(origins = "*")
public class RegisterController {

    private final JavaMailSender mailSender;
    private final VerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public RegisterController(JavaMailSender mailSender,
                              VerificationCodeRepository codeRepository,
                              UserRepository userRepository,
                              RoleRepository roleRepository) {
        this.mailSender = mailSender;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        String otp = String.format("%06d", new Random().nextInt(1000000));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(otp);
        verificationCode.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        codeRepository.save(verificationCode);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
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
            @RequestParam("studentCard") MultipartFile studentCardFile,
            @RequestParam("otp") String otpCode
    ) {
        // 1. Kiểm tra Email trùng lặp
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại!");
        }

        // 2. Xác thực OTP bằng cách lấy mã mới nhất từ Repo của bạn
        VerificationCode validCode = codeRepository.findFirstByEmailOrderByExpiryTimeDesc(request.getEmail())
                .orElse(null);

        if (validCode == null ||
                !validCode.getCode().equals(otpCode) ||
                validCode.getExpiryTime().isBefore(LocalDateTime.now())) {

            return ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn!");
        }

        // 3. Log file ảnh thẻ tạm thời
        try {
            if (studentCardFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng tải lên ảnh thẻ sinh viên!");
            }
            System.out.println(">>> Đã nhận file ảnh thẻ thành công: " + studentCardFile.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý file hình ảnh: " + e.getMessage());
        }

        // 4. Map thông tin User (Mật khẩu trần để test local)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());

        if ("true".equalsIgnoreCase(request.getIsFptStudent())) {
            user.setStudentType(StudentType.FPT);
            user.setUniversityName("FPT University");
            user.setStatus(UserStatus.ACTIVE);
        } else {
            user.setStudentType(StudentType.EXTERNAL);
            user.setUniversityName(request.getUniversityName());
            user.setStatus(UserStatus.PENDING);
        }

        // 5. Gán Role
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy định nghĩa quyền STUDENT trong DB!"));
        user.getRoles().add(studentRole);

        // 6. Lưu User mới và xóa OTP đã dùng
        userRepository.save(user);
        codeRepository.delete(validCode);

        return ResponseEntity.ok("Tạo tài khoản thành công!");
    }
}