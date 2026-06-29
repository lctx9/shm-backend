package com.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String studentId;
    private String universityName;
    private String otpCode;       // Nhận mã 6 số từ ô OTP màu cam
    private String isFptStudent;  // Nhận chuỗi "true" hoặc "false" từ Select box
}