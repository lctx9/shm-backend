package com.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class TeamDetailsResponse {
    private UUID id;
    private String name;
    private UUID trackId;
    private String status; // APPROVED, ELIMINATED, DISQUALIFIED
    private String visibility; // PUBLIC, PRIVATE
    private List<TeamMemberResponse> members;
}