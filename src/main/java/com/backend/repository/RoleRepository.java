package com.backend.repository;

import com.backend.entity.Role;
import com.backend.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // Thay đổi tham số từ String thành RoleName
    Optional<Role> findByName(RoleName name);
}