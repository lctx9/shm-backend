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
public class InterRaterStatsResponse {
    private double averageStandardDeviation;
    private int multiGradedSubmissionsCount;
    private double exactAgreementRate; // Tỷ lệ đồng thuận cao (độ lệch chuẩn <= 5.0)
    private List<JudgeBiasDto> judgeBiases;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgeBiasDto {
        private String judgeName;
        private String judgeEmail;
        private int submissionsGraded;
        private double averageBias; // Độ lệch trung bình so với điểm trung bình của các giám khảo khác
    }
}
