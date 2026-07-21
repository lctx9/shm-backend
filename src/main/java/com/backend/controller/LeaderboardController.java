package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.LeaderboardResponse;
import com.backend.dto.response.TeamMemberResponse;
import com.backend.entity.Submission;
import com.backend.entity.TeamMember;
import com.backend.entity.User;
import com.backend.entity.HackathonEvent;
import com.backend.entity.enums.RoleType;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final HackathonEventRepository eventRepository;

    @GetMapping
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard(@RequestParam(required = false) Long eventId) {
        Long targetEventId = eventId;
        if (targetEventId == null) {
            targetEventId = getDefaultEventId();
        }

        if (targetEventId == null) {
            return ApiResponse.<List<LeaderboardResponse>>builder()
                    .result(new ArrayList<>())
                    .build();
        }

        HackathonEvent event = eventRepository.findById(targetEventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));

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

        if (!Boolean.TRUE.equals(event.getResultsPublished()) && !showUnpublished) {
            return ApiResponse.<List<LeaderboardResponse>>builder()
                    .result(new ArrayList<>())
                    .build();
        }

        List<Submission> submissions = submissionRepository.findFinalRoundGradedSubmissions(targetEventId);

        List<Submission> sortedSubmissions = submissions.stream()
                .sorted((s1, s2) -> {
                    double score1 = s1.getScore() != null ? s1.getScore() : 0.0;
                    double score2 = s2.getScore() != null ? s2.getScore() : 0.0;
                    int scoreCompare = Double.compare(score2, score1);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    if (s1.getCreatedAt() == null && s2.getCreatedAt() == null) return 0;
                    if (s1.getCreatedAt() == null) return 1;
                    if (s2.getCreatedAt() == null) return -1;
                    return s1.getCreatedAt().compareTo(s2.getCreatedAt());
                })
                .toList();

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardResponse> rows = new ArrayList<>();

        for (Submission submission : sortedSubmissions) {
            String trackName = "Chung kết";

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

    @GetMapping("/all")
    public ApiResponse<List<LeaderboardResponse>> getGlobalLeaderboard() {
        // Get all graded final-round submissions across every event
        List<Submission> allSubmissions = submissionRepository.findAllFinalRoundGradedSubmissions();

        // Keep best score per team (across all events they participated in)
        Map<Long, Submission> bestByTeam = new HashMap<>();
        for (Submission s : allSubmissions) {
            if (s.getTeam() == null) continue;
            Long teamId = s.getTeam().getId();
            double score = s.getScore() != null ? s.getScore() : 0.0;
            Submission existing = bestByTeam.get(teamId);
            double existingScore = existing != null && existing.getScore() != null ? existing.getScore() : -1.0;
            if (score > existingScore) {
                bestByTeam.put(teamId, s);
            }
        }

        // Sort by score desc, then by submission time asc
        List<Submission> sorted = bestByTeam.values().stream()
                .sorted(Comparator
                        .comparingDouble((Submission s) -> s.getScore() != null ? s.getScore() : 0.0).reversed()
                        .thenComparing(s -> s.getCreatedAt() != null ? s.getCreatedAt() : java.time.LocalDateTime.MIN))
                .toList();

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardResponse> rows = new ArrayList<>();
        for (Submission s : sorted) {
            HackathonEvent event = s.getMatrix() != null && s.getMatrix().getRound() != null
                    ? s.getMatrix().getRound().getEvent() : null;
            rows.add(LeaderboardResponse.builder()
                    .id(s.getId())
                    .rank(rank.getAndIncrement())
                    .eventId(event != null ? event.getId() : null)
                    .eventName(event != null ? event.getName() : "Không rõ")
                    .eventYear(event != null ? event.getYear() : null)
                    .teamName(s.getTeam().getName())
                    .track("Tổng hợp")
                    .score(s.getScore())
                    .members(teamMemberRepository.findByTeamId(s.getTeam().getId()).stream()
                            .map(this::toMemberResponse)
                            .toList())
                    .build());
        }

        return ApiResponse.<List<LeaderboardResponse>>builder()
                .result(rows)
                .build();
    }

    private Long getDefaultEventId() {
        List<HackathonEvent> activeEvents = eventRepository.findByIsActiveTrue();
        if (!activeEvents.isEmpty()) {
            activeEvents.sort((e1, e2) -> e2.getId().compareTo(e1.getId()));
            return activeEvents.get(0).getId();
        }
        List<HackathonEvent> allEvents = eventRepository.findAll();
        if (!allEvents.isEmpty()) {
            allEvents.sort((e1, e2) -> e2.getId().compareTo(e1.getId()));
            return allEvents.get(0).getId();
        }
        return null;
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
