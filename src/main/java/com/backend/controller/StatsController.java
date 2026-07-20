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
        long totalTeamsCount = teamRepository.count(); // Hàm count() có sẵn
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
}