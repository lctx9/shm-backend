package com.backend.repository;

import com.backend.entity.TeamMember;
import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByUser(User user);
    List<TeamMember> findByUser(User user);
    List<TeamMember> findByTeamId(Long teamId);
    long countByTeamId(Long teamId);

    // Kiểm tra xem User này đã có đội chưa
    boolean existsByUser(User user);
    boolean existsByUserIdAndTeamEventId(Long userId, Long eventId);
    Optional<TeamMember> findByUserIdAndTeamEventId(Long userId, Long eventId);
    Optional<TeamMember> findByUserIdAndTeamId(Long userId, Long teamId);
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
