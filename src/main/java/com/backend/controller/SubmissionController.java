package com.backend.controller;

import com.backend.dto.request.GradeRequest;
import com.backend.dto.request.SubmissionRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.Submission;
import com.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*") // Đừng quên mở CORS
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    public ApiResponse<Submission> submitWork(@RequestBody SubmissionRequest request) {
        return ApiResponse.<Submission>builder()
                .result(submissionService.submitWork(request))
                .build();
    }

    // THÊM MỚI: Kiểm tra xem đã nộp bài chưa
    @GetMapping("/my-submission")
    @PreAuthorize("hasAnyRole('LEADER', 'MEMBER')")
    public ApiResponse<Submission> getMySubmission() {
        return ApiResponse.<Submission>builder()
                .result(submissionService.getMySubmission())
                .build();
    }

    // THÊM MỚI: Cập nhật bài dự thi
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    public ApiResponse<Submission> updateSubmission(@PathVariable Long id, @RequestBody SubmissionRequest request) {
        return ApiResponse.<Submission>builder()
                .result(submissionService.updateSubmission(id, request))
                .build();
    }

    // 1. API Lấy toàn bộ bài nộp cho Giám khảo xem
    @GetMapping
    @PreAuthorize("hasAnyRole('JUDGE', 'ADMIN', 'COORDINATOR')")
    public ApiResponse<List<Submission>> getAllSubmissions() {
        return ApiResponse.<List<Submission>>builder()
                .result(submissionService.getAllSubmissions())
                .build();
    }

    // 2. API Chấm điểm
    @PutMapping("/{id}/grade")
    @PreAuthorize("hasRole('JUDGE')")
    public ApiResponse<Submission> gradeSubmission(@PathVariable Long id, @RequestBody GradeRequest request) {
        return ApiResponse.<Submission>builder()
                .result(submissionService.gradeSubmission(id, request))
                .build();
    }
}