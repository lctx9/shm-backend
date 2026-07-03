package com.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class EventResponse {
    private UUID id;
    private String name;
    private String season;
    private String academicYear;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // DRAFT, REGISTRATION_OPEN, ONGOING, COMPLETED, CANCELLED
}