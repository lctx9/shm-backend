package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SubmissionRequest {
    private UUID roundId;
    private String repoUrl;
    private String demoUrl;
    private String reportUrl;
}