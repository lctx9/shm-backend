package com.backend.controller;

import com.backend.dto.request.NotificationRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.NotificationResponse;
import com.backend.entity.Notification;
import com.backend.entity.NotificationRead;
import com.backend.entity.User;
import com.backend.repository.NotificationRepository;
import com.backend.repository.NotificationReadRepository;
import com.backend.repository.UserRepository;
import com.backend.entity.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        User currentUser = getCurrentUser();
        List<Notification> visible = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> isSenderOrVisible(item, currentUser))
                .toList();
        if (visible.isEmpty()) {
            return ApiResponse.<List<NotificationResponse>>builder().result(List.of()).build();
        }
        Set<Long> readIds = notificationReadRepository
                .findByUserIdAndNotificationIdIn(currentUser.getId(), visible.stream().map(Notification::getId).toList())
                .stream().map(item -> item.getNotification().getId()).collect(Collectors.toSet());
        List<NotificationResponse> rows = visible.stream()
                .map(item -> toResponse(item, readIds.contains(item.getId())))
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
                .result(toResponse(notificationRepository.save(notification), false))
                .build();
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ApiResponse<String> markAsRead(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        if (!isVisibleTo(notification, currentUser)) {
            throw new RuntimeException("Bạn không có quyền xem thông báo này");
        }
        if (!notificationReadRepository.existsByUserIdAndNotificationId(currentUser.getId(), id)) {
            notificationReadRepository.save(NotificationRead.builder().notification(notification).user(currentUser).build());
        }
        return ApiResponse.<String>builder().result("Đã đánh dấu là đã đọc").build();
    }

    @PatchMapping("/read-all")
    @Transactional
    public ApiResponse<String> markAllAsRead() {
        User currentUser = getCurrentUser();
        List<Notification> visible = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> isVisibleTo(item, currentUser))
                .toList();

        if (visible.isEmpty()) {
            return ApiResponse.<String>builder().result("Đã đọc tất cả thông báo").build();
        }

        Set<Long> readIds = notificationReadRepository
                .findByUserIdAndNotificationIdIn(currentUser.getId(), visible.stream().map(Notification::getId).toList())
                .stream().map(item -> item.getNotification().getId()).collect(Collectors.toSet());

        List<NotificationRead> newReads = visible.stream()
                .filter(item -> !readIds.contains(item.getId()))
                .map(item -> NotificationRead.builder().notification(item).user(currentUser).build())
                .toList();

        if (!newReads.isEmpty()) {
            notificationReadRepository.saveAll(newReads);
        }
        return ApiResponse.<String>builder().result("Đã đọc tất cả thông báo").build();
    }

    /**
     * Xóa/Đánh dấu đọc tất cả thông báo hiển thị của người dùng hiện tại mà không làm hỏng dữ liệu liên quan hay gây lỗi FK DB.
     */
    @DeleteMapping("/my")
    @Transactional
    public ApiResponse<String> deleteAllMyNotifications() {
        User currentUser = getCurrentUser();

        List<Notification> visible = notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> isSenderOrVisible(item, currentUser))
                .toList();

        if (visible.isEmpty()) {
            return ApiResponse.<String>builder().result("Đã xóa tất cả thông báo").build();
        }

        Set<Long> readIds = notificationReadRepository
                .findByUserIdAndNotificationIdIn(currentUser.getId(), visible.stream().map(Notification::getId).toList())
                .stream().map(item -> item.getNotification().getId()).collect(Collectors.toSet());

        List<NotificationRead> newReads = visible.stream()
                .filter(item -> !readIds.contains(item.getId()))
                .map(item -> NotificationRead.builder().notification(item).user(currentUser).build())
                .toList();

        if (!newReads.isEmpty()) {
            notificationReadRepository.saveAll(newReads);
        }

        return ApiResponse.<String>builder().result("Đã xóa tất cả thông báo khỏi hộp thư của bạn").build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private NotificationResponse toResponse(Notification notification, boolean read) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .targetRole(notification.getTargetRole())
                .recipientEmail(notification.getRecipient() == null ? null : notification.getRecipient().getEmail())
                .senderEmail(notification.getSender() == null ? null : notification.getSender().getEmail())
                .actionUrl(notification.getActionUrl())
                .read(read)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private boolean isVisibleTo(Notification item, User user) {
        // Chỉ hiển thị cho người nhận (recipient) hoặc người có role phù hợp
        // Không hiển thị cho chính người gửi (sender) — tránh việc người tạo đội thấy lời mời mình đã gửi
        boolean recipientMatches = item.getRecipient() == null || item.getRecipient().getId().equals(user.getId());
        boolean roleMatches = item.getTargetRole() == null || item.getTargetRole() == user.getRole()
                || (isStaffRole(item.getTargetRole()) && isStaffRole(user.getRole()));
        // Nếu notification có recipient cụ thể → chỉ recipient mới thấy, dù sender là ai
        if (item.getRecipient() != null) {
            return item.getRecipient().getId().equals(user.getId());
        }
        // Broadcast (recipient = null): hiển thị theo role
        return roleMatches;
    }

    private boolean isSenderOrVisible(Notification item, User user) {
        return isVisibleTo(item, user);
    }

    private boolean isStaffRole(RoleType role) {
        return role == RoleType.STAFF || role == RoleType.MENTOR || role == RoleType.JUDGE;
    }
}
