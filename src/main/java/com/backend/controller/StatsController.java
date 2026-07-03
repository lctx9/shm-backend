package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.DashboardStatsResponse;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatsController {

    private final HackathonEventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;

    @GetMapping
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        // Gọi thẳng các hàm đếm (count) từ Cơ sở dữ liệu
        long activeEventsCount = eventRepository.countByIsActiveTrue();
        long totalTeamsCount = teamRepository.count(); // Hàm count() có sẵn
        long pendingSubmissionsCount = submissionRepository.countByIsGradedFalse();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .activeEvents(activeEventsCount)
                .totalTeams(totalTeamsCount)
                .pendingSubmissions(pendingSubmissionsCount)
                .build();

        return ApiResponse.<DashboardStatsResponse>builder()
                .result(stats)
                .build();
    }
}