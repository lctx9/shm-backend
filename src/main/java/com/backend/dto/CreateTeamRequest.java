package com.backend.dto;

import com.backend.entity.enums.TeamVisibility;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CreateTeamRequest {
    private String name;
    private UUID trackId;
    private List<UUID> memberIds; // Ít nhất 2 ID thành viên khác khi tạo
    private TeamVisibility visibility; // PUBLIC hoặc PRIVATE
    private String pinCode; // Chỉ truyền khi chọn PRIVATE (4 chữ số)
}