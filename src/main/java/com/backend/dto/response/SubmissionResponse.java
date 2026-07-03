package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long matrixId;
    private String trackName;
    private String roundName;
    private String fileUrl;
    private Boolean flagged;
    private String flagReason;
    private Double score;
    private String feedback;
    private String criteriaScoresJson;
    private Boolean graded;
}
