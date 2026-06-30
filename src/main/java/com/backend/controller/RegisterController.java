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
import org.springframework.web.multipart.MultipartFile; // <-- Nhớ import thư viện này

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/auth") // Giữ đúng Prefix hệ thống cũ của bạn
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

    // API 2: XỬ LÝ ĐĂNG KÝ TÀI KHOẢN CHÍNH THỨC (Đã nâng cấpMultipart)
    @PostMapping(value = "/register", consumes = {"multipart/form-data"}) // <-- Bắt buộc chỉ định loại dữ liệu nhận
    public ResponseEntity<?> register(
            @ModelAttribute RegisterRequest request, // <-- Đổi từ @RequestBody sang @ModelAttribute
            @RequestParam("studentCard") MultipartFile studentCardFile // <-- Thêm trường hứng file ảnh từ FE gửi lên
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký trên hệ thống!");
        }

        // Kiểm tra logic OTP
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

        // KIỂM TRA FILE ẢNH THẺ SINH VIÊN
        if (studentCardFile == null || studentCardFile.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng tải lên hình ảnh thẻ sinh viên để BTC xác thực!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // NOTE: Nên bọc BCryptPasswordEncoder ở đây sau này
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());

        // Phân tách trạng thái duyệt theo loại sinh viên
        if ("true".equals(request.getIsFptStudent())) {
            user.setStudentType("FPT");
            user.setUniversityName("FPT University");
            user.setStatus("ACTIVE");
        } else {
            user.setStudentType("EXTERNAL");
            user.setUniversityName(request.getUniversityName());
            user.setStatus("PENDING"); // Chờ Coordinator duyệt ảnh thẻ
        }

        // Xử lý lưu File Ảnh Thẻ Sinh Viên
        try {
            // NOTE: Tạm thời bạn có thể lấy tên file lưu vào một cột tên là `studentCardImage` trong bảng User để test
            // String fileName = studentCardFile.getOriginalFilename();
            // user.setStudentCardImage(fileName);

            System.out.println(">>> Đã nhận file ảnh thẻ thành công: " + studentCardFile.getOriginalFilename());
            System.out.println(">>> Dung lượng file: " + studentCardFile.getSize() + " bytes");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý file hình ảnh: " + e.getMessage());
        }

        Role studentRole = roleRepository.findById(5)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy định nghĩa quyền STUDENT (ID=5) trong DB!"));
        user.getRoles().add(studentRole);

        userRepository.save(user);
        return ResponseEntity.ok("Tạo tài khoản thành công! Trạng thái: " + user.getStatus());
    }
}