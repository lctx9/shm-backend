package com.backend.controller;

import com.backend.dto.EventCriteriaCustomRequest;
import com.backend.dto.TemplateCriteriaRequest;
import com.backend.entity.ScoringCriteriaTemplate;
import com.backend.service.ScoringCriteriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scoring-criteria")
public class ScoringCriteriaController {

    @Autowired
    private ScoringCriteriaService criteriaService;

    // 1. Tạo Template mẫu dùng chung hệ thống
    @PostMapping("/templates")
    public ResponseEntity<?> createSystemTemplate(@RequestBody TemplateCriteriaRequest request) {
        try {
            return ResponseEntity.ok(criteriaService.createSystemTemplate(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Lấy danh sách các Template mẫu dùng chung hệ thống
    @GetMapping("/templates")
    public ResponseEntity<List<ScoringCriteriaTemplate>> getAllSystemTemplates() {
        return ResponseEntity.ok(criteriaService.getAllSystemTemplates());
    }

    // 3. Kế thừa: Nhân bản tiêu chí mẫu hệ thống vào một Vòng thi (Round) cụ thể
    @PostMapping("/rounds/{roundId}/inherit")
    public ResponseEntity<?> inheritCriteriaToRound(
            @PathVariable UUID roundId,
            @RequestBody List<UUID> templateIds) {
        try {
            return ResponseEntity.ok(criteriaService.inheritFromTemplateToRound(roundId, templateIds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. Tùy biến: Tạo tiêu chí riêng biệt trực tiếp trong Vòng thi (Round)
    @PostMapping("/rounds/{roundId}/custom")
    public ResponseEntity<?> addCustomCriteriaToRound(
            @PathVariable UUID roundId,
            @RequestBody EventCriteriaCustomRequest request) {
        try {
            return ResponseEntity.ok(criteriaService.createCustomCriteriaForRound(roundId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 5. Tùy biến: Điều chỉnh thông tin, điểm tối đa, trọng số (weight) trong Vòng thi
    @PutMapping("/{criteriaId}")
    public ResponseEntity<?> updateRoundCriteriaDetails(
            @PathVariable UUID criteriaId,
            @RequestBody EventCriteriaCustomRequest request) {
        try {
            return ResponseEntity.ok(criteriaService.updateRoundCriteria(criteriaId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. Tùy biến: Loại bỏ tiêu chí ra khỏi Vòng thi
    @DeleteMapping("/{criteriaId}")
    public ResponseEntity<?> removeCriteriaFromRound(@PathVariable UUID criteriaId) {
        try {
            criteriaService.deleteRoundCriteria(criteriaId);
            return ResponseEntity.ok("Xử lý xóa/tắt kích hoạt tiêu chí thành công khỏi vòng thi.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7. Truy vấn: Xem bộ tiêu chí chấm điểm hiện tại của một Vòng thi (Round)
    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<List<ScoringCriteriaTemplate>> getCriteriaOfRound(@PathVariable UUID roundId) {
        return ResponseEntity.ok(criteriaService.getCriteriaByRound(roundId));
    }
}