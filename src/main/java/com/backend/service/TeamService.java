package com.backend.service;

import com.backend.dto.request.TeamCreateRequest;
import com.backend.dto.response.TeamMemberResponse;
import com.backend.dto.response.TeamResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.Track;
import com.backend.entity.User;
import com.backend.entity.enums.MemberRole;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.UserRepository;
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
    private final HackathonEventRepository eventRepository;
    private final TrackRepository trackRepository;

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (teamRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TEAM_EXISTED);
        }

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        HackathonEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        Track track = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng mục thi"));

        if (!track.getEvent().getId().equals(event.getId())) {
            throw new RuntimeException("Hạng mục thi không thuộc giải đấu đã chọn");
        }

        Team newTeam = Team.builder()
                .name(request.getName())
                .type(request.getType())
                .joinPassword(request.getJoinPassword())
                .event(event)
                .track(track)
                .build();
        teamRepository.save(newTeam);

        currentUser.setRole(RoleType.LEADER);
        userRepository.save(currentUser);

        TeamMember leaderMember = TeamMember.builder()
                .team(newTeam)
                .user(currentUser)
                .role(MemberRole.LEADER)
                .build();
        teamMemberRepository.save(leaderMember);

        return toTeamResponse(newTeam);
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(this::toTeamResponse).toList();
    }

    public TeamResponse getMyTeam() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TeamMember myMembership = teamMemberRepository.findByUser(currentUser).orElse(null);

        if (myMembership == null) {
            return null;
        }

        return toTeamResponse(myMembership.getTeam());
    }

    public void removeMember(Long teamId, Long memberId) {
        teamMemberRepository.deleteById(memberId);
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

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build();
        teamMemberRepository.save(newMember);
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

        if (team.getJoinPassword() == null || !team.getJoinPassword().equals(password)) {
            throw new AppException(ErrorCode.WRONG_JOIN_PASSWORD);
        }

        if (teamMemberRepository.existsByUser(currentUser)) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build();
        teamMemberRepository.save(newMember);
    }

    private TeamResponse toTeamResponse(Team team) {
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId()).stream()
                .map(this::toMemberResponse)
                .toList();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .type(team.getType())
                .eventId(team.getEvent() == null ? null : team.getEvent().getId())
                .eventName(team.getEvent() == null ? null : team.getEvent().getName())
                .trackId(team.getTrack() == null ? null : team.getTrack().getId())
                .trackName(team.getTrack() == null ? null : team.getTrack().getName())
                .members(members)
                .memberCount(members.size())
                .build();
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentId(user.getStudentId())
                .universityName(user.getUniversityName())
                .role(member.getRole())
                .build();
    }
}
