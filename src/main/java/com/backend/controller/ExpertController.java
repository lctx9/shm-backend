package com.backend.controller;

import com.backend.dto.CreateExpertRequest;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/coordinators/users")
@CrossOrigin(origins = "*")
public class ExpertController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    // POST /api/coordinators/users/experts
    @PostMapping("/experts")
    public ResponseEntity<?> createExpert(@RequestBody CreateExpertRequest request,
                                          @RequestParam("createdById") String createdByIdStr) {
        try {
            // 1. Validate createdById
            if (createdByIdStr == null || createdByIdStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Thiếu createdById (ID của Coordinator)!");
            }

            UUID createdById = UUID.fromString(createdByIdStr);
            User createdBy = userRepository.findById(createdById)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Coordinator với ID: " + createdByIdStr));

            // 2. Check role COORDINATOR
            boolean isCoordinator = createdBy.getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("COORDINATOR"));
            if (!isCoordinator) {
                return ResponseEntity.badRequest().body("User này không có quyền tạo tài khoản Expert!");
            }

            // 3. Gọi service tạo Expert
            Map<String, Object> response = userService.createExpert(request, createdBy);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("createdById không đúng định dạng UUID!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}