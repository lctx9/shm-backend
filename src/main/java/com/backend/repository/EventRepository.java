package com.backend.repository;

import com.backend.entity.HackathonEvent;
import com.backend.entity.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<HackathonEvent, UUID> {

    // Lấy danh sách giải đấu theo status (để lọc REGISTRATION_OPEN, ONGOING, COMPLETED)
    List<HackathonEvent> findByStatus(EventStatus status);
}