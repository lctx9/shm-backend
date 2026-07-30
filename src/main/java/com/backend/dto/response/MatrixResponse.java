package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixResponse {
    private Long id;
    private Long trackId;
    private String trackName;
    private Long roundId;
    private String roundName;
    private Integer roundOrder;
    private Boolean finalRound;
    private Boolean isPublished;
    private Integer topN;
    private Integer durationMinutes;
    private String guidelineUrl;
    private LocalDateTime submissionStartDate;
    private LocalDateTime submissionDeadline;
    private Integer gradingDurationMinutes;
    private LocalDateTime gradingDeadline;
    private Long gradingRemainingSeconds;
    private Boolean gradingExtensionNotified;
    private Integer breakDurationMinutes;
    private LocalDateTime breakEndTime;
    private Long breakRemainingSeconds;
    private String scoringCriteriaJson;
    private List<PublicStaffResponse> mentors;
    private List<PublicStaffResponse> judges;
}
