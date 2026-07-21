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
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
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
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new AppException(ErrorCode.MATRIX_NOT_FOUND));

        requireTeamLeader(team.getId());

        if (matrix.getTrack() != null && (team.getTrack() == null || !team.getTrack().getId().equals(matrix.getTrack().getId()))) {
            throw new AppException(ErrorCode.BUSINESS_ERROR);
        }

        if (submissionRepository.existsByTeamIdAndMatrixId(team.getId(), matrix.getId())) {
            throw new AppException(ErrorCode.SUBMISSION_ALREADY_EXISTS);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new AppException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
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

    public SubmissionResponse getMySubmission(Long teamId, Long eventId) {
        Team team = null;
        if (teamId != null) {
            team = teamRepository.findById(teamId).orElse(null);
        } else {
            team = getCurrentUserTeam(eventId);
        }
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
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new AppException(ErrorCode.MATRIX_NOT_FOUND));

        if (submission.getTeam() == null || !submission.getTeam().getId().equals(request.getTeamId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        requireTeamLeader(submission.getTeam().getId());

        if (matrix.getTrack() != null && (submission.getTeam().getTrack() == null || !submission.getTeam().getTrack().getId().equals(matrix.getTrack().getId()))) {
            throw new AppException(ErrorCode.BUSINESS_ERROR);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new AppException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
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

    private Team getCurrentUserTeam(Long eventId) {
        User currentUser = getCurrentUser();
        List<TeamMember> memberships = teamMemberRepository.findByUser(currentUser);
        if (memberships.isEmpty()) {
            return null;
        }

        TeamMember target = null;
        if (eventId != null) {
            target = memberships.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getEvent() != null && m.getTeam().getEvent().getId().equals(eventId))
                    .findFirst()
                    .orElse(null);
        } else {
            target = memberships.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getEvent() != null)
                    .max(Comparator.comparing(m -> m.getTeam().getEvent().getId()))
                    .orElse(memberships.get(0));
        }

        return target == null ? null : target.getTeam();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isAssignedToMatrix(User user, TrackRoundMatrix matrix) {
        if (matrix == null) return false;
        boolean mentor = matrix.getMentors() != null && matrix.getMentors().stream().anyMatch(item -> item.getId().equals(user.getId()));
        boolean judge = matrix.getJudges() != null && matrix.getJudges().stream().anyMatch(item -> item.getId().equals(user.getId()));
        return mentor || judge;
    }

    private void requireTeamLeader(Long teamId) {
        TeamMember membership = teamMemberRepository.findByUserIdAndTeamId(getCurrentUser().getId(), teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        if (membership.getRole() != MemberRole.LEADER) {
            throw new AppException(ErrorCode.NOT_TEAM_LEADER);
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
