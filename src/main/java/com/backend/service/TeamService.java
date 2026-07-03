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

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public Team createTeam(TeamCreateRequest request) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (teamRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TEAM_EXISTED);
        }

        // Thay thế RuntimeException bằng AppException chuẩn hóa
        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        // 1. Tạo đội mới
        Team newTeam = Team.builder()
                .name(request.getName())
                .type(request.getType())
                .joinPassword(request.getJoinPassword())
                .build();
        teamRepository.save(newTeam);

        // 2. Cập nhật User thành LEADER
        currentUser.setRole(RoleType.LEADER);
        userRepository.save(currentUser);

        // 3. Lưu người tạo vào bảng TeamMember với vai trò LEADER
        TeamMember leaderMember = TeamMember.builder()
                .team(newTeam)
                .user(currentUser)
                .role(MemberRole.LEADER)
                .build();
        teamMemberRepository.save(leaderMember);

        return newTeam;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Team getMyTeam() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TeamMember myMembership = teamMemberRepository.findByUser(currentUser).orElse(null);

        if (myMembership == null) {
            return null;
        }

        return myMembership.getTeam();
    }

    public void removeMember(Long teamId, Long memberId) {
        // Xử lý logic xóa thành viên sau
    }

    @Transactional
    public void requestToJoinPublicTeam(Long teamId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));

        if (!"PUBLIC".equals(team.getType().name())) {
            throw new AppException(ErrorCode.INVALID_JOIN_TYPE);
        }

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        // TODO: Lưu vào Database bảng Yêu Cầu (JoinRequest) để Leader duyệt sau
    }

    @Transactional
    public void joinPrivateTeam(Long teamId, String password) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));

        if (!"PRIVATE".equals(team.getType().name())) {
            throw new AppException(ErrorCode.INVALID_JOIN_TYPE);
        }

        // Kiểm tra mật khẩu bằng AppException
        if (!team.getJoinPassword().equals(password)) {
            throw new AppException(ErrorCode.WRONG_JOIN_PASSWORD);
        }

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        // LƯU VÀO DATABASE BẢNG TEAM_MEMBERS
        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build();
        teamMemberRepository.save(newMember);
    }
}