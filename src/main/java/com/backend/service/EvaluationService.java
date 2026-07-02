package com.backend.service;

import com.backend.dto.ScoreCriteriaRequest;
import com.backend.dto.SubmitScoreRequest;
import com.backend.entity.*;
import com.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EvaluationService {

    @Autowired
    private JudgeScoreRepository judgeScoreRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventScoringCriteriaRepository criteriaRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void submitOrUpdateScores(UUID judgeId, SubmitScoreRequest request) {
        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp hợp lệ!"));

        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Giám khảo!"));

        UUID eventId = submission.getRound().getEvent().getId();
        UUID teamId = submission.getTeam().getId();

        for (ScoreCriteriaRequest scoreDto : request.getCriteriaScores()) {
            EventScoringCriteria criteria = criteriaRepository.findById(scoreDto.getCriteriaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tiêu chí chấm điểm!"));

            if (scoreDto.getScore() > criteria.getMaxScore() || scoreDto.getScore() < 0) {
                throw new RuntimeException("Điểm số vượt quá mức tối đa cho phép: " + criteria.getMaxScore());
            }

            var existingScoreOpt = judgeScoreRepository.findBySubmissionIdAndJudgeIdAndCriteriaId(
                    submission.getId(), judge.getId(), criteria.getId());

            Double oldScore = null;
            String actionType = "SCORING";
            JudgeScore judgeScore;

            if (existingScoreOpt.isPresent()) {
                judgeScore = existingScoreOpt.get();
                oldScore = judgeScore.getScore();
                actionType = "UPDATE_SCORE";
                judgeScore.setScore(scoreDto.getScore());
            } else {
                judgeScore = new JudgeScore();
                judgeScore.setSubmission(submission);
                judgeScore.setJudge(judge);
                judgeScore.setCriteria(criteria);
                judgeScore.setScore(scoreDto.getScore());
                judgeScore.setCalibration(false);
            }

            judgeScoreRepository.save(judgeScore);

            // Ghi Audit Log chuẩn hóa theo yêu cầu từ hình ảnh image_7d953b.png
            AuditLog log = new AuditLog();
            log.setActorId(judgeId);
            log.setActionType(actionType);
            log.setEventId(eventId);
            log.setTeamId(teamId);
            log.setSubmissionId(submission.getId());

            String desc = actionType.equals("UPDATE_SCORE")
                    ? "Cập nhật điểm tiêu chí [" + criteria.getName() + "] từ: " + oldScore + " thành: " + scoreDto.getScore()
                    : "Chấm điểm mới tiêu chí [" + criteria.getName() + "]: " + scoreDto.getScore();
            log.setDescription(desc);
            log.setTimestamp(LocalDateTime.now());

            auditLogRepository.save(log);
        }
    }

    public List<JudgeScore> getScoresBySubmissionAndJudge(UUID submissionId, UUID judgeId) {
        return judgeScoreRepository.findBySubmissionIdAndJudgeId(submissionId, judgeId);
    }
}