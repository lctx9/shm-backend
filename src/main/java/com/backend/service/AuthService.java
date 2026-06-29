package com.backend.service;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // Kiểm tra mật khẩu (đang so sánh chuỗi thô để bạn dễ demo)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        // Kiểm tra xem Ban tổ chức đã kích hoạt tài khoản chưa (theo nghiệp vụ SEAL)
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt hoặc đã bị khóa!");
        }

        // Tạo chuỗi token giả lập để Frontend lưu lại
        String mockToken = "SEAL-MOCK-JWT-" + user.getEmail() + "-" + System.currentTimeMillis();

        // 1. Duyệt qua Set<Role>, lấy ra trường 'name' của từng Role rồi nối lại thành chuỗi, cách nhau bằng dấu phẩy
        String rolesString = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(","));

        // 2. Nếu user không có role nào trong DB thì gán mặc định là "USER" để tránh bị rỗng
        if (rolesString.isEmpty()) {
            rolesString = "USER";
        }

        // 3. Trả về AuthResponse với chuỗi role đã được xử lý chuẩn
        return new AuthResponse(mockToken, user.getEmail(), rolesString, "Đăng nhập thành công!");
    }


    public String logout(String token) {
        // Với JWT Stateless, chủ yếu Frontend tự xóa token ở LocalStorage là xong.
        // Backend trả về message xác nhận thành công.
        return "Đăng xuất thành công!";
    }
}