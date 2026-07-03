package com.backend.dto.request;

import com.backend.entity.enums.Season;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequest {
    private String name;
    private Season season;
    private int year;
    private LocalDateTime regStartDate;
    private LocalDateTime regEndDate;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private List<String> tracks;
    private Integer roundCount;
    private LocalDateTime submissionDeadline;
}
