package com.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoundResponse {
    private UUID id;
    private String name;
    private String roundType; // QUALIFIER, SEMIFINAL, FINAL
    private Integer roundOrder;
    private LocalDateTime submissionDeadline;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}