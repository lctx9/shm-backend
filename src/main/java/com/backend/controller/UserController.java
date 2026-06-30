package com.backend.controller;

import com.backend.dto.UserResponse;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/users") // Đổi từ /api/users thành /api/public/users để khớp với SecurityConfig
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // API URL thực tế bây giờ: GET /api/public/users/search?keyword=huy
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        try {
            List<UserResponse> responses = userService.searchUsers(keyword);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}