package com.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeamSummaryResponse {
    private UUID id;
    private String name;
    private String trackName;      // Tên hạng mục (VD: "AI/ML Track")
    private UUID trackId;          // ID của track (để navigate)
    private Integer memberCount;   // Số lượng thành viên (3-5 người)
    private String visibility;     // PUBLIC hoặc PRIVATE
    private String status;         // APPROVED, ELIMINATED, DISQUALIFIED
    private LocalDateTime createdAt;
}