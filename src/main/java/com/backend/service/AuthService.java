package com.backend.service;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // 2. Kiểm tra mật khẩu (Sử dụng BCrypt đã hash)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        // 3. Kiểm tra trạng thái tài khoản (So sánh với Enum thay vì String)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản của bạn đang ở trạng thái: " + user.getStatus());
        }

        // 4. Tạo token (Tạm thời)
        String mockToken = "SEAL-MOCK-JWT-" + user.getId();

        // 5. Xử lý roles (Sử dụng Enum Name)
        String rolesString = user.getRoles().stream()
                .map(role -> role.getName().name()) // Lấy tên từ Enum RoleName
                .collect(Collectors.joining(","));

        return new AuthResponse(mockToken, user.getEmail(), rolesString, "Đăng nhập thành công!");
    }

    public String logout(String token) {
        // Sau này có thể thêm blacklist cho JWT ở đây
        return "Đăng xuất thành công!";
    }
}