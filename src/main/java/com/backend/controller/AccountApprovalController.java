package com.backend.controller;

import com.backend.dto.AccountApprovalRequest;
import com.backend.dto.UserResponse;
import com.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coordinator")
public class AccountApprovalController {

    @Autowired
    private UserService userService;

    @PutMapping("/approve-account")
    public ResponseEntity<?> approveOrRejectAccount(@RequestBody AccountApprovalRequest request) {
        try {
            UserResponse response = userService.approveOrRejectAccount(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}