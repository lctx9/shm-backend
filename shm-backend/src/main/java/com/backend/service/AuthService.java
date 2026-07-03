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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public String register(RegisterRequest request) {
        // Kiểm tra email tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // TODO: Đoạn này sau này sẽ check request.getOtp() khớp với OTP trong Redis hay không

        // Tạo User mới (Mặc định khi sinh viên tự đăng ký sẽ có role MEMBER)
        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Mã hóa mật khẩu
                .studentId(request.getStudentId())
                .isFptStudent(request.isFptStudent())
                .universityName(request.getUniversityName())
                .studentCardUrl(request.getStudentCardUrl())
                .role(RoleType.MEMBER)
                .status(AccountStatus.PENDING) // Cần Coordinator duyệt
                .build();

        userRepository.save(newUser);
        return "Đăng ký thành công! Vui lòng chờ Coordinator phê duyệt tài khoản.";
    }

    public AuthResponse login(LoginRequest request) {
        // Tìm User theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu
        boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isMatch) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        // (Tùy chọn) Kiểm tra trạng thái tài khoản
        // if (user.getStatus() != AccountStatus.APPROVED) {
        //     throw new RuntimeException("Tài khoản chưa được phê duyệt");
        // }

        // Sinh token
        String token = jwtProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}