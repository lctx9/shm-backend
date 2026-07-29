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
    @PreAuthorize("hasAnyRole('STAFF', 'JUDGE')")
    public ApiResponse<String> gradeSubmission(@RequestBody @jakarta.validation.Valid ScoreRequest request) {
        scoreService.gradeSubmission(request);
        return ApiResponse.<String>builder()
                .result("Lưu điểm thành công")
                .build();
    }

    @PostMapping("/matrix/{matrixId}/extend-grading")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<String> extendGradingTime(
            @PathVariable Long matrixId,
            @RequestParam(defaultValue = "5") int extraMinutes) {
        scoreService.extendGradingTime(matrixId, extraMinutes);
        return ApiResponse.<String>builder()
                .result("Gia hạn thời gian chấm bài thành công thêm " + extraMinutes + " phút")
                .build();
    }
}
