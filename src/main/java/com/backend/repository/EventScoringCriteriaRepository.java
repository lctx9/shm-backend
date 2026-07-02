package com.backend.repository;

import com.backend.entity.EventScoringCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface EventScoringCriteriaRepository extends JpaRepository<EventScoringCriteria, UUID> {
}