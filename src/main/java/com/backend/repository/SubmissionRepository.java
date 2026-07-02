package com.backend.repository;

import com.backend.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    // Tìm các bài nộp thuộc về một vòng thi cụ thể
    List<Submission> findByRoundId(UUID roundId);
}