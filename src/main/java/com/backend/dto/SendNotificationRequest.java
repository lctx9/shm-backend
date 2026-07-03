package com.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SendNotificationRequest {
    private String title;
    private String content;

    // Nếu truyền ID user cụ thể -> Gửi riêng cho người đó.
    // Nếu để null -> Gửi thông báo cho TẤT CẢ MỌI NGƯỜI.
    private UUID receiverId;
}