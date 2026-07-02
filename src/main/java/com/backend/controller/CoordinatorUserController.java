package com.backend.controller;

import com.backend.dto.ApproveUserRequest;
import com.backend.dto.UserResponse;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinator/users") // Định nghĩa riêng vùng bảo mật cho Coordinator
@CrossOrigin(origins = "*")
public class CoordinatorUserController {

    @Autowired
    private UserService userService;

    /**
     * API Lấy danh sách tài khoản chờ phê duyệt (PENDING)
     * URL chuẩn: GET http://localhost:8080/api/coordinator/users/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<UserResponse>> getPendingUsers() {
        try {
            List<UserResponse> responses = userService.getPendingUsers();
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API Phê duyệt hoặc Từ chối tài khoản user mới đăng ký
     * URL chuẩn: PUT http://localhost:8080/api/coordinator/users/approve
     */
    @PutMapping("/approve")
    public ResponseEntity<?> approveUser(@RequestBody ApproveUserRequest request) {
        try {
            userService.approveUser(request);
            return ResponseEntity.ok("Xử lý trạng thái tài khoản thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}