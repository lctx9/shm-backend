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
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Submission s WHERE s.isGraded = true " +
            "AND s.matrix.round.event.id = :eventId " +
            "AND s.matrix.track IS NULL")
    List<Submission> findFinalRoundGradedSubmissions(@org.springframework.data.repository.query.Param("eventId") Long eventId);
    
    long countByIsGradedFalse();
}
