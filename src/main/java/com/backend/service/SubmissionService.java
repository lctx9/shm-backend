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
import com.backend.repository.ScoreRepository;
import com.backend.entity.Score;
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
    private final ScoreRepository scoreRepository;

    public SubmissionResponse submitWork(SubmissionRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        TrackRoundMatrix matrix = matrixRepository.findById(request.getMatrixId())
                .orElseThrow(() -> new AppException(ErrorCode.MATRIX_NOT_FOUND));

        requireTeamLeader(team.getId());

        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        if (memberCount < 3) {
            throw new RuntimeException("Đội thi của bạn chưa đủ điều kiện (tối thiểu 3 thành viên chính thức) để nộp bài dự thi.");
        }

        if (matrix.getTrack() != null && (team.getTrack() == null || !team.getTrack().getId().equals(matrix.getTrack().getId()))) {
            throw new AppException(ErrorCode.BUSINESS_ERROR);
        }

        if (submissionRepository.existsByTeamIdAndMatrixId(team.getId(), matrix.getId())) {
            throw new AppException(ErrorCode.SUBMISSION_ALREADY_EXISTS);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.backend.entity.HackathonEvent event = team.getEvent();
        if (event != null && event.getEventStartDate() != null && now.isBefore(event.getEventStartDate())) {
            throw new AppException(ErrorCode.EVENT_NOT_STARTED);
        }

        int currentOrder = matrix.getRound().getOrderIndex();
        if (currentOrder > 1) {
            List<TrackRoundMatrix> allEventMatrices = matrixRepository.findByRoundEventId(matrix.getRound().getEvent().getId());
            for (TrackRoundMatrix other : allEventMatrices) {
                if (other.getRound().getOrderIndex() == currentOrder - 1) {
                    boolean isPreceding = false;
                    if (matrix.getTrack() == null) {
                        isPreceding = true;
                    } else if (other.getTrack() != null && other.getTrack().getId().equals(matrix.getTrack().getId())) {
                        isPreceding = true;
                    }

                    if (isPreceding && other.getSubmissionDeadline() != null && now.isBefore(other.getSubmissionDeadline())) {
                        throw new AppException(ErrorCode.PREVIOUS_ROUND_NOT_ENDED);
                    }
                }
            }
        }

        if (matrix.getSubmissionStartDate() != null && now.isBefore(matrix.getSubmissionStartDate())) {
            throw new AppException(ErrorCode.SUBMISSION_NOT_STARTED);
        }
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new AppException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
        }

        Submission submission = Submission.builder()
                .team(team)
                .matrix(matrix)
                .fileUrl(request.getFileUrl())
                .submissionDataJson(request.getSubmissionDataJson())
                .isFlagged(false)
                .isGraded(false)
                .build();

        return toSubmissionResponse(submissionRepository.save(submission));
    }

    public List<SubmissionResponse> getMySubmissions(Long teamId, Long eventId) {
        User currentUser = getCurrentUser();
        List<TeamMember> memberships = teamMemberRepository.findAllByUser(currentUser);
        List<SubmissionResponse> result = new java.util.ArrayList<>();
        
        if (teamId != null) {
            memberships = memberships.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(teamId))
                    .toList();
        } else if (eventId != null) {
            memberships = memberships.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getEvent() != null && m.getTeam().getEvent().getId().equals(eventId))
                    .toList();
        }

        // Trả về TẤT CẢ bài nộp của mỗi đội (tất cả các vòng) để frontend có thể tự filter theo matrixId
        for (TeamMember m : memberships) {
            submissionRepository.findByTeamId(m.getTeam().getId()).stream()
                    .map(this::toSubmissionResponse)
                    .forEach(result::add);
        }
        return result;
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
        com.backend.entity.HackathonEvent event = submission.getTeam().getEvent();
        if (event != null && event.getEventStartDate() != null && now.isBefore(event.getEventStartDate())) {
            throw new AppException(ErrorCode.EVENT_NOT_STARTED);
        }

        int currentOrder = matrix.getRound().getOrderIndex();
        if (currentOrder > 1) {
            List<TrackRoundMatrix> allEventMatrices = matrixRepository.findByRoundEventId(matrix.getRound().getEvent().getId());
            for (TrackRoundMatrix other : allEventMatrices) {
                if (other.getRound().getOrderIndex() == currentOrder - 1) {
                    boolean isPreceding = false;
                    if (matrix.getTrack() == null) {
                        isPreceding = true;
                    } else if (other.getTrack() != null && other.getTrack().getId().equals(matrix.getTrack().getId())) {
                        isPreceding = true;
                    }

                    if (isPreceding && other.getSubmissionDeadline() != null && now.isBefore(other.getSubmissionDeadline())) {
                        throw new AppException(ErrorCode.PREVIOUS_ROUND_NOT_ENDED);
                    }
                }
            }
        }

        if (matrix.getSubmissionStartDate() != null && now.isBefore(matrix.getSubmissionStartDate())) {
            throw new AppException(ErrorCode.SUBMISSION_NOT_STARTED);
        }
        if (matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            throw new AppException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
        }

        // Clear all previous judge scores when team leader resubmits
        List<com.backend.entity.Score> oldScores = scoreRepository.findBySubmissionId(submission.getId());
        if (oldScores != null && !oldScores.isEmpty()) {
            scoreRepository.deleteAll(oldScores);
        }

        submission.setMatrix(matrix);
        submission.setFileUrl(request.getFileUrl());
        submission.setSubmissionDataJson(request.getSubmissionDataJson());
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

        // Default values (global view)
        Double scoreVal = submission.getScore();
        String criteriaScores = submission.getCriteriaScoresJson();
        String feedbackVal = submission.getFeedback();
        Boolean isGradedVal = submission.getIsGraded();

        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName();
            java.util.Optional<User> currentUserOpt = userRepository.findByEmail(email);
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                
                // 1. If current user is a participant (USER role), apply results publication check
                if (currentUser.getRole() == RoleType.USER) {
                    boolean resultsPublished = matrix != null 
                            && matrix.getRound() != null 
                            && matrix.getRound().getEvent() != null 
                            && Boolean.TRUE.equals(matrix.getRound().getEvent().getResultsPublished());
                    
                    if (!resultsPublished) {
                        scoreVal = null;
                        criteriaScores = null;
                        feedbackVal = null;
                        isGradedVal = false;
                    }
                } 
                // 2. If the current user is assigned as a judge, return their personal grading state.
                else if (matrix != null && matrix.getJudges() != null
                        && matrix.getJudges().stream().anyMatch(judge -> judge.getId().equals(currentUser.getId()))) {
                    Score personalScore = scoreRepository.findBySubmissionIdAndJudgeId(submission.getId(), currentUser.getId()).orElse(null);
                    if (personalScore != null) {
                        isGradedVal = true;
                        scoreVal = personalScore.getScoreValue();
                        criteriaScores = personalScore.getCriteriaScoresJson();
                        feedbackVal = personalScore.getComment();
                    } else {
                        isGradedVal = false;
                        scoreVal = null;
                        criteriaScores = null;
                        feedbackVal = null;
                    }
                }
            }
        }

        return SubmissionResponse.builder()
                .id(submission.getId())
                .teamId(team == null ? null : team.getId())
                .teamName(team == null ? null : team.getName())
                .matrixId(matrix == null ? null : matrix.getId())
                .trackName(matrix == null ? null : (matrix.getTrack() == null ? "Chung kết" : matrix.getTrack().getName()))
                .roundName(matrix == null ? null : matrix.getRound().getName())
                .fileUrl(submission.getFileUrl())
                .submissionDataJson(submission.getSubmissionDataJson())
                .flagged(submission.isFlagged())
                .flagReason(submission.getFlagReason())
                .score(scoreVal)
                .feedback(feedbackVal)
                .criteriaScoresJson(criteriaScores)
                .graded(isGradedVal)
                // Disqualification workflow — always expose so all judges see the same status
                .disqualificationStatus(team == null ? null : team.getDisqualificationStatus())
                .disqualificationReason(team == null ? null : team.getDisqualificationReason())
                .disqualifierEmail(team == null ? null : team.getDisqualifierEmail())
                .rejectionReason(team == null ? null : team.getRejectionReason())
                // Round matrix & schema fields
                .isPublished(matrix != null && Boolean.TRUE.equals(matrix.getIsPublished()))
                .guidelineUrl(matrix == null ? null : matrix.getGuidelineUrl())
                .submissionDeadline(matrix == null ? null : matrix.getSubmissionDeadline())
                .roundOrderIndex(matrix == null || matrix.getRound() == null ? null : matrix.getRound().getOrderIndex())
                .submissionFormSchema(matrix == null || matrix.getRound() == null || matrix.getRound().getEvent() == null ? null : matrix.getRound().getEvent().getSubmissionFormSchema())
                .build();
    }
}
