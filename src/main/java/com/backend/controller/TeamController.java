package com.backend.controller;

import com.backend.dto.request.JoinPrivateRequest;
import com.backend.dto.request.TeamCreateRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.TeamJoinRequestResponse;
import com.backend.dto.response.TeamResponse;
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
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<TeamResponse> createTeam(@RequestBody TeamCreateRequest request) {
        return ApiResponse.<TeamResponse>builder()
                .result(teamService.createTeam(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<TeamResponse>> getAllTeams() {
        return ApiResponse.<List<TeamResponse>>builder()
                .result(teamService.getAllTeams())
                .build();
    }

    @GetMapping("/my-team")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<TeamResponse>> getMyTeam() {
        return ApiResponse.<List<TeamResponse>>builder()
                .result(teamService.getMyTeams())
                .build();
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> removeMember(@PathVariable Long teamId, @PathVariable Long memberId) {
        teamService.removeMember(teamId, memberId);
        return ApiResponse.<String>builder()
                .result("Xóa thành viên thành công")
                .build();
    }

    @PostMapping("/{teamId}/invite")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<TeamResponse> inviteMember(@PathVariable Long teamId, @RequestBody java.util.Map<String, String> body) {
        return ApiResponse.<TeamResponse>builder()
                .result(teamService.inviteMemberByEmail(teamId, body.get("email")))
                .build();
    }

    @PutMapping("/{teamId}/leader/{memberId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<TeamResponse> transferLeader(@PathVariable Long teamId, @PathVariable Long memberId) {
        return ApiResponse.<TeamResponse>builder()
                .result(teamService.transferLeader(teamId, memberId))
                .build();
    }

    @GetMapping("/{teamId}/join-requests")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<TeamJoinRequestResponse>> getJoinRequests(@PathVariable Long teamId) {
        return ApiResponse.<List<TeamJoinRequestResponse>>builder()
                .result(teamService.getPendingJoinRequests(teamId))
                .build();
    }

    @PostMapping("/{teamId}/join-requests/{requestId}/approve")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<TeamResponse> approveJoinRequest(@PathVariable Long teamId, @PathVariable Long requestId) {
        return ApiResponse.<TeamResponse>builder()
                .result(teamService.approveJoinRequest(teamId, requestId))
                .build();
    }

    @PostMapping("/{teamId}/join-requests/{requestId}/reject")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> rejectJoinRequest(@PathVariable Long teamId, @PathVariable Long requestId) {
        teamService.rejectJoinRequest(teamId, requestId);
        return ApiResponse.<String>builder()
                .result("Đã từ chối yêu cầu tham gia")
                .build();
    }

    // ==========================================
    // CÁC API XỬ LÝ GIA NHẬP ĐỘI (ĐÃ UPDATE CHUẨN)
    // ==========================================

    // 1. Xin gia nhập đội PUBLIC (Gửi yêu cầu chờ duyệt)
    @PostMapping("/{teamId}/join-request")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> joinPublicTeam(@PathVariable Long teamId) {
        teamService.requestToJoinPublicTeam(teamId);
        return ApiResponse.<String>builder()
                .result("Đã gửi yêu cầu gia nhập thành công. Đang chờ Leader duyệt.")
                .build();
    }

    // 2. Gia nhập đội PRIVATE (Cần mật khẩu)
    @PostMapping("/{teamId}/join-private")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> joinPrivateTeam(@PathVariable Long teamId, @RequestBody JoinPrivateRequest request) {
        teamService.joinPrivateTeam(teamId, request.getPassword());
        return ApiResponse.<String>builder()
                .result("Gia nhập đội thành công!")
                .build();
    }

    // 3. Rời khỏi đội
    @PostMapping("/{teamId}/leave")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> leaveTeam(@PathVariable Long teamId) {
        teamService.leaveTeam(teamId);
        return ApiResponse.<String>builder()
                .result("Rời đội thành công!")
                .build();
    }
}

