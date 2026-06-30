package com.backend.service;

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

    public List<UserResponse> searchUsers(String keyword) {
        // Tìm kiếm theo keyword (fullName hoặc email)
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

        // Chuyển đổi Entity sang DTO
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setStatus(user.getStatus());

        // Thông tin sinh viên
        response.setStudentId(user.getStudentId());
        response.setUniversityName(user.getUniversityName());

        // Lưu ý: User Entity hiện tại của bạn không có avatarUrl
        // Nếu bạn muốn thêm, hãy bổ sung vào class User Entity trước nhé
        // response.setAvatarUrl(user.getAvatarUrl());

        // Map roles: Tùy vào DTO của bạn yêu cầu trả về Set<Role> hay List<String>
        response.setRoles(user.getRoles());

        return response;
    }
}