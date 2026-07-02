package com.backend.service;

import com.backend.dto.UpdateProfileRequest; // Đã thêm import còn thiếu
import com.backend.dto.UserResponse;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // --- HÀM CŨ GIỮ NGUYÊN ---
    public List<UserResponse> searchUsers(String keyword) {
        // Tìm kiếm theo keyword (fullName hoặc email)
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

        // Chuyển đổi Entity sang DTO
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // CHỨC NĂNG PROFILE MỚI
    // ==========================================

    // 1. Hàm lấy thông tin Profile
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này!"));
        return mapToUserResponse(user);
    }

    // 2. Hàm cập nhật thông tin Profile
    public UserResponse updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này!"));

        // Chỉ cập nhật những trường mà Frontend gửi lên (không bị null hoặc rỗng)
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getUniversityName() != null && !request.getUniversityName().trim().isEmpty()) {
            user.setUniversityName(request.getUniversityName());
        }

        // Lưu bản ghi đã cập nhật vào DB
        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

    // 3. ĐÃ GỘP HÀM MAP (Giữ nguyên logic map Role và Status của code cũ để không bị conflict)
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setStatus(user.getStatus());

        // Thông tin sinh viên
        response.setStudentId(user.getStudentId());
        response.setUniversityName(user.getUniversityName());

        // Map roles: Giữ nguyên theo DTO cũ của bạn
        response.setRoles(user.getRoles());

        return response;
    }
}