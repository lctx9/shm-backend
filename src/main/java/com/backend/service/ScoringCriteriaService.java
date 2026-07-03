package com.backend.service;

import com.backend.dto.EventCriteriaCustomRequest;
import com.backend.dto.TemplateCriteriaRequest;
import com.backend.entity.Round;
import com.backend.entity.ScoringCriteriaTemplate;
import com.backend.repository.RoundRepository;
import com.backend.repository.ScoringCriteriaTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScoringCriteriaService {

    @Autowired
    private ScoringCriteriaTemplateRepository templateRepository;

    @Autowired
    private RoundRepository roundRepository;

    // ================= P1: QUẢN LÝ TIÊU CHÍ MẪU HỆ THỐNG (Không gắn Round) =================

    @Transactional
    public ScoringCriteriaTemplate createSystemTemplate(TemplateCriteriaRequest request) {
        ScoringCriteriaTemplate template = ScoringCriteriaTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxScore(request.getMaxScore())
                .weight(request.getDefaultWeight() != null ? request.getDefaultWeight() : 1.0)
                .round(null) // Cấu hình mẫu dùng chung nên chưa gán vào Round nào
                .isActive(true)
                .build();
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<ScoringCriteriaTemplate> getAllSystemTemplates() {
        // Lấy các tiêu chí mẫu dùng chung của hệ thống (những bản ghi có round_id là null)
        return templateRepository.findAll().stream()
                .filter(t -> t.getRound() == null)
                .collect(Collectors.toList());
    }

    // ================= P2: KẾ THỪA TEMPLATE VÀO VÒNG THI (ROUND) =================

    @Transactional
    public List<ScoringCriteriaTemplate> inheritFromTemplateToRound(UUID roundId, List<UUID> templateIds) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi (Round) yêu cầu!"));

        List<ScoringCriteriaTemplate> systemTemplates = templateRepository.findAllById(templateIds);

        // Kế thừa bằng cách nhân bản (clone) tiêu chí mẫu sang cho Round cụ thể
        List<ScoringCriteriaTemplate> roundCriteriaList = systemTemplates.stream().map(template -> {
            // Tránh clone trùng lặp nếu tiêu chí này đã được gán cho Round rồi
            boolean exists = templateRepository.findAll().stream()
                    .anyMatch(t -> t.getRound() != null
                            && t.getRound().getId().equals(roundId)
                            && t.getName().equalsIgnoreCase(template.getName()));

            if (exists) return null;

            return ScoringCriteriaTemplate.builder()
                    .name(template.getName())
                    .description(template.getDescription())
                    .maxScore(template.getMaxScore())
                    .weight(template.getWeight()) // Giữ nguyên trọng số mặc định
                    .round(round) // Gán trực tiếp vào Round hiện tại
                    .isActive(true)
                    .build();
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        return templateRepository.saveAll(roundCriteriaList);
    }

    // ================= P3: THÊM / BỎ / ĐIỀU CHỈNH TIÊU CHÍ TRONG VÒNG THI =================

    // Thêm mới thủ công một tiêu chí riêng biệt cho Vòng thi (Không qua template)
    @Transactional
    public ScoringCriteriaTemplate createCustomCriteriaForRound(UUID roundId, EventCriteriaCustomRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi (Round) yêu cầu!"));

        ScoringCriteriaTemplate customCriteria = ScoringCriteriaTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxScore(request.getMaxScore())
                .weight(request.getWeight() != null ? request.getWeight() : 1.0)
                .round(round)
                .isActive(true)
                .build();

        return templateRepository.save(customCriteria);
    }

    // Cập nhật chi tiết hoặc điều chỉnh trọng số (weight) của một tiêu chí thuộc Vòng thi
    @Transactional
    public ScoringCriteriaTemplate updateRoundCriteria(UUID criteriaId, EventCriteriaCustomRequest request) {
        ScoringCriteriaTemplate criteria = templateRepository.findById(criteriaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tiêu chí chấm điểm yêu cầu!"));

        criteria.setName(request.getName());
        criteria.setDescription(request.getDescription());
        criteria.setMaxScore(request.getMaxScore());
        criteria.setWeight(request.getWeight());
        criteria.setActive(request.isActive());

        return templateRepository.save(criteria);
    }

    // Loại bỏ (Xóa hẳn hoặc tắt kích hoạt) tiêu chí ra khỏi Vòng thi
    @Transactional
    public void deleteRoundCriteria(UUID criteriaId) {
        ScoringCriteriaTemplate criteria = templateRepository.findById(criteriaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tiêu chí chấm điểm để xóa!"));

        try {
            templateRepository.delete(criteria);
        } catch (Exception e) {
            // Fallback sang soft-delete nếu đã có bảng điểm tham chiếu không thể xóa cứng
            criteria.setActive(false);
            templateRepository.save(criteria);
        }
    }

    // Lấy danh sách toàn bộ tiêu chí đang áp dụng cho một Vòng thi
    @Transactional(readOnly = true)
    public List<ScoringCriteriaTemplate> getCriteriaByRound(UUID roundId) {
        return templateRepository.findAll().stream()
                .filter(t -> t.getRound() != null && t.getRound().getId().equals(roundId) && t.isActive())
                .collect(Collectors.toList());
    }
}