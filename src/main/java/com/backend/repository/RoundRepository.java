package com.backend.repository;

import com.backend.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByEventIdOrderByOrderIndexAsc(Long eventId);
    long countByEventId(Long eventId);
}
