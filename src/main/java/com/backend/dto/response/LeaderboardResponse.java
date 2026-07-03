package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private Long id;
    private Integer rank;
    private String teamName;
    private String track;
    private String projectName;
    private String description;
    private Double score;
}