package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.AuditLogResponse;
import com.backend.entity.AuditLog;
import com.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<List<AuditLogResponse>> getAuditLogs() {
        return ApiResponse.<List<AuditLogResponse>>builder()
                .result(auditLogRepository.findAll().stream().map(this::toResponse).toList())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        String teamName = null;
        if (log.getScore() != null && log.getScore().getSubmission() != null && log.getScore().getSubmission().getTeam() != null) {
            teamName = log.getScore().getSubmission().getTeam().getName();
        }

        return AuditLogResponse.builder()
                .id(log.getId())
                .judgeName(log.getJudge() == null ? null : log.getJudge().getFullName())
                .judgeEmail(log.getJudge() == null ? null : log.getJudge().getEmail())
                .teamName(teamName)
                .oldScore(log.getOldScore())
                .newScore(log.getNewScore())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
