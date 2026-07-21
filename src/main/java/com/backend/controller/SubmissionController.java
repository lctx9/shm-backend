package com.backend.controller;

import com.backend.dto.request.GradeRequest;
import com.backend.dto.request.SubmissionRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.SubmissionResponse;
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
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<SubmissionResponse> submitWork(@RequestBody SubmissionRequest request) {
        return ApiResponse.<SubmissionResponse>builder()
                .result(submissionService.submitWork(request))
                .build();
    }

    // THÊM MỚI: Kiểm tra xem đã nộp bài chưa
    @GetMapping("/my-submission")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<SubmissionResponse>> getMySubmission() {
        return ApiResponse.<List<SubmissionResponse>>builder()
                .result(submissionService.getMySubmissions())
                .build();
    }

    // THÊM MỚI: Cập nhật bài dự thi
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<SubmissionResponse> updateSubmission(@PathVariable Long id, @RequestBody SubmissionRequest request) {
        return ApiResponse.<SubmissionResponse>builder()
                .result(submissionService.updateSubmission(id, request))
                .build();
    }

    // 1. API Lấy toàn bộ bài nộp cho Giám khảo xem
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'JUDGE', 'MENTOR', 'ADMIN', 'COORDINATOR')")
    public ApiResponse<List<SubmissionResponse>> getAllSubmissions() {
        return ApiResponse.<List<SubmissionResponse>>builder()
                .result(submissionService.getAllSubmissions())
                .build();
    }

}
