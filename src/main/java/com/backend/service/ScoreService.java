package com.backend.service;

import com.backend.dto.request.ScoreRequest;
import com.backend.entity.AuditLog;
import com.backend.entity.Score;
import com.backend.entity.Submission;
import com.backend.entity.User;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
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
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        boolean assignedJudge = submission.getMatrix() != null
                && submission.getMatrix().getJudges() != null
                && submission.getMatrix().getJudges().stream().anyMatch(user -> user.getId().equals(judge.getId()));
        if (!assignedJudge) {
            throw new AppException(ErrorCode.JUDGE_NOT_ASSIGNED);
        }

        if (submission.getFileUrl() == null || submission.getFileUrl().isBlank()) {
            throw new RuntimeException("Đội thi chưa nộp bài giải cho vòng đấu này, không thể chấm điểm");
        }

        if (submission.getTeam() != null && "PENDING".equals(submission.getTeam().getDisqualificationStatus())) {
            throw new RuntimeException("Đội thi này đang trong quá trình xử lý kỷ luật/chờ duyệt loại, không thể chấm điểm");
        }
        if (submission.getTeam() != null && "APPROVED".equals(submission.getTeam().getDisqualificationStatus())) {
            throw new RuntimeException("Đội thi này đã bị loại khỏi giải đấu, không thể chấm điểm");
        }

        if (submission.getMatrix() != null && submission.getMatrix().getSubmissionDeadline() != null) {
            if (java.time.LocalDateTime.now().isBefore(submission.getMatrix().getSubmissionDeadline())) {
                throw new RuntimeException("Hạn nộp bài của vòng đấu này chưa kết thúc, giám khảo chưa thể chấm điểm");
            }
        }

        Double finalScore = resolveScore(request);
        Optional<Score> existingScoreOpt = scoreRepository.findBySubmissionIdAndJudgeId(
                submission.getId(), judge.getId());

        Score savedScore;
        if (existingScoreOpt.isPresent()) {
            Score existingScore = existingScoreOpt.get();
            Double oldScoreValue = existingScore.getScoreValue();

            if (request.getEditReason() == null || request.getEditReason().isBlank()) {
                throw new AppException(ErrorCode.EDIT_REASON_REQUIRED);
            }

            AuditLog auditLog = AuditLog.builder()
                    .score(existingScore)
                    .judge(judge)
                    .oldScore(oldScoreValue)
                    .newScore(finalScore)
                    .teamName(submission.getTeam() != null ? submission.getTeam().getName() : "Đội thi")
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

        // C-03: Sắp xếp đồng nhất với Leaderboard: điểm trung bình giảm dần, nếu bằng điểm thì ai nộp trước (createdAt) xếp hạng cao hơn
        List<Submission> topSubmissions = submissions.stream()
                .sorted((s1, s2) -> {
                    double score1 = averageScore(s1);
                    double score2 = averageScore(s2);
                    int scoreCompare = Double.compare(score2, score1);
                    if (scoreCompare != 0) return scoreCompare;
                    if (s1.getCreatedAt() == null && s2.getCreatedAt() == null) return 0;
                    if (s1.getCreatedAt() == null) return 1;
                    if (s2.getCreatedAt() == null) return -1;
                    return s1.getCreatedAt().compareTo(s2.getCreatedAt());
                })
                .limit(matrix.getTopN())
                .toList();

        List<Long> topTeamIds = topSubmissions.stream().map(s -> s.getTeam().getId()).toList();

        // C-02: Thu hồi thăng hạng các đội bị rớt khỏi top N (chỉ hạ hạng nếu chưa nộp bài và chưa được chấm ở vòng sau)
        List<Submission> nextRoundSubmissions = submissionRepository.findByMatrixId(nextMatrix.getId());
        for (Submission nextSub : nextRoundSubmissions) {
            Long teamId = nextSub.getTeam().getId();
            if (!topTeamIds.contains(teamId)
                    && (nextSub.getFileUrl() == null || nextSub.getFileUrl().isBlank())
                    && !Boolean.TRUE.equals(nextSub.getIsGraded())) {
                submissionRepository.delete(nextSub);
            }
        }

        // Thăng hạng các đội trong top N (tạo bản ghi placeholder nếu chưa có)
        topSubmissions.forEach(item -> {
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
                throw new AppException(ErrorCode.INVALID_SCORE_RANGE);
            }
            return request.getScoreValue();
        }
        if (request.getCriteriaScoresJson() == null || request.getCriteriaScoresJson().isBlank()) {
            throw new AppException(ErrorCode.SCORE_REQUIRED);
        }

        try {
            JsonNode root = objectMapper.readTree(request.getCriteriaScoresJson());
            double weightedSum = 0;
            double totalWeight = 0;

            if (root.isArray()) {
                for (JsonNode item : root) {
                    double score = item.path("score").asDouble(0);
                    if (score < 0.0 || score > 100.0) {
                        throw new AppException(ErrorCode.INVALID_CRITERIA_SCORE);
                    }
                    double weight = item.path("weight").asDouble(1);
                    weightedSum += score * weight;
                    totalWeight += weight;
                }
            }

            if (totalWeight <= 0) {
                throw new AppException(ErrorCode.INVALID_CRITERIA_WEIGHT);
            }
            return Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.CRITERIA_SCORE_PARSE_FAILED);
        }
    }
}
