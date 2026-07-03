package com.backend.service;

import com.backend.dto.TeamSummaryResponse;
import com.backend.entity.Team;
import com.backend.entity.enums.TeamStatus;
import com.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamViewService {

    @Autowired
    private TeamRepository teamRepository;

    // ==========================================
    // 1. LẤY DANH SÁCH ĐỘI CỦA MỘT GIẢI ĐẤU (EVENT)
    // ==========================================
    public List<TeamSummaryResponse> getTeamsByEvent(UUID eventId) {
        // Validate event tồn tại (tránh trả về list rỗng khi event không có thật)
        // (Có thể thêm EventRepository nếu muốn check kỹ, nhưng ở đây ta tin vào eventId từ URL)

        List<Team> teams = teamRepository.findByTrackEventId(eventId);

        return teams.stream()
                .filter(team -> team.getStatus() == TeamStatus.APPROVED) // Chỉ lấy đội được duyệt
                .sorted(Comparator.comparing(Team::getCreatedAt).reversed()) // Đội mới nhất lên đầu
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 2. LẤY DANH SÁCH ĐỘI CỦA MỘT HẠNG MỤC (TRACK)
    // ==========================================
    public List<TeamSummaryResponse> getTeamsByTrack(UUID trackId) {
        List<Team> teams = teamRepository.findByTrackId(trackId);

        return teams.stream()
                .filter(team -> team.getStatus() == TeamStatus.APPROVED)
                .sorted(Comparator.comparing(Team::getCreatedAt).reversed())
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // HÀM MAP ENTITY SANG DTO (ĐÚNG STYLE TEAMSERVICE)
    // ==========================================
    private TeamSummaryResponse mapToSummaryResponse(Team team) {
        TeamSummaryResponse res = new TeamSummaryResponse();
        res.setId(team.getId());
        res.setName(team.getName());
        res.setTrackName(team.getTrack().getName());
        res.setTrackId(team.getTrack().getId());
        res.setMemberCount(team.getMembers().size());
        res.setVisibility(team.getVisibility().name());
        res.setStatus(team.getStatus().name());
        res.setCreatedAt(team.getCreatedAt());
        return res;
    }
}