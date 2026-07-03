package com.backend.dto.request;

import lombok.Data;

@Data
public class PrizeRequest {
    private String name;
    private String description;
    private Long teamId;
}
