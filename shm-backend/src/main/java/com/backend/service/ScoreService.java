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
        // 1. Lấy Giám khảo đang đăng nhập
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User judge = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giám khảo"));

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        // 2. Kiểm tra xem Giám khảo này đã chấm bài này chưa
        Optional<Score> existingScoreOpt = scoreRepository.findBySubmissionIdAndJudgeId(
                submission.getId(), judge.getId());

        if (existingScoreOpt.isPresent()) {
            // NẾU ĐÃ CHẤM RỒI -> TIẾN HÀNH SỬA ĐIỂM VÀ LƯU AUDIT LOG
            Score existingScore = existingScoreOpt.get();
            Double oldScoreValue = existingScore.getScoreValue();

            if (request.getEditReason() == null || request.getEditReason().isEmpty()) {
                throw new RuntimeException("Phải cung cấp lý do (editReason) khi sửa điểm!");
            }

            // Ghi log
            AuditLog auditLog = AuditLog.builder()
                    .score(existingScore)
                    .judge(judge)
                    .oldScore(oldScoreValue)
                    .newScore(request.getScoreValue())
                    .reason(request.getEditReason())
                    .build();
            auditLogRepository.save(auditLog);

            // Cập nhật điểm mới
            existingScore.setScoreValue(request.getScoreValue());
            existingScore.setComment(request.getComment());
            return scoreRepository.save(existingScore);

        } else {
            // NẾU CHƯA CHẤM -> TẠO ĐIỂM MỚI
            Score newScore = Score.builder()
                    .submission(submission)
                    .judge(judge)
                    .scoreValue(request.getScoreValue())
                    .comment(request.getComment())
                    .build();
            return scoreRepository.save(newScore);
        }
    }
}