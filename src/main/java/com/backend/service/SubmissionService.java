package com.backend.service;

import com.backend.dto.request.GradeRequest;
import com.backend.dto.request.SubmissionRequest;
import com.backend.entity.Submission;
import com.backend.entity.Team;
import com.backend.entity.TrackRoundMatrix;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamRepository;
// Bạn nhớ tạo TrackRoundMatrixRepository nhé
// import com.backend.repository.TrackRoundMatrixRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    // private final TrackRoundMatrixRepository matrixRepository;

    public Submission submitWork(SubmissionRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội thi"));

        /* Logic thực tế sẽ cần kiểm tra xem User hiện tại có phải là LEADER của Team này không
           Và TrackRoundMatrix (Vòng thi) có đang mở không (Deadline).
           Tạm thời comment để bạn chạy thử cấu trúc.

        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi"));
        */

        Submission submission = Submission.builder()
                .team(team)
                // .matrix(matrix)
                .fileUrl(request.getFileUrl())
                .isFlagged(false)
                .build();

        return submissionRepository.save(submission);
    }

    // Thêm vào SubmissionService.java

    public Submission getMySubmission() {
        // Trả về null để Frontend hiểu là chưa nộp bài, sẽ hiện form nộp mới (POST)
        // Khi làm DB thực tế, hãy query: return submissionRepository.findByTeamId(...);
        return null;
    }

    public Submission updateSubmission(Long id, SubmissionRequest request) {
        // Update logic vào Database
        // Submission sub = submissionRepository.findById(id)...
        // sub.setFileUrl(request.getFileUrl());
        // return submissionRepository.save(sub);
        return new Submission(); // Tạm thời trả về object rỗng để test frontend
    }

    public List<Submission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    @Transactional
    public Submission gradeSubmission(Long id, GradeRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setIsGraded(true); // Đánh dấu là đã chấm

        return submissionRepository.save(submission);
    }
}