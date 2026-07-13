package com.backend.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class MatrixUpdateRequest {
    private String guidelineUrl;
    private LocalDateTime submissionDeadline;
    private String scoringCriteriaJson;
    private Set<Long> mentorIds;
    private Set<Long> judgeIds;
    private Integer topN;
}
