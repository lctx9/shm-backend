package com.backend.controller;

import com.backend.dto.request.SubmissionRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.Submission;
import com.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    // Chỉ có LEADER mới được nộp bài
    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    public ApiResponse<Submission> submitWork(@RequestBody SubmissionRequest request) {
        return ApiResponse.<Submission>builder()
                .result(submissionService.submitWork(request))
                .build();
    }
}