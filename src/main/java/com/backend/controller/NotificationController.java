package com.backend.controller;

import com.backend.dto.NotificationResponse;
import com.backend.dto.SendNotificationRequest;
import com.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // ==========================================
    // 1. USER: XEM DANH SÁCH THÔNG BÁO CỦA TÔI
    // ==========================================
    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(@RequestHeader("X-User-Id") UUID userId) {
        try {
            List<NotificationResponse> notifications = notificationService.getMyNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 2. USER: ĐÁNH DẤU THÔNG BÁO LÀ ĐÃ ĐỌC
    // ==========================================
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID notificationId) {
        try {
            notificationService.markAsRead(notificationId, userId);
            return ResponseEntity.ok("Đã đánh dấu thông báo là đã đọc!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 3. USER: ĐẾM SỐ THÔNG BÁO CHƯA ĐỌC
    // ==========================================
    @GetMapping("/unread-count")
    public ResponseEntity<?> countUnread(@RequestHeader("X-User-Id") UUID userId) {
        try {
            long count = notificationService.countUnreadNotifications(userId);
            return ResponseEntity.ok(count);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 4. COORDINATOR: GỬI THÔNG BÁO (MỚI)
    // ==========================================
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestHeader("X-User-Id") UUID senderId, @RequestBody SendNotificationRequest request) {
        try {
            String message = notificationService.sendNotification(senderId, request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}