package com.backend.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long teamId;
    private String content;
}
