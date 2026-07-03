package com.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class EventDetailResponse {
    private UUID id;
    private String name;
    private String season;
    private String academicYear;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    // Nhúng luôn danh sách Hạng mục và Vòng thi vào đây cho tiện
    private List<TrackResponse> tracks;
    private List<RoundResponse> rounds;
}