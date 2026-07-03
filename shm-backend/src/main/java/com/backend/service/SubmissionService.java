package com.backend.service;

import com.backend.dto.request.SubmissionRequest;
import com.backend.entity.Submission;
import com.backend.entity.Team;
import com.backend.entity.TrackRoundMatrix;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamRepository;
// Bạn nhớ tạo TrackRoundMatrixRepository nhé
// import com.backend.repository.TrackRoundMatrixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}