package com.backend.controller;

import com.backend.dto.request.ScoreRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    // STAFF chỉ được chấm khi có assignment judge ở matrix tương ứng.
    @PostMapping("/grade")
    @PreAuthorize("hasAnyRole('STAFF', 'JUDGE', 'ADMIN', 'COORDINATOR')")
    public ApiResponse<String> gradeSubmission(@RequestBody @jakarta.validation.Valid ScoreRequest request) {
        scoreService.gradeSubmission(request);
        return ApiResponse.<String>builder()
                .result("Lưu điểm thành công")
                .build();
    }
}
