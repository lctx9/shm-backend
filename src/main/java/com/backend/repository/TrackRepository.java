package com.backend.repository;

import com.backend.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrackRepository extends JpaRepository<Track, UUID> {

    // Lấy danh sách hạng mục (tracks) của một giải đấu
    List<Track> findByEventId(UUID eventId);
}