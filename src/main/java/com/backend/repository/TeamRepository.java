package com.backend.repository;

import com.backend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    boolean existsByName(String name);
    long countByEventId(Long eventId);
    long countByTrackId(Long trackId);

    @Query("SELECT COUNT(t) FROM Team t WHERE t.track.id = :trackId AND (SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = t.id) >= 3")
    long countEligibleTeamsByTrackId(@Param("trackId") Long trackId);

    @Query("SELECT COUNT(t) FROM Team t WHERE t.event.id = :eventId AND (SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = t.id) >= 3")
    long countEligibleTeamsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(t) FROM Team t WHERE (SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = t.id) >= 3")
    long countEligibleTeams();
}

