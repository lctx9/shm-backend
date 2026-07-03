package com.backend.repository;

import com.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Lấy toàn bộ lịch sử tin nhắn của một phòng chat, sắp xếp từ CŨ -> MỚI (ASC)
    // (Để khi load lên giao diện, tin nhắn cũ nằm trên, tin nhắn mới nằm dưới)
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(UUID chatRoomId);
}