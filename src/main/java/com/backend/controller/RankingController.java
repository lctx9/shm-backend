package com.backend.controller;

import com.backend.entity.TeamRoundRanking;
import com.backend.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coordinator/ranking") // Thuộc vùng bảo mật điều phối viên giống CoordinatorUserController
@CrossOrigin(origins = "*")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    /**
     * API thực hiện tính toán điểm, xếp hạng và xét duyệt thăng chức tự động cho các đội trong Round
     * POST http://localhost:8080/api/coordinator/ranking/execute?roundId=...
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeRanking(@RequestParam UUID roundId) {
        try {
            List<TeamRoundRanking> results = rankingService.calculateAndExecuteRanking(roundId);
            return ResponseEntity.ok("Tính toán tổng điểm có trọng số và xét thăng vòng thànhhh côngggggggg!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API xem danh sách bảng xếp hạng hiện tại của một Round
     * GET http://localhost:8080/api/coordinator/ranking/view?roundId=...
     */
    @GetMapping("/view")
    public ResponseEntity<List<TeamRoundRanking>> getRankingsByRound(@RequestParam UUID roundId) {
        try {
            List<TeamRoundRanking> rankings = rankingService.getRankingsByRound(roundId);
            return ResponseEntity.ok(rankings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}