package com.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal staff info returned on public endpoints (e.g. GET /api/events).
 * Does NOT include email, role or other sensitive fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStaffResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
}
