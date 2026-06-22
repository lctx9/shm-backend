package com.backend.controller;

import com.backend.dto.LoginRequest;
import com.backend.dto.AuthResponse;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getUsername());

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getPassword().equals(loginRequest.getPassword())) {


                if ("LOCKED".equals(user.getStatus())) {
                    return ResponseEntity.status(403).body("Tài khoản của bạn đã bị khóa!");
                }

                String dummyToken = "eyJhbGciOiJIUzI1NiJ9.eyBzdWIiOiAi" + user.getId() + "\"";
                return ResponseEntity.ok(new AuthResponse(dummyToken, "Đăng nhập thành công! Chào mừng " + user.getFullName()));
            }
        }

        return ResponseEntity.status(401).body("Email hoặc mật khẩu không chính xác!");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Đăng xuất thành công!");
    }
}