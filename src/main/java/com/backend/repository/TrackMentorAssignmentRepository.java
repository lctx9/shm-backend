package com.backend.repository;

import com.backend.entity.TrackMentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrackMentorAssignmentRepository extends JpaRepository<TrackMentorAssignment, UUID> {

    // Tìm tất cả các mentor được phân công cho một Track cụ thể
    List<TrackMentorAssignment> findByTrackId(UUID trackId);
}