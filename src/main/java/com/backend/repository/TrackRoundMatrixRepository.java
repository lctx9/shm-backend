package com.backend.repository;

import com.backend.entity.TrackRoundMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRoundMatrixRepository extends JpaRepository<TrackRoundMatrix, Long> {
    List<TrackRoundMatrix> findByTrackEventId(Long eventId);
    List<TrackRoundMatrix> findByRoundEventId(Long eventId);
    List<TrackRoundMatrix> findByTrackId(Long trackId);
    long countByTrackEventId(Long eventId);
    long countByRoundEventId(Long eventId);
    java.util.Optional<TrackRoundMatrix> findByTrackIdAndRoundOrderIndex(Long trackId, int orderIndex);
    java.util.Optional<TrackRoundMatrix> findByRoundEventIdAndTrackIsNullAndRoundOrderIndex(Long eventId, int orderIndex);
    List<TrackRoundMatrix> findDistinctByMentorsId(Long userId);
    List<TrackRoundMatrix> findDistinctByJudgesId(Long userId);
}
