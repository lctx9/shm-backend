package com.backend.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class MatrixUpdateRequest {
    private String guidelineUrl;
    private LocalDateTime submissionStartDate;
    private LocalDateTime submissionDeadline;
    private Integer gradingDurationMinutes;
    private Integer breakDurationMinutes;
    private String scoringCriteriaJson;
    private Set<Long> mentorIds;
    private Set<Long> judgeIds;
    private Integer topN;
    private Integer durationMinutes;
}
