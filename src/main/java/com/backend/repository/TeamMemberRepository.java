package com.backend.repository;

import com.backend.entity.TeamMember;
import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    // Tìm thông tin thành viên dựa vào User
    Optional<TeamMember> findByUser(User user);

    // Kiểm tra xem User này đã có đội chưa
    boolean existsByUser(User user);
}