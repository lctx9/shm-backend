package com.backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateExpertRequest {
    private String email;
    private String fullName;
    private String role;      // "JUDGE" hoặc "MENTOR"
    private Boolean isGuest;  // true nếu là Guest Judge
}