package com.backend.dto.request;

import com.backend.entity.enums.RoleType;
import lombok.Data;

@Data
public class NotificationRequest {
    private String title;
    private String body;
    private RoleType targetRole;
    private Long recipientId;
}
