package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class TeamMemberResponse {
    private UUID memberRecordId; // ID của bảng team_members
    private UUID userId;
    private String fullName;
    private String email;
    private String role; // LEADER hoặc MEMBER
}