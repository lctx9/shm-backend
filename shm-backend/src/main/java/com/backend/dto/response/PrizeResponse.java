package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizeResponse {
    private Long id;
    private String name;
    private String description;
    private Long eventId;
    private String eventName;
    private Long teamId;
    private String teamName;
}
