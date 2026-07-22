package com.backend.repository;

import com.backend.entity.Team;
import com.backend.entity.TeamJoinRequest;
import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {
    List<TeamJoinRequest> findByTeamIdAndStatus(Long teamId, String status);
    List<TeamJoinRequest> findByTeamIdAndTypeAndStatus(Long teamId, String type, String status);
    List<TeamJoinRequest> findByUserIdAndTypeAndStatus(Long userId, String type, String status);
    boolean existsByTeamAndUserAndStatus(Team team, User user, String status);
    boolean existsByTeamAndUserAndTypeAndStatus(Team team, User user, String type, String status);
    List<TeamJoinRequest> findByTeamId(Long teamId);

    /** Tìm invitation (bất kể status) của 1 user trong 1 team – dùng cho upsert khi mời lại */
    Optional<TeamJoinRequest> findByTeamAndUserAndType(Team team, User user, String type);
}

