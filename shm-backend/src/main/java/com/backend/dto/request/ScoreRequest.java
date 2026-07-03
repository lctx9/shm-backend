package com.backend.dto.request;

import lombok.Data;

@Data
public class ScoreRequest {
    private Long submissionId;
    private Double scoreValue;
    private String comment;
    private String editReason; // Bắt buộc nếu là sửa điểm (để lưu Audit Log)
}