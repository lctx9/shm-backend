package com.backend.dto.request;

import lombok.Data;

@Data
public class GradeRequest {
    private Double score;
    private String feedback;
}