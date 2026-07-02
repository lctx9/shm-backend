package com.backend.controller;

import com.backend.entity.AuditLog;
import com.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coordinator/audit-log") // Chỉ dành cho Coordinator/Admin tra cứu
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Xem lịch sử theo Event
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<AuditLog>> getLogsByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(auditLogRepository.findByEventIdOrderByTimestampDesc(eventId));
    }

    // Xem lịch sử theo Team
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<AuditLog>> getLogsByTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(auditLogRepository.findByTeamIdOrderByTimestampDesc(teamId));
    }

    // Xem lịch sử theo Submission
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<AuditLog>> getLogsBySubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(auditLogRepository.findBySubmissionIdOrderByTimestampDesc(submissionId));
    }
}