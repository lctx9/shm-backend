package com.backend.repository;

import com.backend.entity.JoinRequest;
import com.backend.entity.Team;
import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {
    List<JoinRequest> findByTeamIdAndIsApprovedIsNull(UUID teamId); // Lấy các request chờ duyệt
    Optional<JoinRequest> findByTeamAndUserAndIsApprovedIsNull(Team team, User user);
}