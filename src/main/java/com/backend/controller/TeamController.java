package com.backend.controller;

import com.backend.dto.CreateTeamRequest;
import com.backend.dto.TeamDetailsResponse;
import com.backend.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {

    @Autowired private TeamService teamService;

    @PostMapping("/create")
    public ResponseEntity<?> createTeam(@RequestHeader("X-User-Id") UUID userId, @RequestBody CreateTeamRequest request) {
        try {
            return ResponseEntity.ok(teamService.createTeam(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Gửi yêu cầu gia nhập (Có kèm pinCode dạng RequestParam nếu nhóm Private)
    @PostMapping("/{teamId}/join")
    public ResponseEntity<?> joinTeam(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID teamId, @RequestParam(required = false) String pinCode) {
        try {
            return ResponseEntity.ok(teamService.joinTeamRequest(userId, teamId, pinCode));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Trưởng nhóm duyệt yêu cầu xin vào đội (PUBLIC)
    @PutMapping("/requests/{requestId}/handle")
    public ResponseEntity<?> handleRequest(@RequestHeader("X-User-Id") UUID leaderId, @PathVariable UUID requestId, @RequestParam boolean accept) {
        try {
            teamService.handleJoinRequest(leaderId, requestId, accept);
            return ResponseEntity.ok("Xử lý yêu cầu thành viên thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{teamId}/remove")
    public ResponseEntity<?> removeMember(@RequestHeader("X-User-Id") UUID leaderId, @PathVariable UUID teamId, @RequestParam UUID targetUserId) {
        try {
            return ResponseEntity.ok(teamService.removeMember(leaderId, teamId, targetUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{teamId}/transfer-leader")
    public ResponseEntity<?> transferLeader(@RequestHeader("X-User-Id") UUID leaderId, @PathVariable UUID teamId, @RequestParam UUID newLeaderUserId) {
        try {
            return ResponseEntity.ok(teamService.transferLeader(leaderId, teamId, newLeaderUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}