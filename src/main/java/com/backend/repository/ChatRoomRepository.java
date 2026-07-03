package com.backend.repository;

import com.backend.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    // Tìm phòng chat của một đội cụ thể dựa vào team_id
    Optional<ChatRoom> findByTeamId(UUID teamId);
}