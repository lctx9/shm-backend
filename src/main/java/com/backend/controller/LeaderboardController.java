package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.LeaderboardResponse;
import com.backend.dto.response.TeamMemberResponse;
import com.backend.entity.Submission;
import com.backend.entity.TeamMember;
import com.backend.entity.User;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LeaderboardController {

    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;

    @GetMapping
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard() {
        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardResponse> rows = new ArrayList<>();

        for (Submission submission : submissionRepository.findByIsGradedTrueOrderByScoreDesc()) {
            rows.add(LeaderboardResponse.builder()
                    .id(submission.getId())
                    .rank(rank.getAndIncrement())
                    .eventId(submission.getTeam() == null || submission.getTeam().getEvent() == null ? null : submission.getTeam().getEvent().getId())
                    .eventName(submission.getTeam() == null || submission.getTeam().getEvent() == null ? null : submission.getTeam().getEvent().getName())
                    .eventYear(submission.getTeam() == null || submission.getTeam().getEvent() == null ? null : submission.getTeam().getEvent().getYear())
                    .teamName(submission.getTeam() == null ? "Không rõ đội" : submission.getTeam().getName())
                    .track(submission.getMatrix() == null ? "Chưa gắn track" : submission.getMatrix().getTrack().getName())
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
