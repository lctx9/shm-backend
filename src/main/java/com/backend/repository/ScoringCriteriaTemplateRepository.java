package com.backend.repository;

import com.backend.entity.ScoringCriteriaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScoringCriteriaTemplateRepository extends JpaRepository<ScoringCriteriaTemplate, UUID> {
    // Chỉ lấy các template đang active
    List<ScoringCriteriaTemplate> findByIsActiveTrue();
}