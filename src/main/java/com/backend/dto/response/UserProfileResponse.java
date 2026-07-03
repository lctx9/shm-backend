package com.backend.dto.response;

import com.backend.entity.enums.AccountStatus;
import com.backend.entity.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String studentId;
    private boolean fptStudent;
    private String universityName;
    private String avatarUrl;
    private String studentCardUrl;
    private String rejectionReason;
    private RoleType role;
    private AccountStatus status;
}
