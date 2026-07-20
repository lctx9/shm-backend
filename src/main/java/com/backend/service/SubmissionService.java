package com.backend.service;

import com.backend.dto.request.GradeRequest;
import com.backend.dto.request.SubmissionRequest;
import com.backend.dto.response.SubmissionResponse;
import com.backend.entity.Submission;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
import com.backend.entity.enums.MemberRole;
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

        requireTeamLeader(team.getId());

        if (matrix.getTrack() != null && (team.getTrack() == null || !team.getTrack().getId().equals(matrix.getTrack().getId()))) {
            throw new RuntimeException("Vòng thi không thuộc hạng mục của đội");
        }

        if (submissionRepository.existsByTeamIdAndMatrixId(team.getId(), matrix.getId())) {
            throw new RuntimeException("Đội thi đã nộp bài cho vòng thi này rồi");
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new RuntimeException("Hạn nộp bài cho vòng thi này đã kết thúc (Hạn chót: " + matrix.getSubmissionDeadline() + ")");
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

        if (submission.getTeam() == null || !submission.getTeam().getId().equals(request.getTeamId())) {
            throw new RuntimeException("Bài nộp không thuộc đội đã chọn");
        }
        requireTeamLeader(submission.getTeam().getId());

        if (matrix.getTrack() != null && (submission.getTeam().getTrack() == null || !submission.getTeam().getTrack().getId().equals(matrix.getTrack().getId()))) {
            throw new RuntimeException("Vòng thi không thuộc hạng mục của đội");
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new RuntimeException("Hạn nộp bài cho vòng thi này đã kết thúc (Hạn chót: " + matrix.getSubmissionDeadline() + ")");
        }

        submission.setMatrix(matrix);
        submission.setFileUrl(request.getFileUrl());
        submission.setIsGraded(false);
        submission.setScore(null);
        submission.setFeedback(null);
        submission.setCriteriaScoresJson(null);

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> getAllSubmissions() {
        User user = getCurrentUser();
        if (user.getRole() == RoleType.ADMIN || user.getRole() == RoleType.COORDINATOR) {
            return submissionRepository.findAll().stream().map(this::toSubmissionResponse).toList();
        }

        return submissionRepository.findAll().stream()
                .filter(submission -> isAssignedToMatrix(user, submission.getMatrix()))
                .map(this::toSubmissionResponse)
                .toList();
    }

    private Team getCurrentUserTeam() {
        User currentUser = getCurrentUser();
        TeamMember membership = teamMemberRepository.findByUser(currentUser).orElse(null);
        return membership == null ? null : membership.getTeam();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private boolean isAssignedToMatrix(User user, TrackRoundMatrix matrix) {
        if (matrix == null) return false;
        boolean mentor = matrix.getMentors() != null && matrix.getMentors().stream().anyMatch(item -> item.getId().equals(user.getId()));
        boolean judge = matrix.getJudges() != null && matrix.getJudges().stream().anyMatch(item -> item.getId().equals(user.getId()));
        return mentor || judge;
    }

    private void requireTeamLeader(Long teamId) {
        TeamMember membership = teamMemberRepository.findByUser(getCurrentUser())
                .orElseThrow(() -> new RuntimeException("Bạn chưa tham gia đội thi"));
        if (!membership.getTeam().getId().equals(teamId) || membership.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Leader của đội mới được nộp hoặc cập nhật bài");
        }
    }

    private SubmissionResponse toSubmissionResponse(Submission submission) {
        TrackRoundMatrix matrix = submission.getMatrix();
        Team team = submission.getTeam();

        return SubmissionResponse.builder()
                .id(submission.getId())
                .teamId(team == null ? null : team.getId())
                .teamName(team == null ? null : team.getName())
                .matrixId(matrix == null ? null : matrix.getId())
                .trackName(matrix == null ? null : (matrix.getTrack() == null ? "Chung kết" : matrix.getTrack().getName()))
                .roundName(matrix == null ? null : matrix.getRound().getName())
                .fileUrl(submission.getFileUrl())
                .flagged(submission.isFlagged())
                .flagReason(submission.getFlagReason())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .criteriaScoresJson(submission.getCriteriaScoresJson())
                .graded(submission.getIsGraded())
                .build();
    }
}
