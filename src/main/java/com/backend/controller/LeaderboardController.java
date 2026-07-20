package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.LeaderboardResponse;
import com.backend.dto.response.TeamMemberResponse;
import com.backend.entity.Submission;
import com.backend.entity.TeamMember;
import com.backend.entity.User;
import com.backend.entity.HackathonEvent;
import com.backend.entity.enums.RoleType;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LeaderboardController {

    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard() {
        boolean showUnpublished = false;
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String currentEmail = auth.getName();
            Optional<User> userOpt = userRepository.findByEmail(currentEmail);
            if (userOpt.isPresent()) {
                RoleType role = userOpt.get().getRole();
                if (role == RoleType.ADMIN || role == RoleType.COORDINATOR) {
                    showUnpublished = true;
                }
            }
        }

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardResponse> rows = new ArrayList<>();
        final boolean finalShowUnpublished = showUnpublished;

        for (Submission submission : submissionRepository.findByIsGradedTrueOrderByScoreDesc()) {
            HackathonEvent event = submission.getTeam() == null || submission.getTeam().getEvent() == null 
                    ? null 
                    : submission.getTeam().getEvent();
            if (event == null) continue;

            // If results are not published and user is not Coordinator/Admin, skip
            if (!Boolean.TRUE.equals(event.getResultsPublished()) && !finalShowUnpublished) {
                continue;
            }

            String trackName = "Chưa gắn track";
            if (submission.getMatrix() != null) {
                if (submission.getMatrix().getTrack() == null) {
                    trackName = "Chung kết";
                } else {
                    trackName = submission.getMatrix().getTrack().getName();
                }
            }

            rows.add(LeaderboardResponse.builder()
                    .id(submission.getId())
                    .rank(rank.getAndIncrement())
                    .eventId(event.getId())
                    .eventName(event.getName())
                    .eventYear(event.getYear())
                    .teamName(submission.getTeam() == null ? "Không rõ đội" : submission.getTeam().getName())
                    .track(trackName)
                    .projectName(submission.getFileUrl())
                    .description(submission.getFeedback())
                    .score(submission.getScore())
                    .members(submission.getTeam() == null ? List.of() : teamMemberRepository.findByTeamId(submission.getTeam().getId()).stream()
                            .map(this::toMemberResponse)
                            .toList())
                    .build());
        }

        return ApiResponse.<List<LeaderboardResponse>>builder()
                .result(rows)
                .build();
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentId(user.getStudentId())
                .universityName(user.getUniversityName())
                .role(member.getRole())
                .build();
    }
}
