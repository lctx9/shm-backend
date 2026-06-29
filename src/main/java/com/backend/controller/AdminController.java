package com.backend.controller;

import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID; // Nhớ import UUID

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserService userService;

    // 1. API cập nhật trạng thái linh hoạt (Dùng cho cả Active, Rejected, Pending...)
    // URL: PUT /api/admin/users/{id}/status?status=ACTIVE
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(userService.updateAccountStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Nếu vẫn muốn giữ API riêng biệt cho Reject (để Frontend gọi cho tiện)
    // URL: PUT /api/admin/users/{id}/reject
    @PutMapping("/{id}/reject")
    public ResponseEntity<String> rejectUser(@PathVariable UUID id) {
        try {
            // Gọi trực tiếp với trạng thái "REJECTED"
            String message = userService.updateAccountStatus(id, "REJECTED");
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}