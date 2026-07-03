package com.backend.repository;

import com.backend.entity.Team;
import com.backend.entity.TeamJoinRequest;
import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {
    List<TeamJoinRequest> findByTeamIdAndStatus(Long teamId, String status);
    boolean existsByTeamAndUserAndStatus(Team team, User user, String status);
}
