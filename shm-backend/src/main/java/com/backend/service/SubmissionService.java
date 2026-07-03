package com.backend.service;

import com.backend.dto.request.GradeRequest;
import com.backend.dto.request.SubmissionRequest;
import com.backend.dto.response.SubmissionResponse;
import com.backend.entity.Submission;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TrackRoundMatrixRepository matrixRepository;

    public SubmissionResponse submitWork(SubmissionRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội thi"));
        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi"));

        if (team.getTrack() == null || !team.getTrack().getId().equals(matrix.getTrack().getId())) {
            throw new RuntimeException("Vòng thi không thuộc hạng mục của đội");
        }

        Submission submission = Submission.builder()
                .team(team)
                .matrix(matrix)
                .fileUrl(request.getFileUrl())
                .isFlagged(false)
                .isGraded(false)
                .build();

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    public SubmissionResponse getMySubmission() {
        Team team = getCurrentUserTeam();
        if (team == null) {
            return null;
        }

        return submissionRepository.findByTeamId(team.getId()).stream()
                .max(Comparator.comparing(Submission::getCreatedAt))
                .map(this::toSubmissionResponse)
                .orElse(null);
    }

    public SubmissionResponse updateSubmission(Long id, SubmissionRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));
        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi"));

        submission.setMatrix(matrix);
        submission.setFileUrl(request.getFileUrl());
        submission.setIsGraded(false);
        submission.setScore(null);
        submission.setFeedback(null);

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> getAllSubmissions() {
        return submissionRepository.findAll().stream().map(this::toSubmissionResponse).toList();
    }

    @Transactional
    public SubmissionResponse gradeSubmission(Long id, GradeRequest request) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setIsGraded(true);

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    private Team getCurrentUserTeam() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        TeamMember membership = teamMemberRepository.findByUser(currentUser).orElse(null);
        return membership == null ? null : membership.getTeam();
    }

    private SubmissionResponse toSubmissionResponse(Submission submission) {
        TrackRoundMatrix matrix = submission.getMatrix();
        Team team = submission.getTeam();

        return SubmissionResponse.builder()
                .id(submission.getId())
                .teamId(team == null ? null : team.getId())
                .teamName(team == null ? null : team.getName())
                .matrixId(matrix == null ? null : matrix.getId())
                .trackName(matrix == null ? null : matrix.getTrack().getName())
                .roundName(matrix == null ? null : matrix.getRound().getName())
                .fileUrl(submission.getFileUrl())
                .flagged(submission.isFlagged())
                .flagReason(submission.getFlagReason())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .graded(submission.getIsGraded())
                .build();
    }
}
