package com.backend.controller;

import com.backend.entity.User;
import com.backend.entity.enums.AccountStatus;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Đảm bảo không bị chặn CORS khi gọi PUT/GET
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 1. API lấy danh sách người dùng cho giao diện Quản lý
    @GetMapping
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("result", users); // Trả về bọc trong object "result" để khớp với Frontend
        return ResponseEntity.ok(response);
    }

    // 2. API Cập nhật trạng thái duyệt (Duyệt/Từ chối)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('COORDINATOR', 'ROLE_COORDINATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        // Tìm user, nếu không có thì ném lỗi
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        // Cập nhật trạng thái thành APPROVED hoặc REJECTED
        user.setStatus(AccountStatus.valueOf(body.get("status")));
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("result", "Cập nhật trạng thái thành công!");
        return ResponseEntity.ok(response);
    }
}