package com.backend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String universityName;
    // Bạn có thể thêm avatarUrl hoặc các trường khác nếu Database có
}