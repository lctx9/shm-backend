package com.backend.repository;

import com.backend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    boolean existsByName(String name);

    // 1. Lấy danh sách đội của 1 hạng mục (track)
    List<Team> findByTrackId(UUID trackId);

    // 2. Lấy danh sách đội của 1 giải đấu (event) - thông qua track
    List<Team> findByTrackEventId(UUID eventId);
}