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
        // Gọi repo tìm kiếm song song cả tên và email theo keyword
        List<User> users = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

        // Chuyển đổi từ List<User> sang List<UserResponse> để bảo mật thông tin (ẩn password)
        return users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    // Helper method để map data từ Entity sang DTO
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setStatus(user.getStatus());
        response.setStudentId(user.getStudentId());
        response.setUniversityName(user.getUniversityName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(user.getRoles());
        return response;
    }
}