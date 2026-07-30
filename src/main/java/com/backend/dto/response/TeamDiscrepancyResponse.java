package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDiscrepancyResponse {
    private Long submissionId;
    private Long teamId;
    private String teamName;
    private String eventName;
    private String roundName;
    private String trackName;
    private List<JudgeScoreDetailDto> judgeScores;
    private double averageScore;
    private double maxDiscrepancy; // Max score - Min score
    private double standardDeviation;
    private boolean isHighDiscrepancy; // true nếu maxDiscrepancy > 15.0 điểm

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgeScoreDetailDto {
        private Long judgeId;
        private String judgeName;
        private String judgeEmail;
        private double score;
        private String comment;
    }
}
