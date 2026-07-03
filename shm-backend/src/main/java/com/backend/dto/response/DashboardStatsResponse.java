package com.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long activeEvents;
    private long totalTeams;
    private long pendingSubmissions;
}