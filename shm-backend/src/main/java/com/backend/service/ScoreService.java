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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public Score gradeSubmission(ScoreRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User judge = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giám khảo"));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        Optional<Score> existingScoreOpt = scoreRepository.findBySubmissionIdAndJudgeId(
                submission.getId(), judge.getId());

        Score savedScore;
        if (existingScoreOpt.isPresent()) {
            Score existingScore = existingScoreOpt.get();
            Double oldScoreValue = existingScore.getScoreValue();

            if (request.getEditReason() == null || request.getEditReason().isBlank()) {
                throw new RuntimeException("Phải cung cấp lý do khi sửa điểm");
            }

            AuditLog auditLog = AuditLog.builder()
                    .score(existingScore)
                    .judge(judge)
                    .oldScore(oldScoreValue)
                    .newScore(request.getScoreValue())
                    .reason(request.getEditReason())
                    .build();
            auditLogRepository.save(auditLog);

            existingScore.setScoreValue(request.getScoreValue());
            existingScore.setComment(request.getComment());
            savedScore = scoreRepository.save(existingScore);
        } else {
            Score newScore = Score.builder()
                    .submission(submission)
                    .judge(judge)
                    .scoreValue(request.getScoreValue())
                    .comment(request.getComment())
                    .build();
            savedScore = scoreRepository.save(newScore);
        }

        submission.setScore(request.getScoreValue());
        submission.setFeedback(request.getComment());
        submission.setIsGraded(true);
        submissionRepository.save(submission);

        return savedScore;
    }
}
