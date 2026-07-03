package com.backend.service;

import com.backend.dto.request.ScoreRequest;
import com.backend.entity.AuditLog;
import com.backend.entity.Score;
import com.backend.entity.Submission;
import com.backend.entity.User;
import com.backend.repository.AuditLogRepository;
import com.backend.repository.ScoreRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ObjectMapper objectMapper;
    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public Score gradeSubmission(ScoreRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User judge = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay giam khao"));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay bai nop"));

        Double finalScore = resolveScore(request);
        Optional<Score> existingScoreOpt = scoreRepository.findBySubmissionIdAndJudgeId(
                submission.getId(), judge.getId());

        Score savedScore;
        if (existingScoreOpt.isPresent()) {
            Score existingScore = existingScoreOpt.get();
            Double oldScoreValue = existingScore.getScoreValue();

            if (request.getEditReason() == null || request.getEditReason().isBlank()) {
                throw new RuntimeException("Phai cung cap ly do khi sua diem");
            }

            AuditLog auditLog = AuditLog.builder()
                    .score(existingScore)
                    .judge(judge)
                    .oldScore(oldScoreValue)
                    .newScore(finalScore)
                    .reason(request.getEditReason())
                    .build();
            auditLogRepository.save(auditLog);

            existingScore.setScoreValue(finalScore);
            existingScore.setCriteriaScoresJson(request.getCriteriaScoresJson());
            existingScore.setComment(request.getComment());
            savedScore = scoreRepository.save(existingScore);
        } else {
            Score newScore = Score.builder()
                    .submission(submission)
                    .judge(judge)
                    .scoreValue(finalScore)
                    .criteriaScoresJson(request.getCriteriaScoresJson())
                    .comment(request.getComment())
                    .build();
            savedScore = scoreRepository.save(newScore);
        }

        submission.setScore(finalScore);
        submission.setCriteriaScoresJson(request.getCriteriaScoresJson());
        submission.setFeedback(request.getComment());
        submission.setIsGraded(true);
        submissionRepository.save(submission);

        return savedScore;
    }

    private Double resolveScore(ScoreRequest request) {
        if (request.getScoreValue() != null) {
            return request.getScoreValue();
        }
        if (request.getCriteriaScoresJson() == null || request.getCriteriaScoresJson().isBlank()) {
            throw new RuntimeException("Phai nhap diem cham");
        }

        try {
            JsonNode root = objectMapper.readTree(request.getCriteriaScoresJson());
            double weightedSum = 0;
            double totalWeight = 0;

            if (root.isArray()) {
                for (JsonNode item : root) {
                    double score = item.path("score").asDouble(0);
                    double weight = item.path("weight").asDouble(1);
                    weightedSum += score * weight;
                    totalWeight += weight;
                }
            }

            if (totalWeight <= 0) {
                throw new RuntimeException("Tong trong so tieu chi phai lon hon 0");
            }
            return Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
        } catch (Exception ex) {
            throw new RuntimeException("Khong the tinh diem tu cau truc tieu chi");
        }
    }
}
