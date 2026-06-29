package com.backend.repository;

import com.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Tìm kiếm Role theo tên (ví dụ: "ROLE_STUDENT") nếu sau này bạn cần dùng text để tra cứu
    Optional<Role> findByName(String name);
}