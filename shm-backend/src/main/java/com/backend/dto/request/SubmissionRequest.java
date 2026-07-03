package com.backend.dto.request;

import lombok.Data;

@Data
public class SubmissionRequest {
    private Long matrixId; // Nộp cho vòng nào / track nào
    private Long teamId;
    private String fileUrl; // Link driver, github hoặc file đính kèm
}