package com.backend.controller;

import com.backend.dto.request.ScoreRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.Score;
import com.backend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    // Chỉ có JUDGE mới được chấm điểm
    @PostMapping("/grade")
    @PreAuthorize("hasRole('JUDGE')")
    public ApiResponse<Score> gradeSubmission(@RequestBody ScoreRequest request) {
        return ApiResponse.<Score>builder()
                .result(scoreService.gradeSubmission(request))
                .build();
    }
}