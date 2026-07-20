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
public class CohenKappaStatsResponse {
    private double overallKappa;         // Hệ số Cohen's Kappa tổng thể
    private double observedAgreement;    // Tỷ lệ đồng thuận thực tế Po (%)
    private double expectedAgreement;    // Tỷ lệ đồng thuận kỳ vọng ngẫu nhiên Pe (%)
    private String agreementLevel;       // Đánh giá mức độ (vd: "Đồng thuận cao", "Đồng thuận vừa phải")
    private int evaluatedPairsCount;     // Số cặp đánh giá co-grading
    private List<JudgePairKappaDto> judgePairKappas; // Chi tiết Cohen's Kappa giữa từng cặp giám khảo

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JudgePairKappaDto {
        private String judge1Name;
        private String judge1Email;
        private String judge2Name;
        private String judge2Email;
        private int sharedSubmissionsCount;
        private double pairKappa;
        private double observedAgreement;
        private double expectedAgreement;
        private String agreementLevel;
    }
}
