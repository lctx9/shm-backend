package com.backend.dto.request;

import com.backend.entity.enums.RoleType;
import lombok.Data;

@Data
public class StaffCreateRequest {
    private String fullName;
    private String email;
    private String password;
    private RoleType role;
}
