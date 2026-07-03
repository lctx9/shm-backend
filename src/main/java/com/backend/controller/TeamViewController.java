package com.backend.controller;

import com.backend.dto.TeamSummaryResponse;
import com.backend.service.TeamViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class TeamViewController {

    @Autowired
    private TeamViewService teamViewService;

    // ==========================================
    // 1. LẤY DANH SÁCH ĐỘI CỦA MỘT GIẢI ĐẤU (EVENT)
    // Dùng cho trang "Sảnh chờ đội" khi User chọn giải đấu
    // ==========================================
    @GetMapping("/api/events/{eventId}/teams")
    public ResponseEntity<?> getTeamsByEvent(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID eventId) {
        try {
            List<TeamSummaryResponse> teams = teamViewService.getTeamsByEvent(eventId);
            return ResponseEntity.ok(teams);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 2. LẤY DANH SÁCH ĐỘI CỦA MỘT HẠNG MỤC (TRACK)
    // Dùng khi User muốn xem chi tiết đội trong 1 hạng mục cụ thể
    // ==========================================
    @GetMapping("/api/tracks/{trackId}/teams")
    public ResponseEntity<?> getTeamsByTrack(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID trackId) {
        try {
            List<TeamSummaryResponse> teams = teamViewService.getTeamsByTrack(trackId);
            return ResponseEntity.ok(teams);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}