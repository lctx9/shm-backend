package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.LeaderboardResponse;
import com.backend.entity.Submission;
import com.backend.repository.SubmissionRepository;
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

    @GetMapping
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard() {
        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardResponse> rows = new ArrayList<>();

        for (Submission submission : submissionRepository.findByIsGradedTrueOrderByScoreDesc()) {
            rows.add(LeaderboardResponse.builder()
                    .id(submission.getId())
                    .rank(rank.getAndIncrement())
                    .teamName(submission.getTeam() == null ? "Không rõ đội" : submission.getTeam().getName())
                    .track(submission.getMatrix() == null ? "Chưa gắn track" : submission.getMatrix().getTrack().getName())
                    .projectName(submission.getFileUrl())
                    .description(submission.getFeedback())
                    .score(submission.getScore())
                    .build());
        }

        return ApiResponse.<List<LeaderboardResponse>>builder()
                .result(rows)
                .build();
    }
}
