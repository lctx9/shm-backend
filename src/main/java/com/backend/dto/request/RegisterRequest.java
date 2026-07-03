package com.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private String studentId;
    private boolean isFptStudent;
    private String universityName;
    private String studentCardUrl;
    private String otp; // Nhận mã OTP để xác thực (Sẽ xử lý logic gửi mail sau)
}