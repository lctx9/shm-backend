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
    private TeamType type;
    private Long eventId;
    private String eventName;
    private Long trackId;
    private String trackName;
    private List<TeamMemberResponse> members;
    private int memberCount;
}
