package com.backend.service;

import com.backend.dto.AccountApprovalRequest;
import com.backend.dto.UserResponse;
import com.backend.entity.User;
import com.backend.entity.enums.UserStatus;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Phê duyệt hoặc từ chối tài khoản dành cho Coordinator (Có kèm lý do từ chối)
     */
    @Transactional
    public UserResponse approveOrRejectAccount(AccountApprovalRequest request) {
        // 1. Tìm user theo UUID từ request
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + request.getUserId()));

        // 2. Ràng buộc: Chỉ xử lý các tài khoản đang ở trạng thái PENDING
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("Tài khoản này đã được xử lý từ trước, trạng thái hiện tại: " + user.getStatus());
        }

        // 3. Kiểm tra trạng thái hợp lệ và xử lý lý do (reason)
        if (request.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
        } else if (request.getStatus() == UserStatus.INACTIVE) {
            // Ràng buộc bổ sung: Nếu từ chối (INACTIVE) thì bắt buộc phải nhập lý do
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng cung cấp lý do từ chối phê duyệt tài khoản này!");
            }
            user.setStatus(UserStatus.INACTIVE);

            // LƯU Ý: Nếu sau này nhóm bạn thêm trường lưu lý do vào User Entity (ví dụ: user.setRejectReason(...))
            // bạn hãy mở ghi chú dòng dưới đây để lưu vào DB nhé:
            // user.setRejectReason(request.getReason());
        } else {
            throw new IllegalArgumentException("Trạng thái phê duyệt không hợp lệ! Chỉ chấp nhận ACTIVE hoặc INACTIVE.");
        }

        // 4. Lưu lại sự thay đổi xuống database
        User updatedUser = userRepository.save(user);

        // 5. Trả về format UserResponse thông qua hàm map có sẵn của bạn
        return this.mapToUserResponse(updatedUser);
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