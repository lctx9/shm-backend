package com.backend.controller;

import com.backend.dto.UpdateProfileRequest;
import com.backend.dto.UserResponse;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/profile") // Endpoint PRIVATE: Không có chữ /public/
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserService userService;

    // 1. API: Xem thông tin cá nhân (GET /api/profile)
    @GetMapping
    public ResponseEntity<?> getMyProfile(
            Principal principal,
            @RequestParam(required = false) String emailFallback) {
        try {
            // Lấy email từ Token (nếu đã cấu hình JwtFilter) HOẶC lấy tạm từ RequestParam để test
            String userEmail = (principal != null) ? principal.getName() : emailFallback;

            if (userEmail == null) {
                return ResponseEntity.status(401).body("Lỗi: Không xác định được danh tính (Chưa đăng nhập)!");
            }

            UserResponse profile = userService.getUserProfile(userEmail);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. API: Cập nhật thông tin cá nhân (PUT /api/profile)
    @PutMapping
    public ResponseEntity<?> updateMyProfile(
            Principal principal,
            @RequestParam(required = false) String emailFallback,
            @RequestBody UpdateProfileRequest request) {
        try {
            String userEmail = (principal != null) ? principal.getName() : emailFallback;

            if (userEmail == null) {
                return ResponseEntity.status(401).body("Lỗi: Không xác định được danh tính (Chưa đăng nhập)!");
            }

            UserResponse updatedProfile = userService.updateUserProfile(userEmail, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}