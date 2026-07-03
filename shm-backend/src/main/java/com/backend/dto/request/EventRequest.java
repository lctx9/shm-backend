package com.backend.dto.request;

import com.backend.entity.enums.Season;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    private String name;
    private Season season;
    private int year;
    private LocalDateTime regStartDate;
    private LocalDateTime regEndDate;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}