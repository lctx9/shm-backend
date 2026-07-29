package com.backend.repository;

import com.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    java.util.List<AuditLog> findByScoreId(Long scoreId);
    java.util.List<AuditLog> findAllByOrderByCreatedAtDesc();
}
