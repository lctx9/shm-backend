package com.backend.repository;

import com.backend.entity.JudgeScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JudgeScoreRepository extends JpaRepository<JudgeScore, UUID> {
    Optional<JudgeScore> findBySubmissionIdAndJudgeIdAndCriteriaId(UUID submissionId, UUID judgeId, UUID criteriaId);
    List<JudgeScore> findBySubmissionIdAndJudgeId(UUID submissionId, UUID judgeId);
}