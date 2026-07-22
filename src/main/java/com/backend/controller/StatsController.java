package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.DashboardStatsResponse;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatsController {

    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final com.backend.repository.ScoreRepository scoreRepository;

    @GetMapping
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        // Gọi thẳng các hàm đếm (count) từ Cơ sở dữ liệu
        long activeEventsCount = eventRepository.countByIsActiveTrue();
        long totalTeamsCount = teamRepository.countEligibleTeams();
        long pendingSubmissionsCount = submissionRepository.countByIsGradedFalse();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .activeEvents(activeEventsCount)
                .totalTeams(totalTeamsCount)
                .pendingSubmissions(pendingSubmissionsCount)
                .build();

        return ApiResponse.<DashboardStatsResponse>builder()
                .result(stats)
                .build();
    }

    @GetMapping("/inter-rater")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<com.backend.dto.response.InterRaterStatsResponse> getInterRaterStats() {
        List<com.backend.entity.Score> allScores = scoreRepository.findAll();
        
        // Group scores by submission
        java.util.Map<Long, List<com.backend.entity.Score>> scoresBySubmission = allScores.stream()
                .filter(s -> s.getScoreValue() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getSubmission().getId()));
                
        double totalStdDevSum = 0.0;
        int multiGradedCount = 0;
        int agreementCount = 0;
        
        for (List<com.backend.entity.Score> scores : scoresBySubmission.values()) {
            int k = scores.size();
            if (k >= 2) {
                double sum = 0.0;
                for (com.backend.entity.Score s : scores) {
                    sum += s.getScoreValue();
                }
                double mean = sum / k;
                double sumSqDiff = 0.0;
                for (com.backend.entity.Score s : scores) {
                    sumSqDiff += Math.pow(s.getScoreValue() - mean, 2);
                }
                double variance = sumSqDiff / (k - 1);
                double stdDev = Math.sqrt(variance);
                
                totalStdDevSum += stdDev;
                multiGradedCount++;
                if (stdDev <= 5.0) {
                    agreementCount++;
                }
            }
        }
        
        double avgStdDev = multiGradedCount > 0 ? totalStdDevSum / multiGradedCount : 0.0;
        double exactRate = multiGradedCount > 0 ? (double) agreementCount / multiGradedCount * 100.0 : 0.0;
        
        // Calculate Judge Bias
        java.util.Map<Long, List<com.backend.entity.Score>> scoresByJudge = allScores.stream()
                .filter(s -> s.getScoreValue() != null && s.getJudge() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getJudge().getId()));
                
        List<com.backend.dto.response.InterRaterStatsResponse.JudgeBiasDto> judgeBiases = new java.util.ArrayList<>();
        
        for (java.util.Map.Entry<Long, List<com.backend.entity.Score>> entry : scoresByJudge.entrySet()) {
            List<com.backend.entity.Score> judgeScores = entry.getValue();
            if (judgeScores.isEmpty()) continue;
            
            com.backend.entity.User judge = judgeScores.get(0).getJudge();
            double biasSum = 0.0;
            int biasCount = 0;
            
            for (com.backend.entity.Score s : judgeScores) {
                Long subId = s.getSubmission().getId();
                List<com.backend.entity.Score> allSubScores = scoresBySubmission.get(subId);
                if (allSubScores != null && allSubScores.size() >= 2) {
                    double otherSum = 0.0;
                    int otherCount = 0;
                    for (com.backend.entity.Score other : allSubScores) {
                        if (!other.getJudge().getId().equals(judge.getId())) {
                            otherSum += other.getScoreValue();
                            otherCount++;
                        }
                    }
                    if (otherCount > 0) {
                        double otherMean = otherSum / otherCount;
                        double bias = s.getScoreValue() - otherMean;
                        biasSum += bias;
                        biasCount++;
                    }
                }
            }
            
            double avgBias = biasCount > 0 ? biasSum / biasCount : 0.0;
            
            // Round values
            avgBias = Math.round(avgBias * 100.0) / 100.0;
            
            judgeBiases.add(com.backend.dto.response.InterRaterStatsResponse.JudgeBiasDto.builder()
                    .judgeName(judge.getFullName())
                    .judgeEmail(judge.getEmail())
                    .submissionsGraded(judgeScores.size())
                    .averageBias(avgBias)
                    .build());
        }
        
        avgStdDev = Math.round(avgStdDev * 100.0) / 100.0;
        exactRate = Math.round(exactRate * 10.0) / 10.0;
        
        com.backend.dto.response.InterRaterStatsResponse response = com.backend.dto.response.InterRaterStatsResponse.builder()
                .averageStandardDeviation(avgStdDev)
                .multiGradedSubmissionsCount(multiGradedCount)
                .exactAgreementRate(exactRate)
                .judgeBiases(judgeBiases)
                .build();
                
        return ApiResponse.<com.backend.dto.response.InterRaterStatsResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/cohen-kappa")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<com.backend.dto.response.CohenKappaStatsResponse> getCohenKappaStats() {
        List<com.backend.entity.Score> allScores = scoreRepository.findAll();

        // Group valid scores by submission
        java.util.Map<Long, List<com.backend.entity.Score>> scoresBySubmission = allScores.stream()
                .filter(s -> s.getScoreValue() != null && s.getJudge() != null && s.getSubmission() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getSubmission().getId()));

        // Map to store co-graded pairs: key = "judge1Id_judge2Id", value = list of paired scores
        java.util.Map<String, List<PairScore>> judgePairScores = new java.util.HashMap();
        java.util.Map<Long, com.backend.entity.User> judgeMap = new java.util.HashMap();

        int totalEvaluatedPairs = 0;
        int totalObservedAgreements = 0;
        int[] overallMarginalJ1 = new int[4];
        int[] overallMarginalJ2 = new int[4];

        for (List<com.backend.entity.Score> scores : scoresBySubmission.values()) {
            if (scores.size() < 2) continue;

            for (int i = 0; i < scores.size(); i++) {
                com.backend.entity.Score s1 = scores.get(i);
                judgeMap.put(s1.getJudge().getId(), s1.getJudge());
                int tier1 = scoreToTier(s1.getScoreValue());

                for (int j = i + 1; j < scores.size(); j++) {
                    com.backend.entity.Score s2 = scores.get(j);
                    judgeMap.put(s2.getJudge().getId(), s2.getJudge());
                    int tier2 = scoreToTier(s2.getScoreValue());

                    Long idA = s1.getJudge().getId();
                    Long idB = s2.getJudge().getId();

                    // Canonical pair key (smaller ID first)
                    String pairKey = idA < idB ? idA + "_" + idB : idB + "_" + idA;

                    judgePairScores.computeIfAbsent(pairKey, k -> new java.util.ArrayList<>())
                            .add(new PairScore(idA < idB ? tier1 : tier2, idA < idB ? tier2 : tier1));

                    totalEvaluatedPairs++;
                    if (tier1 == tier2) {
                        totalObservedAgreements++;
                    }
                    overallMarginalJ1[tier1]++;
                    overallMarginalJ2[tier2]++;
                }
            }
        }

        // Calculate Overall Cohen's Kappa
        double overallPo = totalEvaluatedPairs > 0 ? (double) totalObservedAgreements / totalEvaluatedPairs : 0.0;
        double overallPe = 0.0;
        if (totalEvaluatedPairs > 0) {
            for (int t = 0; t < 4; t++) {
                double p1 = (double) overallMarginalJ1[t] / totalEvaluatedPairs;
                double p2 = (double) overallMarginalJ2[t] / totalEvaluatedPairs;
                overallPe += p1 * p2;
            }
        }

        double overallKappa = calculateKappaValue(overallPo, overallPe);

        // Calculate Pair-wise Cohen's Kappa
        List<com.backend.dto.response.CohenKappaStatsResponse.JudgePairKappaDto> pairKappas = new java.util.ArrayList<>();

        for (java.util.Map.Entry<String, List<PairScore>> entry : judgePairScores.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long j1Id = Long.parseLong(parts[0]);
            Long j2Id = Long.parseLong(parts[1]);
            List<PairScore> pairs = entry.getValue();

            com.backend.entity.User j1 = judgeMap.get(j1Id);
            com.backend.entity.User j2 = judgeMap.get(j2Id);

            int n = pairs.size();
            int observedAgree = 0;
            int[] m1 = new int[4];
            int[] m2 = new int[4];

            for (PairScore ps : pairs) {
                if (ps.tier1 == ps.tier2) observedAgree++;
                m1[ps.tier1]++;
                m2[ps.tier2]++;
            }

            double po = (double) observedAgree / n;
            double pe = 0.0;
            for (int t = 0; t < 4; t++) {
                pe += ((double) m1[t] / n) * ((double) m2[t] / n);
            }

            double kappa = calculateKappaValue(po, pe);

            pairKappas.add(com.backend.dto.response.CohenKappaStatsResponse.JudgePairKappaDto.builder()
                    .judge1Name(j1 != null ? j1.getFullName() : "Giám khảo #" + j1Id)
                    .judge1Email(j1 != null ? j1.getEmail() : "")
                    .judge2Name(j2 != null ? j2.getFullName() : "Giám khảo #" + j2Id)
                    .judge2Email(j2 != null ? j2.getEmail() : "")
                    .sharedSubmissionsCount(n)
                    .pairKappa(Math.round(kappa * 100.0) / 100.0)
                    .observedAgreement(Math.round(po * 100.0 * 10.0) / 10.0)
                    .expectedAgreement(Math.round(pe * 100.0 * 10.0) / 10.0)
                    .agreementLevel(interpretKappa(kappa))
                    .build());
        }

        com.backend.dto.response.CohenKappaStatsResponse response = com.backend.dto.response.CohenKappaStatsResponse.builder()
                .overallKappa(Math.round(overallKappa * 100.0) / 100.0)
                .observedAgreement(Math.round(overallPo * 100.0 * 10.0) / 10.0)
                .expectedAgreement(Math.round(overallPe * 100.0 * 10.0) / 10.0)
                .agreementLevel(interpretKappa(overallKappa))
                .evaluatedPairsCount(totalEvaluatedPairs)
                .judgePairKappas(pairKappas)
                .build();

        return ApiResponse.<com.backend.dto.response.CohenKappaStatsResponse>builder()
                .result(response)
                .build();
    }

    private int scoreToTier(double score) {
        if (score < 50.0) return 0; // Tier 1
        if (score < 70.0) return 1; // Tier 2
        if (score < 85.0) return 2; // Tier 3
        return 3;                   // Tier 4
    }

    private double calculateKappaValue(double po, double pe) {
        if (Math.abs(1.0 - pe) < 0.0001) return 1.0;
        return (po - pe) / (1.0 - pe);
    }

    private String interpretKappa(double kappa) {
        if (kappa < 0) return "Không đồng thuận (Kém hơn ngẫu nhiên)";
        if (kappa <= 0.20) return "Đồng thuận rất thấp (Slight)";
        if (kappa <= 0.40) return "Đồng thuận trung bình nhẹ (Fair)";
        if (kappa <= 0.60) return "Đồng thuận vừa phải (Moderate)";
        if (kappa <= 0.80) return "Đồng thuận cao (Substantial)";
        return "Rất hoàn hảo (Almost Perfect)";
    }

    private record PairScore(int tier1, int tier2) {}
}