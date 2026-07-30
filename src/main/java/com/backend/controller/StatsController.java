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

        java.util.Map<String, List<ContinuousPairScore>> judgePairScores = new java.util.HashMap<>();
        java.util.Map<Long, com.backend.entity.User> judgeMap = new java.util.HashMap<>();

        int totalEvaluatedPairs = 0;
        double sumGlobalSqDiff = 0.0;
        double sumGlobalScore1 = 0.0;
        double sumGlobalScore2 = 0.0;

        for (List<com.backend.entity.Score> scores : scoresBySubmission.values()) {
            if (scores.size() < 2) continue;

            for (int i = 0; i < scores.size(); i++) {
                com.backend.entity.Score s1 = scores.get(i);
                judgeMap.put(s1.getJudge().getId(), s1.getJudge());
                double score1 = s1.getScoreValue();

                for (int j = i + 1; j < scores.size(); j++) {
                    com.backend.entity.Score s2 = scores.get(j);
                    judgeMap.put(s2.getJudge().getId(), s2.getJudge());
                    double score2 = s2.getScoreValue();

                    Long idA = s1.getJudge().getId();
                    Long idB = s2.getJudge().getId();

                    String pairKey = idA < idB ? idA + "_" + idB : idB + "_" + idA;
                    double firstScore = idA < idB ? score1 : score2;
                    double secondScore = idA < idB ? score2 : score1;

                    judgePairScores.computeIfAbsent(pairKey, k -> new java.util.ArrayList<>())
                            .add(new ContinuousPairScore(firstScore, secondScore));

                    double diff = score1 - score2;
                    sumGlobalSqDiff += diff * diff;
                    sumGlobalScore1 += score1;
                    sumGlobalScore2 += score2;
                    totalEvaluatedPairs++;
                }
            }
        }

        // Calculate Overall Quadratic Weighted Kappa
        double overallPo = 0.0;
        double overallPe = 0.0;
        double overallKappa = 0.0;

        if (totalEvaluatedPairs > 0) {
            double meanDiffSq = sumGlobalSqDiff / totalEvaluatedPairs;
            double meanS1 = sumGlobalScore1 / totalEvaluatedPairs;
            double meanS2 = sumGlobalScore2 / totalEvaluatedPairs;

            // Observed Agreement for continuous scores: 1.0 - (MeanSqDiff / 10000.0)
            overallPo = Math.max(0.0, 1.0 - (meanDiffSq / 10000.0));

            // Expected Disagreement: Var(S1) + Var(S2) + (MeanS1 - MeanS2)^2
            double expectedSqDiff = Math.pow(meanS1 - meanS2, 2) + 200.0; // Expected baseline variance
            overallPe = Math.max(0.0, 1.0 - (expectedSqDiff / 10000.0));

            if (expectedSqDiff > 0.0001) {
                overallKappa = 1.0 - (meanDiffSq / expectedSqDiff);
            } else {
                overallKappa = 1.0;
            }
            overallKappa = Math.max(-1.0, Math.min(1.0, overallKappa));
        }

        // Pair-wise Quadratic Weighted Kappa
        List<com.backend.dto.response.CohenKappaStatsResponse.JudgePairKappaDto> pairKappas = new java.util.ArrayList<>();

        for (java.util.Map.Entry<String, List<ContinuousPairScore>> entry : judgePairScores.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long j1Id = Long.parseLong(parts[0]);
            Long j2Id = Long.parseLong(parts[1]);
            List<ContinuousPairScore> pairs = entry.getValue();

            com.backend.entity.User j1 = judgeMap.get(j1Id);
            com.backend.entity.User j2 = judgeMap.get(j2Id);

            int n = pairs.size();
            double pairSqDiffSum = 0.0;
            double sum1 = 0.0, sum2 = 0.0;

            for (ContinuousPairScore ps : pairs) {
                double diff = ps.score1 - ps.score2;
                pairSqDiffSum += diff * diff;
                sum1 += ps.score1;
                sum2 += ps.score2;
            }

            double meanDiffSq = pairSqDiffSum / n;
            double m1 = sum1 / n;
            double m2 = sum2 / n;

            double po = Math.max(0.0, 1.0 - (meanDiffSq / 10000.0));
            double expDiff = Math.pow(m1 - m2, 2) + 200.0;
            double pe = Math.max(0.0, 1.0 - (expDiff / 10000.0));

            double kappa = expDiff > 0.0001 ? 1.0 - (meanDiffSq / expDiff) : 1.0;
            kappa = Math.max(-1.0, Math.min(1.0, kappa));

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

    @GetMapping("/discrepancies")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<List<com.backend.dto.response.TeamDiscrepancyResponse>> getTeamDiscrepancies() {
        List<com.backend.entity.Score> allScores = scoreRepository.findAll();

        java.util.Map<Long, List<com.backend.entity.Score>> scoresBySub = allScores.stream()
                .filter(s -> s.getScoreValue() != null && s.getSubmission() != null && s.getSubmission().getTeam() != null)
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getSubmission().getId()));

        List<com.backend.dto.response.TeamDiscrepancyResponse> result = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Long, List<com.backend.entity.Score>> entry : scoresBySub.entrySet()) {
            List<com.backend.entity.Score> scores = entry.getValue();
            if (scores.isEmpty()) continue;

            com.backend.entity.Submission sub = scores.get(0).getSubmission();
            com.backend.entity.Team team = sub.getTeam();
            com.backend.entity.TrackRoundMatrix matrix = sub.getMatrix();

            double minScore = Double.MAX_VALUE;
            double maxScore = Double.MIN_VALUE;
            double sum = 0.0;

            List<com.backend.dto.response.TeamDiscrepancyResponse.JudgeScoreDetailDto> judgeDetails = new java.util.ArrayList<>();

            for (com.backend.entity.Score s : scores) {
                double val = s.getScoreValue();
                if (val < minScore) minScore = val;
                if (val > maxScore) maxScore = val;
                sum += val;

                com.backend.entity.User judge = s.getJudge();
                judgeDetails.add(com.backend.dto.response.TeamDiscrepancyResponse.JudgeScoreDetailDto.builder()
                        .judgeId(judge != null ? judge.getId() : null)
                        .judgeName(judge != null ? judge.getFullName() : "Unknown Judge")
                        .judgeEmail(judge != null ? judge.getEmail() : "")
                        .score(val)
                        .comment(s.getComment())
                        .build());
            }

            int count = scores.size();
            double avg = sum / count;
            double maxDisc = count >= 2 ? (maxScore - minScore) : 0.0;

            double sumSq = 0.0;
            for (com.backend.entity.Score s : scores) {
                sumSq += Math.pow(s.getScoreValue() - avg, 2);
            }
            double stdDev = count >= 2 ? Math.sqrt(sumSq / (count - 1)) : 0.0;

            result.add(com.backend.dto.response.TeamDiscrepancyResponse.builder()
                    .submissionId(sub.getId())
                    .teamId(team != null ? team.getId() : null)
                    .teamName(team != null ? team.getName() : "Unknown Team")
                    .eventName(team != null && team.getEvent() != null ? team.getEvent().getName() : "")
                    .roundName(matrix != null && matrix.getRound() != null ? matrix.getRound().getName() : "")
                    .trackName(matrix != null && matrix.getTrack() != null ? matrix.getTrack().getName() : "")
                    .judgeScores(judgeDetails)
                    .averageScore(Math.round(avg * 100.0) / 100.0)
                    .maxDiscrepancy(Math.round(maxDisc * 100.0) / 100.0)
                    .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                    .isHighDiscrepancy(maxDisc > 15.0)
                    .build());
        }

        return ApiResponse.<List<com.backend.dto.response.TeamDiscrepancyResponse>>builder()
                .result(result)
                .build();
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

    private record ContinuousPairScore(double score1, double score2) {}
}