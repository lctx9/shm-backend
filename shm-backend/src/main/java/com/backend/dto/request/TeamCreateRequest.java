package com.backend.dto.request;

import com.backend.entity.enums.TeamType;
import lombok.Data;

@Data
public class TeamCreateRequest {
    private String name;
    private TeamType type; // PUBLIC hoặc PRIVATE
    private String joinPassword; // Mật khẩu nếu là PRIVATE
    private Long trackId; // ID của hạng mục tham gia
    private Long eventId; // ID của giải đấu
}