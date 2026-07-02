package com.backend.repository;

import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.User;
import com.backend.entity.enums.TeamMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    boolean existsByUser(User user);
    long countByTeam(Team team);
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    Optional<TeamMember> findByTeamIdAndRole(UUID teamId, TeamMemberRole role);
}