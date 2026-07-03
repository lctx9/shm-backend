package com.backend.repository;

import com.backend.entity.TrackRoundMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRoundMatrixRepository extends JpaRepository<TrackRoundMatrix, Long> {
    List<TrackRoundMatrix> findByTrackEventId(Long eventId);
    List<TrackRoundMatrix> findByTrackId(Long trackId);
}
