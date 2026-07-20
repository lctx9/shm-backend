package com.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

@Data
public class ScoreRequest {
    private Long submissionId;

    @DecimalMin(value = "0.0", message = "Điểm phải lớn hơn hoặc bằng 0.0")
    @DecimalMax(value = "100.0", message = "Điểm phải nhỏ hơn hoặc bằng 100.0")
    private Double scoreValue;

    private String criteriaScoresJson;
    private String comment;
    private String editReason; // Bắt buộc nếu là sửa điểm (để lưu Audit Log)
}
