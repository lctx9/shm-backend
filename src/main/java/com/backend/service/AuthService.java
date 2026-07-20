package com.backend.service;

import com.backend.config.JwtProvider;
import com.backend.dto.request.LoginRequest;
import com.backend.dto.request.RegisterRequest;
import com.backend.dto.response.AuthResponse;
import com.backend.entity.User;
import com.backend.entity.enums.AccountStatus;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_TTL_MINUTES = 10;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:sealfpt@gmail.com}")
    private String mailFrom;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;
    private final SystemConfigurationService systemConfigurationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpRecord> registrationOtps = new ConcurrentHashMap<>();

    public String sendRegistrationOtp(String email) {
        ensureRegistrationEnabled();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        registrationOtps.put(normalizedEmail, new OtpRecord(otp, LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(normalizedEmail);
        message.setSubject("Mã OTP đăng ký SEAL Hackathon");
        message.setText("""
                Xin chào,

                Mã OTP đăng ký tài khoản SEAL Hackathon của bạn là: %s

                Mã này có hiệu lực trong %d phút. Vui lòng không chia sẻ mã này cho người khác.

                SEAL Hackathon Team
                """.formatted(otp, OTP_TTL_MINUTES));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            String errorDetail = e.getMessage();
            if (e.getCause() != null) {
                errorDetail += " (Cause: " + e.getCause().getMessage() + ")";
            }
            throw new RuntimeException("Không thể gửi email mã OTP. Lỗi SMTP: " + errorDetail);
        }

        return "Đã gửi mã OTP đến email của bạn. Vui lòng kiểm tra hộp thư.";
    }

    public String register(RegisterRequest request) {
        ensureRegistrationEnabled();
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        verifyRegistrationOtp(normalizedEmail, request.getOtp());

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .studentId(request.getStudentId())
                .isFptStudent(request.isFptStudent())
                .universityName(request.isFptStudent() ? "Đại học FPT" : request.getUniversityName())
                .studentCardUrl(request.getStudentCardUrl())
                .role(RoleType.USER)
                .status(AccountStatus.PENDING)
                .build();

        userRepository.save(newUser);
        registrationOtps.remove(normalizedEmail);

        return "Đăng ký thành công! Vui lòng chờ Coordinator phê duyệt tài khoản.";
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != AccountStatus.APPROVED) {
            throw new RuntimeException("Tài khoản chưa được duyệt hoặc đã bị khóa");
        }
        if (systemConfigurationService.maintenanceMode() && user.getRole() != RoleType.ADMIN) {
            throw new RuntimeException("Hệ thống đang bảo trì. Vui lòng quay lại sau.");
        }

        boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isMatch) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role((user.getRole() == RoleType.MENTOR || user.getRole() == RoleType.JUDGE)
                        ? RoleType.STAFF.name()
                        : user.getRole().name())
                .build();
    }

    private void verifyRegistrationOtp(String email, String otp) {
        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã OTP đã gửi qua email.");
        }

        OtpRecord record = registrationOtps.get(email);
        if (record == null) {
            throw new RuntimeException("Bạn chưa yêu cầu mã OTP hoặc mã đã hết hạn.");
        }

        if (LocalDateTime.now().isAfter(record.expiresAt())) {
            registrationOtps.remove(email);
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }

        if (!record.code().equals(otp.trim())) {
            throw new RuntimeException("Mã OTP không đúng.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void ensureRegistrationEnabled() {
        if (!systemConfigurationService.registrationEnabled()) {
            throw new RuntimeException("Hệ thống đang tạm đóng đăng ký tài khoản mới");
        }
    }

    private record OtpRecord(String code, LocalDateTime expiresAt) {
    }
}
