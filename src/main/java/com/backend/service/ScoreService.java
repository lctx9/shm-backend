package com.backend.service;

import com.backend.dto.request.ScoreRequest;
import com.backend.entity.AuditLog;
import com.backend.entity.Score;
import com.backend.entity.Submission;
import com.backend.entity.User;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.enums.RoleType;
import com.backend.repository.AuditLogRepository;
import com.backend.repository.ScoreRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ObjectMapper objectMapper;
    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TrackRoundMatrixRepository matrixRepository;

    @Transactional
    public Score gradeSubmission(ScoreRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User judge = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay giam khao"));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay bai nop"));

        boolean manager = judge.getRole() == RoleType.ADMIN || judge.getRole() == RoleType.COORDINATOR;
        boolean assignedJudge = submission.getMatrix() != null
                && submission.getMatrix().getJudges() != null
                && submission.getMatrix().getJudges().stream().anyMatch(user -> user.getId().equals(judge.getId()));
        if (!manager && !assignedJudge) {
            throw new RuntimeException("Bạn chưa được phân công làm giám khảo cho vòng đấu này");
        }

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

        // Calculate average score of all judges for this submission
        double avgScore = scoreRepository.findBySubmissionId(submission.getId()).stream()
                .map(Score::getScoreValue)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        avgScore = Math.round(avgScore * 10.0) / 10.0;

        submission.setScore(avgScore);
        submission.setCriteriaScoresJson(request.getCriteriaScoresJson());
        submission.setFeedback(request.getComment());
        submission.setIsGraded(true);
        submissionRepository.save(submission);

        promoteTopTeamsWhenRoundIsComplete(submission.getMatrix());

        return savedScore;
    }

    private void promoteTopTeamsWhenRoundIsComplete(TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getTopN() == null || matrix.getTopN() < 1) return;

        List<Submission> submissions = submissionRepository.findByMatrixId(matrix.getId());
        int requiredJudges = matrix.getJudges() == null ? 0 : matrix.getJudges().size();
        if (submissions.isEmpty() || requiredJudges < 1) return;

        boolean fullyGraded = submissions.stream()
                .allMatch(item -> scoreRepository.findBySubmissionId(item.getId()).size() >= requiredJudges);
        if (!fullyGraded) return;

        int nextOrder = matrix.getRound().getOrderIndex() + 1;
        Long eventId = matrix.getRound().getEvent().getId();
        TrackRoundMatrix nextMatrix = matrix.getTrack() == null
                ? null
                : matrixRepository.findByTrackIdAndRoundOrderIndex(matrix.getTrack().getId(), nextOrder)
                        .orElseGet(() -> matrixRepository
                                .findByRoundEventIdAndTrackIsNullAndRoundOrderIndex(eventId, nextOrder)
                                .orElse(null));
        if (nextMatrix == null) return;

        submissions.stream()
                .sorted(Comparator.comparingDouble(this::averageScore).reversed())
                .limit(matrix.getTopN())
                .forEach(item -> {
                    if (!submissionRepository.existsByTeamIdAndMatrixId(item.getTeam().getId(), nextMatrix.getId())) {
                        submissionRepository.save(Submission.builder()
                                .team(item.getTeam())
                                .matrix(nextMatrix)
                                .isGraded(false)
                                .build());
                    }
                });
    }

    private double averageScore(Submission submission) {
        return scoreRepository.findBySubmissionId(submission.getId()).stream()
                .map(Score::getScoreValue)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private Double resolveScore(ScoreRequest request) {
        if (request.getScoreValue() != null) {
            if (request.getScoreValue() < 0.0 || request.getScoreValue() > 100.0) {
                throw new RuntimeException("Điểm số phải nằm trong khoảng từ 0 đến 100");
            }
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
                    if (score < 0.0 || score > 100.0) {
                        throw new RuntimeException("Điểm thành phần phải nằm trong khoảng từ 0 đến 100");
                    }
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
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("Khong the tinh diem tu cau truc tieu chi");
        }
    }
}
