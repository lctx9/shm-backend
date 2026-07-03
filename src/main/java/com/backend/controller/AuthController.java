package com.backend.controller;

import com.backend.dto.request.LoginRequest;
import com.backend.dto.request.OtpRequest;
import com.backend.dto.request.RegisterRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.AuthResponse;
import com.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ApiResponse<String> sendOtp(@RequestBody @Valid OtpRequest request) {
        return ApiResponse.<String>builder()
                .result(authService.sendRegistrationOtp(request.getEmail()))
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<String>builder()
                .result(authService.register(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .build();
    }
}
