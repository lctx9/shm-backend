package com.backend.repository;

import com.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    // Có thể thêm custom query sau nếu cần
    // Ví dụ: List<AuditLog> findByTargetId(UUID targetId);
}