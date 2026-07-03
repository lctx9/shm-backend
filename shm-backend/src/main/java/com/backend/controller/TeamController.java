package com.backend.controller;

import com.backend.dto.request.TeamCreateRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.Team;
import com.backend.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    // Các User có Role MEMBER (vừa đăng ký xong) có thể tạo đội để trở thành LEADER
    @PostMapping("/create")
    @PreAuthorize("hasRole('MEMBER') or hasRole('LEADER')")
    public ApiResponse<Team> createTeam(@RequestBody TeamCreateRequest request) {
        return ApiResponse.<Team>builder()
                .result(teamService.createTeam(request))
                .build();
    }
}