package com.backend.dto.response;

import com.backend.entity.enums.TeamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private TeamType type;
    private Long eventId;
    private String eventName;
    private java.time.LocalDateTime eventStartDate;
    private Long trackId;
    private String trackName;
    private List<TeamMemberResponse> members;
    private int memberCount;
    private boolean eligible;
    private String statusLabel;
    private String joinPassword;
    private String disqualificationStatus;
    private String disqualificationReason;
    private String disqualifierEmail;
    private String rejectionReason;
    private String skillsNeeded;
}

