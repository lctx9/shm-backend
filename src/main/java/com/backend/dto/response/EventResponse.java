package com.backend.dto.response;

import com.backend.entity.enums.Season;
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
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private Season season;
    private int year;
    private LocalDateTime regStartDate;
    private LocalDateTime regEndDate;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private LocalDateTime defaultSubmissionDeadline;
    private Integer roundCount;
    private boolean structureInitialized;
    private boolean active;
    private boolean resultsPublished;
    private String submissionFormSchema;
    private String competitionRules;
    private String ruleDocumentUrl;
    private long teamCount;
    private List<TrackResponse> tracks;
    private List<RoundResponse> rounds;
    private List<MatrixResponse> matrices;
}
