package com.backend.repository;

import com.backend.entity.Role;
import com.backend.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // Chỉ thay đổi kiểu tham số String thành Enum RoleName để đồng bộ hóa
    Optional<Role> findByName(RoleName name);
}