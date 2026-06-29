package com.backend.dto;

import com.backend.entity.Role;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String status;
    private String studentId;
    private String universityName;
    private String avatarUrl;
    private Set<Role> roles;
}