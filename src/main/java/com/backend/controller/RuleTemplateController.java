package com.backend.controller;

import com.backend.dto.request.RuleTemplateRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.RuleTemplateResponse;
import com.backend.service.RuleTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule-templates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RuleTemplateController {

    private final RuleTemplateService ruleTemplateService;

    @GetMapping
    public ApiResponse<List<RuleTemplateResponse>> getTemplates() {
        return ApiResponse.<List<RuleTemplateResponse>>builder()
                .result(ruleTemplateService.getTemplates())
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<RuleTemplateResponse> createTemplate(@RequestBody RuleTemplateRequest request) {
        return ApiResponse.<RuleTemplateResponse>builder()
                .result(ruleTemplateService.createTemplate(request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<String> deleteTemplate(@PathVariable Long id) {
        ruleTemplateService.deleteTemplate(id);
        return ApiResponse.<String>builder()
                .result("Đã xoá mẫu thể lệ")
                .build();
    }
}
