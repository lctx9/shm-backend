package com.backend.repository;

import com.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 1. Tìm thông báo gửi riêng cho 1 user cụ thể
    List<Notification> findByReceiverId(UUID receiverId);

    // 2. Tìm thông báo gửi cho TẤT CẢ MỌI NGƯỜI (receiver là null)
    List<Notification> findByReceiverIsNull();

    // 3. Đếm số thông báo chưa đọc gửi riêng cho user
    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    // 4. Đếm số thông báo chung chưa đọc (gửi cho tất cả)
    long countByReceiverIsNullAndIsReadFalse();
}