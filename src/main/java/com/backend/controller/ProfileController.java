package com.backend.controller;

import com.backend.dto.UpdateProfileRequest;
import com.backend.dto.UserResponse;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/public") // Bây giờ nó nằm trong vùng public nên sẽ KHÔNG bị chặn 403 nữa
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserService userService;

    // 1. API: Xem thông tin cá nhân (GET /api/public/profile)
    @GetMapping("/profile") // Phải có dấu nháy "" ở đây
    public ResponseEntity<?> getMyProfile(
            Principal principal,
            @RequestParam(required = false) String emailFallback) {
        try {
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

    // 2. API: Cập nhật thông tin cá nhân (PUT /api/public/profile)
    @PutMapping("/profile") // Thêm "/profile" vào đây để đồng bộ với GET
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