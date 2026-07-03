package com.backend.service;

import com.backend.dto.request.TeamCreateRequest;
import com.backend.entity.*;
import com.backend.entity.enums.MemberRole;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final HackathonEventRepository eventRepository;
    // (Bạn nhớ tạo interface TrackRepository và TeamMemberRepository tương tự như các repo khác nhé)
    // private final TrackRepository trackRepository;
    // private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public Team createTeam(TeamCreateRequest request) {
        // 1. Lấy email của người dùng đang đăng nhập từ Token
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra tên đội đã tồn tại chưa
        if (teamRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TEAM_EXISTED);
        }

        // Tạm thời comment logic check Event/Track để build thử trước, sau này bạn query DB thật nhé
        /*
        HackathonEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        Track track = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Track"));
        */

        // 3. Tạo đội mới
        Team newTeam = Team.builder()
                .name(request.getName())
                .type(request.getType())
                .joinPassword(request.getJoinPassword())
                // .event(event)
                // .track(track)
                .build();
        teamRepository.save(newTeam);

        // 4. Cập nhật role của User thành LEADER và add vào TeamMember
        currentUser.setRole(RoleType.LEADER);
        userRepository.save(currentUser);

        /*
        TeamMember teamMember = TeamMember.builder()
                .team(newTeam)
                .user(currentUser)
                .role(MemberRole.LEADER)
                .build();
        teamMemberRepository.save(teamMember);
        */

        return newTeam;
    }
}