package com.backend.repository;

import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // THÊM HÀM NÀY: Tìm kiếm user theo Tên hoặc Email, bỏ qua hoa thường
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);
}