package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String judgeName;
    private String judgeEmail;
    private String teamName;
    private Double oldScore;
    private Double newScore;
    private String reason;
    private LocalDateTime createdAt;
}
