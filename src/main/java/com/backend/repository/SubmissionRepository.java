package com.backend.repository;

import com.backend.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTeamId(Long teamId);
    List<Submission> findByMatrixId(Long matrixId);
    boolean existsByTeamIdAndMatrixId(Long teamId, Long matrixId);
    List<Submission> findByIsGradedTrueOrderByScoreDesc();
    long countByIsGradedFalse();
}
