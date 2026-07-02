package com.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

// Giả định hệ thống ghi nhận thực thể Log chung
@Repository
public interface EvaluationAuditLogRepository extends JpaRepository<com.backend.entity.EvaluationAuditLog, UUID> {
}