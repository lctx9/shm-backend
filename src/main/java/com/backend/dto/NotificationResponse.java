package com.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NotificationResponse {
    private UUID id;
    private String title;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
}