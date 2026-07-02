package com.backend.controller;

import com.backend.dto.SubmitScoreRequest;
import com.backend.entity.JudgeScore;
import com.backend.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/judge/evaluation")
@CrossOrigin(origins = "*")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitScores(@RequestParam UUID judgeId, @RequestBody SubmitScoreRequest request) {
        try {
            evaluationService.submitOrUpdateScores(judgeId, request);
            return ResponseEntity.ok("Lưu điểm và ghi log hệ thống thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<List<JudgeScore>> viewScores(@RequestParam UUID submissionId, @RequestParam UUID judgeId) {
        return ResponseEntity.ok(evaluationService.getScoresBySubmissionAndJudge(submissionId, judgeId));
    }
}