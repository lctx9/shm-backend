package com.backend.service;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.entity.enums.UserStatus; // Import Enum
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Quan trọng
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Inject Encoder để check pass

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // 1. Kiểm tra mật khẩu (Sử dụng matches thay vì equals)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        // 2. Kiểm tra trạng thái bằng Enum (UserStatus)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt hoặc đang chờ duyệt!");
        }

        // 3. Xử lý logic Token & Role (Giữ nguyên logic của bạn)
        String mockToken = "SEAL-MOCK-JWT-" + user.getEmail() + "-" + System.currentTimeMillis();

        String rolesString = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(","));

        if (rolesString.isEmpty()) {
            rolesString = "USER";
        }

        return new AuthResponse(mockToken, user.getEmail(), rolesString, "Đăng nhập thành công!");
    }

    public String logout(String token) {
        return "Đăng xuất thành công!";
    }
}