package com.backend.dto.response;

import com.backend.entity.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String body;
    private RoleType targetRole;
    private String recipientEmail;
    private String senderEmail;
    private LocalDateTime createdAt;
}
