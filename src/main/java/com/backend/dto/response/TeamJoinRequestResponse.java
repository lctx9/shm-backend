package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamJoinRequestResponse {
    private Long id;
    private Long teamId;
    private Long userId;
    private String fullName;
    private String email;
    private String studentId;
    private String status;
}
