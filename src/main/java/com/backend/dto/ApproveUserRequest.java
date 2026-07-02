package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ApproveUserRequest {
    private UUID userId;
    private boolean approved;
    private String reason;
}