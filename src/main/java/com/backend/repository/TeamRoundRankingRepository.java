package com.backend.repository;

import com.backend.entity.TeamRoundRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRoundRankingRepository extends JpaRepository<TeamRoundRanking, UUID> {
    Optional<TeamRoundRanking> findByTeamIdAndRoundId(UUID teamId, UUID roundId);
    List<TeamRoundRanking> findByRoundId(UUID roundId);
}