package com.backend.dto;

import com.backend.entity.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class AccountApprovalRequest {
    private UUID userId;
    private UserStatus status; // Chấp nhận truyền lên: ACTIVE hoặc INACTIVE
}