package com.backend.controller;

import com.backend.dto.AuthResponse;
import com.backend.dto.LoginRequest;
import com.backend.repository.VerificationCodeRepository;
import com.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JavaMailSender mailSender; // ĐÃ THÊM @Autowired

    @Autowired
    private VerificationCodeRepository codeRepository; // ĐÃ THÊM @Autowired

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String message = authService.logout(token);
        return ResponseEntity.ok(message);
    }
}