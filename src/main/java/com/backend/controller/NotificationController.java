package com.backend.controller;

import com.backend.dto.request.NotificationRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.NotificationResponse;
import com.backend.entity.Notification;
import com.backend.entity.User;
import com.backend.repository.NotificationRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        User currentUser = getCurrentUser();
        List<NotificationResponse> rows = notificationRepository.findAll().stream()
                .filter(item -> item.getRecipient() == null || item.getRecipient().getId().equals(currentUser.getId()))
                .filter(item -> item.getTargetRole() == null || item.getTargetRole() == currentUser.getRole())
                .map(this::toResponse)
                .toList();
        return ApiResponse.<List<NotificationResponse>>builder().result(rows).build();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<NotificationResponse> createNotification(@RequestBody NotificationRequest request) {
        User recipient = request.getRecipientId() == null ? null : userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .targetRole(request.getTargetRole())
                .recipient(recipient)
                .sender(getCurrentUser())
                .build();

        return ApiResponse.<NotificationResponse>builder()
                .result(toResponse(notificationRepository.save(notification)))
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .targetRole(notification.getTargetRole())
                .recipientEmail(notification.getRecipient() == null ? null : notification.getRecipient().getEmail())
                .senderEmail(notification.getSender() == null ? null : notification.getSender().getEmail())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
