package com.backend.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class TrackConfigRequest {
    private String name;
    private Set<Long> mentorIds;
    private Integer maxTeams;
}
