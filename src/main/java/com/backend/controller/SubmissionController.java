package com.backend.controller;

import com.backend.dto.DisqualifyRequest;
import com.backend.dto.SubmissionRequest;
import com.backend.entity.Submission;
import com.backend.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    // API 1: Dành cho Team Leader nộp hoặc cập nhật lại bài thi trước deadline
    @PostMapping("/submit")
    public ResponseEntity<?> submitOrUpdateProject(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody SubmissionRequest request) {
        try {
            Submission submission = submissionService.submitOrUpdate(userId, request);
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API 2: Dành cho toàn bộ Team Member hoặc Leader truy cập để kiểm tra trạng thái bài nộp
    @GetMapping("/team-status")
    public ResponseEntity<?> getTeamSubmissionStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID roundId) {
        try {
            Submission submission = submissionService.getTeamSubmission(userId, roundId);
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API 3: Dành cho Coordinator / Ban tổ chức hủy bài nộp vi phạm (Kèm lý do cụ thể)
    @PutMapping("/coordinator/disqualify")
    public ResponseEntity<?> disqualifyProject(
            @RequestHeader("X-User-Id") UUID coordinatorId, // Để phục vụ việc lưu vết phân quyền nếu cần
            @RequestBody DisqualifyRequest request) {
        try {
            Submission submission = submissionService.disqualifySubmission(request);
            return ResponseEntity.ok(submission);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}