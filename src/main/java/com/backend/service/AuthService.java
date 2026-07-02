package com.backend.service;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;

    // Thay thế @Autowired field bằng Constructor Injection sạch sẽ
    // Gỡ bỏ hoàn toàn PasswordEncoder khỏi đây để tránh lỗi thiếu Bean
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // 2. ĐÃ THAY ĐỔI: So sánh trực tiếp chuỗi mật khẩu trần (Plain Text) để test local
        if (!request.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản của bạn đang ở trạng thái: " + user.getStatus());
        }

        // 4. Tạo token (Tạm thời)
        String mockToken = "SEAL-MOCK-JWT-" + user.getId();

        // 5. Xử lý roles
        String rolesString = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(","));

        return new AuthResponse(mockToken, user.getEmail(), rolesString, "Đăng nhập thành công!");
    }

    public String logout(String token) {
        return "Đăng xuất thành công!";
    }
}