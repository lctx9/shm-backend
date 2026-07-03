package com.backend.service;

import com.backend.dto.NotificationResponse;
import com.backend.dto.SendNotificationRequest;
import com.backend.entity.Notification;
import com.backend.entity.User;
import com.backend.repository.NotificationRepository;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // 1. LUỒNG USER: XEM DANH SÁCH THÔNG BÁO CỦA TÔI
    // ==========================================
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng!"));

        List<Notification> specificNotifications = notificationRepository.findByReceiverId(userId);
        List<Notification> globalNotifications = notificationRepository.findByReceiverIsNull();

        List<Notification> allNotifications = new ArrayList<>();
        allNotifications.addAll(specificNotifications);
        allNotifications.addAll(globalNotifications);

        allNotifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());

        List<NotificationResponse> responses = new ArrayList<>();
        for (Notification n : allNotifications) {
            NotificationResponse res = new NotificationResponse();
            res.setId(n.getId());
            res.setTitle(n.getTitle());
            res.setContent(n.getContent());
            res.setRead(n.isRead());
            res.setCreatedAt(n.getCreatedAt());
            responses.add(res);
        }
        return responses;
    }

    // ==========================================
    // 2. LUỒNG USER: ĐÁNH DẤU ĐÃ ĐỌC
    // ==========================================
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo!"));

        boolean isOwner = (notification.getReceiver() != null && notification.getReceiver().getId().equals(userId))
                || (notification.getReceiver() == null);

        if (!isOwner) {
            throw new RuntimeException("Bạn không có quyền thao tác trên thông báo này!");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    // ==========================================
    // 3. LUỒNG USER: ĐẾM SỐ THÔNG BÁO CHƯA ĐỌC
    // ==========================================
    public long countUnreadNotifications(UUID userId) {
        long unreadSpecific = notificationRepository.countByReceiverIdAndIsReadFalse(userId);
        long unreadGlobal = notificationRepository.countByReceiverIsNullAndIsReadFalse();
        return unreadSpecific + unreadGlobal;
    }

    // ==========================================
    // 4. LUỒNG COORDINATOR: GỬI THÔNG BÁO (MỚI)
    // ==========================================
    @Transactional
    public String sendNotification(UUID senderId, SendNotificationRequest request) {
        // Validate dữ liệu đầu vào (Style thủ công giống nhóm)
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Tiêu đề thông báo không được để trống!");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RuntimeException("Nội dung thông báo không được để trống!");
        }

        // Lấy thông tin người gửi
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người gửi!"));

        // Khởi tạo thông báo
        Notification notification = Notification.builder()
                .sender(sender)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        // Logic gửi riêng hoặc gửi tất cả
        if (request.getReceiverId() != null) {
            // Gửi riêng cho 1 người
            User receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người nhận!"));
            notification.setReceiver(receiver);
        } else {
            // Gửi cho tất cả (receiver = null)
            notification.setReceiver(null);
        }

        notificationRepository.save(notification);
        return "Gửi thông báo thành công!";
    }
}