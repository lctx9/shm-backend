package com.backend.service;

import com.backend.dto.request.TeamCreateRequest;
import com.backend.dto.response.TeamJoinRequestResponse;
import com.backend.dto.response.TeamMemberResponse;
import com.backend.dto.response.TeamResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Notification;
import com.backend.entity.Team;
import com.backend.entity.TeamJoinRequest;
import com.backend.entity.TeamMember;
import com.backend.entity.Track;
import com.backend.entity.User;
import com.backend.entity.enums.MemberRole;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.NotificationRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TeamJoinRequestRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamJoinRequestRepository teamJoinRequestRepository;
    private final HackathonEventRepository eventRepository;
    private final TrackRepository trackRepository;
    private final NotificationRepository notificationRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (teamRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TEAM_EXISTED);
        }

        HackathonEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        Track track = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng mục thi"));

        if (!track.getEvent().getId().equals(event.getId())) {
            throw new RuntimeException("Hạng mục thi không thuộc giải đấu đã chọn");
        }

        if (teamMemberRepository.existsByUserIdAndTeamEventId(currentUser.getId(), event.getId())) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        String rawPassword = request.getJoinPassword();
        String encodedPassword = (rawPassword != null && !rawPassword.isBlank()) ? passwordEncoder.encode(rawPassword) : null;

        Team newTeam = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .joinPassword(encodedPassword)
                .event(event)
                .track(track)
                .build();
        teamRepository.save(newTeam);

        TeamMember leaderMember = TeamMember.builder()
                .team(newTeam)
                .user(currentUser)
                .role(MemberRole.LEADER)
                .build();
        teamMemberRepository.save(leaderMember);

        return toTeamResponse(newTeam);
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(t -> toTeamResponse(t, true)).toList();
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
        TeamMember leader = getCurrentMembership();
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được xoá thành viên");
        }

        TeamMember target = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên"));

        if (!target.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Thành viên không thuộc đội này");
        }

        if (target.getRole() == MemberRole.LEADER) {
            throw new RuntimeException("Không thể xoá Team Leader hiện tại");
        }

        teamMemberRepository.deleteById(memberId);
    }

    @Transactional
    public TeamResponse inviteMemberByEmail(Long teamId, String email) {
        TeamMember leader = getCurrentMembership();
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được mời thành viên");
        }

        User invitedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email thành viên"));

        HackathonEvent event = leader.getTeam().getEvent();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        if (teamMemberRepository.existsByUserIdAndTeamEventId(invitedUser.getId(), event.getId())) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        long memberCount = teamMemberRepository.countByTeamId(leader.getTeam().getId());
        if (memberCount >= 5) {
            throw new RuntimeException("Đội đã đạt tối đa 5 thành viên");
        }

        teamMemberRepository.save(TeamMember.builder()
                .team(leader.getTeam())
                .user(invitedUser)
                .role(MemberRole.MEMBER)
                .build());

        notificationRepository.save(Notification.builder()
                .title("Bạn đã được mời vào đội " + leader.getTeam().getName())
                .body(leader.getUser().getFullName() + " đã mời bạn tham gia đội. Mở trang Đội của tôi để xem chi tiết.")
                .recipient(invitedUser)
                .sender(leader.getUser())
                .actionUrl("/my-team")
                .build());

        return toTeamResponse(leader.getTeam());
    }

    @Transactional
    public TeamResponse transferLeader(Long teamId, Long memberId) {
        TeamMember currentLeader = getCurrentMembership();
        if (!currentLeader.getTeam().getId().equals(teamId) || currentLeader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader hiện tại mới được chuyển quyền");
        }

        TeamMember nextLeader = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên"));

        if (!nextLeader.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Thành viên không thuộc đội này");
        }

        currentLeader.setRole(MemberRole.MEMBER);
        nextLeader.setRole(MemberRole.LEADER);

        teamMemberRepository.save(currentLeader);
        teamMemberRepository.save(nextLeader);

        return toTeamResponse(currentLeader.getTeam());
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

        HackathonEvent event = team.getEvent();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        if (teamMemberRepository.existsByUserIdAndTeamEventId(currentUser.getId(), event.getId())) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        if (memberCount >= 5) {
            throw new RuntimeException("Đội đã đạt tối đa 5 thành viên");
        }

        if (teamJoinRequestRepository.existsByTeamAndUserAndStatus(team, currentUser, "PENDING")) {
            throw new RuntimeException("Bạn đã gửi yêu cầu tham gia đội này");
        }

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .team(team)
                .user(currentUser)
                .status("PENDING")
                .build();
        teamJoinRequestRepository.save(joinRequest);
    }

    public List<TeamJoinRequestResponse> getPendingJoinRequests(Long teamId) {
        TeamMember leader = getCurrentMembership();
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được xem yêu cầu tham gia");
        }

        return teamJoinRequestRepository.findByTeamIdAndStatus(teamId, "PENDING").stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Transactional
    public TeamResponse approveJoinRequest(Long teamId, Long requestId) {
        TeamMember leader = getCurrentMembership();
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được duyệt yêu cầu tham gia");
        }

        TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu tham gia"));

        if (!joinRequest.getTeam().getId().equals(teamId) || !"PENDING".equals(joinRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu tham gia không hợp lệ");
        }

        HackathonEvent event = leader.getTeam().getEvent();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        if (teamMemberRepository.existsByUserIdAndTeamEventId(joinRequest.getUser().getId(), event.getId())) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        long memberCount = teamMemberRepository.countByTeamId(leader.getTeam().getId());
        if (memberCount >= 5) {
            throw new RuntimeException("Đội đã đạt tối đa 5 thành viên");
        }

        teamMemberRepository.save(TeamMember.builder()
                .team(joinRequest.getTeam())
                .user(joinRequest.getUser())
                .role(MemberRole.MEMBER)
                .build());
        joinRequest.setStatus("APPROVED");
        teamJoinRequestRepository.save(joinRequest);

        return toTeamResponse(joinRequest.getTeam());
    }

    @Transactional
    public void rejectJoinRequest(Long teamId, Long requestId) {
        TeamMember leader = getCurrentMembership();
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được từ chối yêu cầu tham gia");
        }

        TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu tham gia"));

        if (!joinRequest.getTeam().getId().equals(teamId) || !"PENDING".equals(joinRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu tham gia không hợp lệ");
        }

        joinRequest.setStatus("REJECTED");
        teamJoinRequestRepository.save(joinRequest);
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

        if (team.getJoinPassword() == null || !passwordEncoder.matches(password, team.getJoinPassword())) {
            throw new AppException(ErrorCode.WRONG_JOIN_PASSWORD);
        }

        HackathonEvent event = team.getEvent();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        if (teamMemberRepository.existsByUserIdAndTeamEventId(currentUser.getId(), event.getId())) {
            throw new AppException(ErrorCode.ALREADY_IN_TEAM);
        }

        long memberCount = teamMemberRepository.countByTeamId(team.getId());
        if (memberCount >= 5) {
            throw new RuntimeException("Đội đã đạt tối đa 5 thành viên");
        }

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build();
        teamMemberRepository.save(newMember);
    }

    private TeamResponse toTeamResponse(Team team) {
        return toTeamResponse(team, false);
    }

    private TeamResponse toTeamResponse(Team team, boolean isLobby) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId()).stream()
                .map(m -> toMemberResponse(m, currentUser, isLobby))
                .toList();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .type(team.getType())
                .eventId(team.getEvent() == null ? null : team.getEvent().getId())
                .eventName(team.getEvent() == null ? null : team.getEvent().getName())
                .trackId(team.getTrack() == null ? null : team.getTrack().getId())
                .trackName(team.getTrack() == null ? null : team.getTrack().getName())
                .members(members)
                .memberCount(members.size())
                .build();
    }

    private TeamMemberResponse toMemberResponse(TeamMember member, User currentUser, boolean isLobby) {
        User user = member.getUser();
        boolean isStaffOrAdmin = currentUser != null && (
                currentUser.getRole() == com.backend.entity.enums.RoleType.ADMIN || 
                currentUser.getRole() == com.backend.entity.enums.RoleType.COORDINATOR || 
                currentUser.getRole() == com.backend.entity.enums.RoleType.MENTOR || 
                currentUser.getRole() == com.backend.entity.enums.RoleType.JUDGE
        );
        boolean isSameTeam = false;
        if (!isLobby && currentUser != null) {
            Optional<TeamMember> currentMembership = teamMemberRepository.findByUser(currentUser);
            if (currentMembership.isPresent() && currentMembership.get().getTeam().getId().equals(member.getTeam().getId())) {
                isSameTeam = true;
            }
        }

        boolean showPrivateDetails = isStaffOrAdmin || isSameTeam;

        return TeamMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(showPrivateDetails ? user.getEmail() : null)
                .studentId(showPrivateDetails ? user.getStudentId() : null)
                .universityName(user.getUniversityName())
                .role(member.getRole())
                .build();
    }

    private TeamJoinRequestResponse toJoinRequestResponse(TeamJoinRequest joinRequest) {
        User user = joinRequest.getUser();
        return TeamJoinRequestResponse.builder()
                .id(joinRequest.getId())
                .teamId(joinRequest.getTeam().getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentId(user.getStudentId())
                .status(joinRequest.getStatus())
                .build();
    }

    private TeamMember getCurrentMembership() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return teamMemberRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
    }
}
