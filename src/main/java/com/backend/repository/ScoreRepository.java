package com.backend.repository;

import com.backend.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findBySubmissionIdAndJudgeId(Long submissionId, Long judgeId);
    List<Score> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionMatrixId(Long matrixId);
}
