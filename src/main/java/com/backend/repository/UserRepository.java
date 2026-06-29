package com.backend.repository;

import com.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Tìm theo email
    Optional<User> findByEmail(String email);

    // Kiểm tra tồn tại
    boolean existsByEmail(String email);

    // Tìm kiếm user theo Tên hoặc Email, bỏ qua hoa thường
    // Hàm này chuẩn, dùng cho thanh search của fen
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);

    // Lưu ý: Không cần định nghĩa lại findById(Long) vì JpaRepository đã tự hiểu findById(UUID)
}