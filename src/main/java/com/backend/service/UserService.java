package com.backend.service;

import com.backend.dto.UserResponse; // Cần import DTO
import com.backend.entity.User;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List; // Cần import
import java.util.UUID;
import java.util.stream.Collectors; // Cần import

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<UserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

        return users.stream().map(user -> {
            UserResponse dto = new UserResponse();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setFullName(user.getFullName());
            dto.setStatus(user.getStatus().name()); // Lưu ý: Status là Enum, cần .name() để lấy String
            dto.setStudentId(user.getStudentId());
            dto.setUniversityName(user.getUniversityName());
            dto.setRoles(user.getRoles());
            return dto;
        }).collect(Collectors.toList());
    }

    public String updateAccountStatus(UUID userId, String statusStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));

        try {
            UserStatus newStatus = UserStatus.valueOf(statusStr.toUpperCase());
            user.setStatus(newStatus);
            userRepository.save(user);
            return "Tài khoản " + user.getEmail() + " đã chuyển sang trạng thái: " + newStatus;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + statusStr);
        }
    }
}