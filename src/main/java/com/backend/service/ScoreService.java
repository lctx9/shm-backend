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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.backend.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ObjectMapper objectMapper;
    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TrackRoundMatrixRepository matrixRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Score gradeSubmission(ScoreRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User judge = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (submission.getMatrix() != null && Boolean.TRUE.equals(submission.getMatrix().getIsPublished())) {
            throw new RuntimeException("Kết quả vòng đấu đã được công bố, không thể tạo hoặc chỉnh sửa điểm");
        }

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

        Double finalScore = resolveScore(request, submission.getMatrix());
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

            AuditLog auditLog = AuditLog.builder()
                    .score(savedScore)
                    .judge(judge)
                    .oldScore(null)
                    .newScore(finalScore)
                    .teamName(submission.getTeam() != null ? submission.getTeam().getName() : "Đội thi")
                    .reason("CHẤM ĐIỂM: Tạo kết quả chấm lần đầu")
                    .build();
            auditLogRepository.save(auditLog);
        }

        Set<Long> assignedJudgeIds = submission.getMatrix().getJudges().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        List<Score> assignedScores = scoreRepository.findBySubmissionId(submission.getId()).stream()
                .filter(score -> score.getJudge() != null && assignedJudgeIds.contains(score.getJudge().getId()))
                .toList();

        // Calculate the public aggregate using only judges currently assigned to this matrix.
        double avgScore = assignedScores.stream()
                .map(Score::getScoreValue)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        avgScore = Math.round(avgScore * 10.0) / 10.0;

        submission.setScore(avgScore);
        submission.setCriteriaScoresJson(assignedScores.size() == 1
                ? assignedScores.get(0).getCriteriaScoresJson()
                : null);
        submission.setFeedback(aggregateFeedback(assignedScores));
        Set<Long> judgesWhoScored = assignedScores.stream()
                .map(Score::getJudge)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());
        submission.setIsGraded(!assignedJudgeIds.isEmpty() && judgesWhoScored.containsAll(assignedJudgeIds));
        submissionRepository.save(submission);

        notifyCoordinatorWhenRoundIsComplete(submission.getMatrix());

        return savedScore;
    }

    public void notifyCoordinatorWhenRoundIsComplete(TrackRoundMatrix matrix) {
        if (matrix == null || Boolean.TRUE.equals(matrix.getGradingCompletionNotified()) || !isMatrixFullyGraded(matrix)) {
            return;
        }

        String roundName = matrix.getRound() != null ? matrix.getRound().getName() : "Vòng đấu";
        List<User> coordinators = userRepository.findByRole(RoleType.COORDINATOR);
        for (User coord : coordinators) {
            notificationRepository.save(com.backend.entity.Notification.builder()
                    .title("Tất cả giám khảo đã chấm xong " + roundName)
                    .body("Tất cả bài nộp tại " + roundName + " đã được các giám khảo hoàn tất chấm điểm. Vui lòng rà soát bảng xếp hạng và bấm Công bố kết quả & Mở vòng đấu tiếp theo.")
                    .recipient(coord)
                    .actionUrl("/events")
                    .build());
        }
        matrix.setGradingCompletionNotified(true);
        matrixRepository.save(matrix);
    }

    public void promoteTopTeamsWhenRoundIsComplete(TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getTopN() == null || matrix.getTopN() < 1) return;

        List<Submission> submissions = submissionRepository.findByMatrixId(matrix.getId()).stream()
                .filter(this::isEligibleSubmission)
                .toList();
        if (!isMatrixFullyGraded(matrix)) return;

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
            if (matrix.getTrack() != null
                    && nextSub.getTeam().getTrack() != null
                    && !matrix.getTrack().getId().equals(nextSub.getTeam().getTrack().getId())) {
                continue;
            }
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
        if (submission.getMatrix() == null || submission.getMatrix().getJudges() == null) {
            return 0;
        }
        Set<Long> assignedJudgeIds = submission.getMatrix().getJudges().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        return scoreRepository.findBySubmissionId(submission.getId()).stream()
                .filter(score -> score.getJudge() != null && assignedJudgeIds.contains(score.getJudge().getId()))
                .map(Score::getScoreValue)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    public boolean isMatrixFullyGraded(TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getJudges() == null || matrix.getJudges().isEmpty()) {
            return false;
        }
        List<Submission> submissions = submissionRepository.findByMatrixId(matrix.getId()).stream()
                .filter(this::isEligibleSubmission)
                .toList();
        if (submissions.isEmpty()) {
            return false;
        }

        Set<Long> requiredJudgeIds = matrix.getJudges().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        return submissions.stream().allMatch(item -> {
            Set<Long> scoredJudgeIds = scoreRepository.findBySubmissionId(item.getId()).stream()
                    .map(Score::getJudge)
                    .filter(java.util.Objects::nonNull)
                    .map(User::getId)
                    .collect(Collectors.toSet());
            return scoredJudgeIds.containsAll(requiredJudgeIds);
        });
    }

    private boolean isEligibleSubmission(Submission submission) {
        return submission.getFileUrl() != null
                && !submission.getFileUrl().isBlank()
                && (submission.getTeam() == null
                || !"APPROVED".equals(submission.getTeam().getDisqualificationStatus()));
    }

    private String aggregateFeedback(List<Score> scores) {
        List<Score> scoresWithComments = scores.stream()
                .filter(score -> score.getComment() != null && !score.getComment().isBlank())
                .toList();
        if (scoresWithComments.size() == 1) {
            return scoresWithComments.get(0).getComment().trim();
        }
        String feedback = scoresWithComments.stream()
                .map(score -> {
                    String judgeName = score.getJudge() == null || score.getJudge().getFullName() == null
                            ? "Giám khảo"
                            : score.getJudge().getFullName();
                    return judgeName + ": " + score.getComment().trim();
                })
                .collect(Collectors.joining("\n\n"));
        return feedback.isBlank() ? null : feedback;
    }

    private Double resolveScore(ScoreRequest request, TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getScoringCriteriaJson() == null
                || matrix.getScoringCriteriaJson().isBlank()) {
            return resolveScore(request);
        }
        if (request.getCriteriaScoresJson() == null || request.getCriteriaScoresJson().isBlank()) {
            throw new AppException(ErrorCode.SCORE_REQUIRED);
        }

        try {
            JsonNode configuredRoot = objectMapper.readTree(matrix.getScoringCriteriaJson());
            JsonNode submittedRoot = objectMapper.readTree(request.getCriteriaScoresJson());
            if (!configuredRoot.isArray() || configuredRoot.isEmpty() || !submittedRoot.isArray()) {
                throw new AppException(ErrorCode.CRITERIA_SCORE_PARSE_FAILED);
            }

            Map<String, JsonNode> submittedByKey = new LinkedHashMap<>();
            for (JsonNode submitted : submittedRoot) {
                String key = criterionKey(submitted);
                if (key.isBlank() || submittedByKey.putIfAbsent(key, submitted) != null) {
                    throw new AppException(ErrorCode.CRITERIA_SCORE_PARSE_FAILED);
                }
            }

            double weightedSum = 0;
            double totalWeight = 0;
            Set<String> configuredKeys = new java.util.HashSet<>();
            for (JsonNode criterion : configuredRoot) {
                String key = criterionKey(criterion);
                if (key.isBlank() || !configuredKeys.add(key)) {
                    throw new AppException(ErrorCode.CRITERIA_SCORE_PARSE_FAILED);
                }
                JsonNode submitted = submittedByKey.get(key);
                if (submitted == null || !submitted.has("score")) {
                    throw new AppException(ErrorCode.SCORE_REQUIRED);
                }

                double maxScore = criterion.path("maxScore").asDouble(0);
                double weight = criterion.path("weight").asDouble(0);
                double score = parseScore(submitted.get("score"));
                if (!Double.isFinite(maxScore) || !Double.isFinite(weight) || !Double.isFinite(score)
                        || maxScore <= 0 || weight <= 0 || score < 0 || score > maxScore) {
                    throw new AppException(ErrorCode.INVALID_CRITERIA_SCORE);
                }
                weightedSum += (score / maxScore * 100.0) * weight;
                totalWeight += weight;
            }

            if (submittedByKey.size() != configuredKeys.size() || totalWeight <= 0) {
                throw new AppException(ErrorCode.INVALID_CRITERIA_WEIGHT);
            }
            return Math.round((weightedSum / totalWeight) * 10.0) / 10.0;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.CRITERIA_SCORE_PARSE_FAILED);
        }
    }

    private String criterionKey(JsonNode criterion) {
        String id = criterion.path("id").asText("").trim();
        return id.isBlank() ? criterion.path("label").asText("").trim() : id;
    }

    private double parseScore(JsonNode scoreNode) {
        if (scoreNode == null || scoreNode.isNull()) {
            throw new AppException(ErrorCode.SCORE_REQUIRED);
        }
        try {
            return Double.parseDouble(scoreNode.asText());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_CRITERIA_SCORE);
        }
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
