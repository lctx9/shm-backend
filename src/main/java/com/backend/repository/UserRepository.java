package com.backend.repository;

import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(RoleType role);
    List<User> findByRoleIn(java.util.Collection<RoleType> roles);
    long countByRole(RoleType role);
    long countByStatus(com.backend.entity.enums.AccountStatus status);
}
