package com.backend.service;

import com.backend.dto.request.RuleTemplateRequest;
import com.backend.dto.response.RuleTemplateResponse;
import com.backend.entity.RuleTemplate;
import com.backend.repository.RuleTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleTemplateService {

    private final RuleTemplateRepository ruleTemplateRepository;

    public List<RuleTemplateResponse> getTemplates() {
        return ruleTemplateRepository.findAll().stream()
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RuleTemplateResponse createTemplate(RuleTemplateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên template không được để trống");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung template không được để trống");
        }

        String trimmedName = request.getName().trim();
        Optional<RuleTemplate> existing = ruleTemplateRepository.findByName(trimmedName);
        if (existing.isPresent()) {
            RuleTemplate template = existing.get();
            template.setContent(request.getContent());
            return toResponse(ruleTemplateRepository.save(template));
        } else {
            RuleTemplate template = RuleTemplate.builder()
                    .name(trimmedName)
                    .content(request.getContent())
                    .build();
            return toResponse(ruleTemplateRepository.save(template));
        }
    }

    @Transactional
    public void deleteTemplate(Long id) {
        if (!ruleTemplateRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy template cần xóa");
        }
        ruleTemplateRepository.deleteById(id);
    }

    private RuleTemplateResponse toResponse(RuleTemplate template) {
        return RuleTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .content(template.getContent())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
