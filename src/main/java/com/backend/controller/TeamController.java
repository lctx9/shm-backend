package com.backend.controller;

import com.backend.dto.request.JoinPrivateRequest;
import com.backend.dto.request.TeamCreateRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.Team;
import com.backend.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('MEMBER') or hasRole('LEADER')")
    public ApiResponse<Team> createTeam(@RequestBody TeamCreateRequest request) {
        return ApiResponse.<Team>builder()
                .result(teamService.createTeam(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<Team>> getAllTeams() {
        return ApiResponse.<List<Team>>builder()
                .result(teamService.getAllTeams())
                .build();
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasAnyRole('LEADER', 'MEMBER')")
    public ApiResponse<Team> getMyTeam() {
        return ApiResponse.<Team>builder()
                .result(teamService.getMyTeam())
                .build();
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    @PreAuthorize("hasRole('LEADER')")
    public ApiResponse<String> removeMember(@PathVariable Long teamId, @PathVariable Long memberId) {
        teamService.removeMember(teamId, memberId);
        return ApiResponse.<String>builder()
                .result("Xóa thành viên thành công")
                .build();
    }

    // ==========================================
    // CÁC API XỬ LÝ GIA NHẬP ĐỘI (ĐÃ UPDATE CHUẨN)
    // ==========================================

    // 1. Xin gia nhập đội PUBLIC (Gửi yêu cầu chờ duyệt)
    @PostMapping("/{teamId}/join-request")
    @PreAuthorize("hasAnyRole('MEMBER', 'LEADER')") // Đổi sang hasAnyRole để dễ dàng test chuyển đội
    public ApiResponse<String> joinPublicTeam(@PathVariable Long teamId) {
        teamService.requestToJoinPublicTeam(teamId);
        return ApiResponse.<String>builder()
                .result("Đã gửi yêu cầu gia nhập thành công. Đang chờ Leader duyệt.")
                .build();
    }

    // 2. Gia nhập đội PRIVATE (Cần mật khẩu)
    @PostMapping("/{teamId}/join-private")
    @PreAuthorize("hasAnyRole('MEMBER', 'LEADER')") // Đổi sang hasAnyRole để dễ dàng test chuyển đội
    public ApiResponse<String> joinPrivateTeam(@PathVariable Long teamId, @RequestBody JoinPrivateRequest request) {
        teamService.joinPrivateTeam(teamId, request.getPassword());
        return ApiResponse.<String>builder()
                .result("Gia nhập đội thành công!")
                .build();
    }
}