package com.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageResponse {
    private UUID id;
    private UUID senderId;
    private String senderName; // Tên người gửi để Frontend hiển thị luôn, không cần gọi API User riêng
    private String content;
    private LocalDateTime createdAt;
}