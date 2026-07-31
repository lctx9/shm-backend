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
    private final HackathonEventRepository eventRepository;
    private final com.backend.repository.TrackRoundMatrixRepository matrixRepository;
    private final com.backend.repository.PrizeRepository prizeRepository;

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

        // Self-heal: If final round matrices are published, automatically publish event results
        Integer rc = event.getRoundCount();
        List<com.backend.entity.TrackRoundMatrix> finalMatrices = matrixRepository.findByRoundEventId(targetEventId).stream()
                .filter(m -> m.getRound() != null && rc != null && rc == m.getRound().getOrderIndex())
                .toList();

        if (!finalMatrices.isEmpty() && finalMatrices.stream().allMatch(m -> Boolean.TRUE.equals(m.getIsPublished()))) {
            if (!Boolean.TRUE.equals(event.getResultsPublished())) {
                event.setResultsPublished(true);
                eventRepository.save(event);
            }
        }

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
        List<com.backend.entity.Prize> configuredPrizes = prizeRepository.findByEventId(targetEventId);
        int maxLeaderboardCount = (configuredPrizes != null && !configuredPrizes.isEmpty())
                ? configuredPrizes.size()
                : 3;

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

        List<LeaderboardResponse> rows = new ArrayList<>();
        int currentRank = 1;

        for (Submission submission : sortedSubmissions) {
            if (rows.size() >= maxLeaderboardCount) {
                break;
            }
            if (submission.getTeam() == null || teamMemberRepository.countByTeamId(submission.getTeam().getId()) < 3) {
                continue;
            }
            String trackName = submission.getMatrix() != null && submission.getMatrix().getTrack() != null
                    ? submission.getMatrix().getTrack().getName()
                    : "Chung kết";

            int idx = rows.size();
            String customPrizeName = (configuredPrizes != null && idx < configuredPrizes.size())
                    ? configuredPrizes.get(idx).getName()
                    : null;

            rows.add(LeaderboardResponse.builder()
                    .id(submission.getId())
                    .rank(currentRank++)
                    .eventId(event.getId())
                    .eventName(event.getName())
                    .eventYear(event.getYear())
                    .teamName(submission.getTeam() == null ? "Không rõ đội" : submission.getTeam().getName())
                    .track(trackName)
                    .projectName(submission.getFileUrl())
                    .description(submission.getFeedback())
                    .score(submission.getScore())
                    .prizeName(customPrizeName)
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
    public ApiResponse<List<LeaderboardResponse>> getAllLeaderboard() {
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

        List<HackathonEvent> events = eventRepository.findAll();
        List<LeaderboardResponse> allRows = new ArrayList<>();

        for (HackathonEvent event : events) {
            if (!Boolean.TRUE.equals(event.getResultsPublished()) && !showUnpublished) {
                continue;
            }

            List<Submission> submissions = submissionRepository.findFinalRoundGradedSubmissions(event.getId());
            List<com.backend.entity.Prize> configuredPrizes = prizeRepository.findByEventId(event.getId());
            int maxLeaderboardCount = (configuredPrizes != null && !configuredPrizes.isEmpty())
                    ? configuredPrizes.size()
                    : 3;

            List<Submission> sortedSubmissions = submissions.stream()
                    .sorted((s1, s2) -> {
                        double score1 = s1.getScore() != null ? s1.getScore() : 0.0;
                        double score2 = s2.getScore() != null ? s2.getScore() : 0.0;
                        return Double.compare(score2, score1);
                    })
                    .limit(maxLeaderboardCount)
                    .toList();

            for (int i = 0; i < sortedSubmissions.size(); i++) {
                Submission submission = sortedSubmissions.get(i);
                String customPrizeName = (configuredPrizes != null && i < configuredPrizes.size())
                        ? configuredPrizes.get(i).getName()
                        : null;

                allRows.add(LeaderboardResponse.builder()
                        .id(submission.getId())
                        .eventId(event.getId())
                        .eventName(event.getName())
                        .eventYear(event.getYear())
                        .teamName(submission.getTeam() == null ? "Không rõ đội" : submission.getTeam().getName())
                        .track(submission.getMatrix() != null && submission.getMatrix().getTrack() != null
                                ? submission.getMatrix().getTrack().getName()
                                : "Chung kết")
                        .projectName(submission.getFileUrl())
                        .description(submission.getFeedback())
                        .score(submission.getScore())
                        .prizeName(customPrizeName)
                        .members(submission.getTeam() == null ? List.of() : teamMemberRepository.findByTeamId(submission.getTeam().getId()).stream()
                                .map(this::toMemberResponse)
                                .toList())
                        .build());
            }
        }

        allRows.sort((r1, r2) -> {
            double score1 = r1.getScore() != null ? r1.getScore() : 0.0;
            double score2 = r2.getScore() != null ? r2.getScore() : 0.0;
            return Double.compare(score2, score1);
        });

        AtomicInteger rank = new AtomicInteger(1);
        for (LeaderboardResponse row : allRows) {
            row.setRank(rank.getAndIncrement());
        }

        return ApiResponse.<List<LeaderboardResponse>>builder()
                .result(allRows)
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
