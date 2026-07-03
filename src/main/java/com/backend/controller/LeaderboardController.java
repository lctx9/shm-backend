package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.LeaderboardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @GetMapping
    // Thêm dòng này để tất cả các Role sau khi Login đều có quyền xem bảng xếp hạng
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR', 'LEADER', 'MEMBER', 'JUDGE')")
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard() {
        return ApiResponse.<List<LeaderboardResponse>>builder()
                .result(new ArrayList<>())
                .build();
    }
}