package com.backend.repository;

import com.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    // Xem lịch sử theo Event, Team hoặc Submission (Đáp ứng yêu cầu từ image_7d953b.png)
    List<AuditLog> findByEventIdOrderByTimestampDesc(UUID eventId);
    List<AuditLog> findByTeamIdOrderByTimestampDesc(UUID teamId);
    List<AuditLog> findBySubmissionIdOrderByTimestampDesc(UUID submissionId);
}