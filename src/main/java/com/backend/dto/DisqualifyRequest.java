package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DisqualifyRequest {
    private UUID submissionId;
    private String reason;
}