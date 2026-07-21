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
    private String teamName;
    private Long eventId;
    private String eventName;
    private String trackName;
    private Long userId;
    private String fullName;
    private String email;
    private String studentId;
    private String status;
    private String type;
    private String inviterName;
    private java.time.LocalDateTime createdAt;
}
