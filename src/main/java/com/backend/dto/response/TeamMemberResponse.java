package com.backend.dto.response;

import com.backend.entity.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String studentId;
    private String universityName;
    private MemberRole role;
}
