package com.backend.controller;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.entity.VerificationCode;
import com.backend.repository.VerificationCodeRepository;
import com.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/auth")
@CrossOrigin(origins = "*") // Bật để tránh lỗi CORS khi test với Frontend

public class AuthController {

    @Autowired
    private AuthService authService;
    private org.springframework.mail.javamail.JavaMailSender mailSender;
    private VerificationCodeRepository codeRepository;

    // URL mới: POST /api/public/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // URL mới: POST /api/public/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String message = authService.logout(token);
        return ResponseEntity.ok(message);
    }



}