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
import com.backend.repository.SubmissionRepository;
import com.backend.repository.ChatMessageRepository;
import com.backend.repository.ScoreRepository;
import com.backend.repository.PrizeRepository;
import com.backend.repository.AuditLogRepository;
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
    private final SubmissionRepository submissionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScoreRepository scoreRepository;
    private final PrizeRepository prizeRepository;
    private final AuditLogRepository auditLogRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    private void validateEventOverlap(User user, HackathonEvent newEvent) {
        List<TeamMember> currentMemberships = teamMemberRepository.findAllByUser(user);
        for (TeamMember member : currentMemberships) {
            HackathonEvent existingEvent = member.getTeam().getEvent();
            if (existingEvent.getId().equals(newEvent.getId())) {
                throw new RuntimeException("Thành viên " + user.getEmail() + " đã tham gia một đội trong cùng sự kiện này!");
            }
            if (existingEvent.getEventStartDate() != null && existingEvent.getEventEndDate() != null &&
                newEvent.getEventStartDate() != null && newEvent.getEventEndDate() != null) {
                if (existingEvent.getEventStartDate().isBefore(newEvent.getEventEndDate()) &&
                    existingEvent.getEventEndDate().isAfter(newEvent.getEventStartDate())) {
                    throw new RuntimeException("Thành viên " + user.getEmail() + " đang tham gia sự kiện '" + existingEvent.getName() + "' có thời gian thi đấu trùng lặp với sự kiện này!");
                }
            }
        }
    }

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

        long currentTeams = teamRepository.countByTrackId(track.getId());
        if (track.getMaxTeams() != null && track.getMaxTeams() > 0 && currentTeams >= track.getMaxTeams()) {
            throw new RuntimeException("Bảng đấu " + track.getName() + " đã đạt giới hạn tối đa " + track.getMaxTeams() + " đội tham gia.");
        }

        validateEventOverlap(currentUser, event);

        // Validate memberEmails (must invite at least 2 other members, unique, exists, not already in team)
        List<String> emails = request.getMemberEmails();
        if (emails == null) {
            throw new RuntimeException("Bạn phải mời tối thiểu 2 thành viên khác khi tạo đội.");
        }
        List<String> uniqueEmails = emails.stream()
                .filter(e -> e != null && !e.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
        if (uniqueEmails.size() < 2) {
            throw new RuntimeException("Bạn phải mời tối thiểu 2 thành viên khác khi tạo đội.");
        }
        if (uniqueEmails.contains(currentUserEmail)) {
            throw new RuntimeException("Không thể tự mời chính mình vào đội.");
        }
        if (uniqueEmails.size() > 4) {
            throw new RuntimeException("Đội chỉ được có tối đa 5 thành viên (kể cả Trưởng nhóm).");
        }

        java.util.List<User> invitedUsers = new java.util.ArrayList<>();
        for (String email : uniqueEmails) {
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            if (user == null || user.getStatus() != com.backend.entity.enums.AccountStatus.APPROVED) {
                throw new RuntimeException("Không tìm thấy thành viên với email: " + email);
            }
            if (user.getRole() == com.backend.entity.enums.RoleType.ADMIN ||
                user.getRole() == com.backend.entity.enums.RoleType.COORDINATOR ||
                user.getRole() == com.backend.entity.enums.RoleType.STAFF ||
                user.getRole() == com.backend.entity.enums.RoleType.MENTOR ||
                user.getRole() == com.backend.entity.enums.RoleType.JUDGE) {
                throw new RuntimeException("Không thể mời tài khoản Ban tổ chức/Staff vào đội thi.");
            }
            validateEventOverlap(user, event);
            invitedUsers.add(user);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (event.getRegStartDate() != null && now.isBefore(event.getRegStartDate())) {
            throw new RuntimeException("Sự kiện chưa mở cổng đăng ký (Thời gian đăng ký bắt đầu từ: " + event.getRegStartDate() + ")");
        }
        if (event.getRegEndDate() != null && now.isAfter(event.getRegEndDate())) {
            throw new RuntimeException("Sự kiện đã đóng cổng đăng ký (Thời gian đăng ký kết thúc vào: " + event.getRegEndDate() + ")");
        }

        String rawPassword = request.getJoinPassword();
        String joinPassword = (rawPassword != null && !rawPassword.isBlank())
                ? passwordEncoder.encode(rawPassword)
                : null;

        Team newTeam = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .joinPassword(joinPassword)
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

        // Send invitations to invited users
        for (User invitedUser : invitedUsers) {
            teamJoinRequestRepository.save(TeamJoinRequest.builder()
                    .team(newTeam)
                    .user(invitedUser)
                    .status("PENDING")
                    .type("INVITATION")
                    .build());

            notificationRepository.save(Notification.builder()
                    .title("Lời mời tham gia đội " + newTeam.getName())
                    .body(currentUser.getFullName() + " đã mời bạn tham gia đội " + newTeam.getName() + ". Vui lòng vào trang Đội của tôi để xem chi tiết và chấp nhận hoặc từ chối.")
                    .recipient(invitedUser)
                    .sender(currentUser)
                    .actionUrl("/my-team")
                    .build());
        }

        return toTeamResponse(newTeam);
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(t -> toTeamResponse(t, true)).toList();
    }

    public List<TeamResponse> getMyTeams(Long eventId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<TeamMember> memberships = teamMemberRepository.findAllByUser(currentUser);
        if (eventId != null) {
            memberships = memberships.stream()
                    .filter(m -> m.getTeam() != null && m.getTeam().getEvent() != null && m.getTeam().getEvent().getId().equals(eventId))
                    .toList();
        }

        return memberships.stream()
                .map(m -> toTeamResponse(m.getTeam()))
                .toList();
    }

    @Transactional
    public void removeMember(Long teamId, Long memberId) {
        TeamMember leader = getCurrentMembership(teamId);
        if (leader.getRole() != MemberRole.LEADER) {
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

        teamMemberRepository.delete(target);

        // Notify target user
        notificationRepository.save(Notification.builder()
                .title("Bạn đã bị xóa khỏi đội")
                .body("Bạn đã bị xóa khỏi đội " + leader.getTeam().getName() + ".")
                .recipient(target.getUser())
                .sender(leader.getUser())
                .actionUrl("/my-team")
                .build());

        // Check if remaining size drops below 3
        List<TeamMember> remainingMembers = teamMemberRepository.findByTeamId(teamId);
        if (remainingMembers.size() < 3) {
            disbandTeam(leader.getTeam());
        } else {
            // Notify other members
            for (TeamMember m : remainingMembers) {
                if (!m.getUser().getId().equals(leader.getUser().getId())) {
                    notificationRepository.save(Notification.builder()
                            .title("Thành viên bị xóa khỏi đội")
                            .body(target.getUser().getFullName() + " đã bị xóa khỏi đội.")
                            .recipient(m.getUser())
                            .sender(leader.getUser())
                            .actionUrl("/my-team")
                            .build());
                }
            }
        }
    }

    @Transactional
    public TeamResponse inviteMemberByEmail(Long teamId, String email) {
        TeamMember leader = getCurrentMembership(teamId);
        if (leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được mời thành viên");
        }

        User invitedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email thành viên"));

        if (invitedUser.getRole() == com.backend.entity.enums.RoleType.ADMIN ||
            invitedUser.getRole() == com.backend.entity.enums.RoleType.COORDINATOR ||
            invitedUser.getRole() == com.backend.entity.enums.RoleType.STAFF ||
            invitedUser.getRole() == com.backend.entity.enums.RoleType.MENTOR ||
            invitedUser.getRole() == com.backend.entity.enums.RoleType.JUDGE) {
            throw new RuntimeException("Không thể mời tài khoản Ban tổ chức/Staff vào đội thi.");
        }

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

        if (teamJoinRequestRepository.existsByTeamAndUserAndTypeAndStatus(leader.getTeam(), invitedUser, "INVITATION", "PENDING")) {
            throw new RuntimeException("Đã gửi lời mời cho thành viên này, đang chờ phản hồi.");
        }

        teamJoinRequestRepository.save(TeamJoinRequest.builder()
                .team(leader.getTeam())
                .user(invitedUser)
                .status("PENDING")
                .type("INVITATION")
                .build());

        notificationRepository.save(Notification.builder()
                .title("Lời mời tham gia đội " + leader.getTeam().getName())
                .body(leader.getUser().getFullName() + " đã mời bạn tham gia đội " + leader.getTeam().getName() + ". Vui lòng vào trang Đội của tôi để xem chi tiết và chấp nhận hoặc từ chối.")
                .recipient(invitedUser)
                .sender(leader.getUser())
                .actionUrl("/my-team")
                .build());

        return toTeamResponse(leader.getTeam());
    }

    @Transactional
    public TeamResponse transferLeader(Long teamId, Long memberId) {
        TeamMember currentLeader = getCurrentMembership(teamId);
        if (currentLeader.getRole() != MemberRole.LEADER) {
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
                .type("REQUEST")
                .build();
        teamJoinRequestRepository.save(joinRequest);

        // SEND NOTIFICATION TO TEAM LEADER
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
        TeamMember leader = teamMembers.stream()
                .filter(m -> m.getRole() == MemberRole.LEADER)
                .findFirst()
                .orElse(null);
        if (leader != null) {
            notificationRepository.save(Notification.builder()
                    .title("Yêu cầu tham gia đội mới")
                    .body(currentUser.getFullName() + " đã gửi yêu cầu gia nhập đội " + team.getName() + " của bạn.")
                    .recipient(leader.getUser())
                    .sender(currentUser)
                    .actionUrl("/my-team")
                    .build());
        }
    }

    public List<TeamJoinRequestResponse> getPendingJoinRequests(Long teamId) {
        TeamMember leader = getCurrentMembership(teamId);
        if (leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được xem yêu cầu tham gia");
        }

        return teamJoinRequestRepository.findByTeamIdAndTypeAndStatus(teamId, "REQUEST", "PENDING").stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    public List<TeamJoinRequestResponse> getPendingInvitationsSent(Long teamId) {
        TeamMember leader = getCurrentMembership(teamId);
        if (!leader.getTeam().getId().equals(teamId) || leader.getRole() != MemberRole.LEADER) {
            throw new RuntimeException("Chỉ Team Leader của đội mới được xem lời mời đã gửi");
        }
        // Trả về cả PENDING (đang chờ) lẫn REJECTED (đã từ chối)
        // để frontend hiển thị trạng thái và cho phép mời lại
        return teamJoinRequestRepository.findByTeamId(teamId).stream()
                .filter(r -> "INVITATION".equals(r.getType())
                        && ("PENDING".equals(r.getStatus()) || "REJECTED".equals(r.getStatus())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toJoinRequestResponse)
                .toList();
    }

    public List<TeamJoinRequestResponse> getMyInvitations() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return teamJoinRequestRepository.findByUserIdAndTypeAndStatus(currentUser.getId(), "INVITATION", "PENDING").stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Transactional
    public TeamResponse acceptInvitation(Long requestId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));

        if (!joinRequest.getUser().getId().equals(currentUser.getId()) || !"PENDING".equals(joinRequest.getStatus()) || !"INVITATION".equals(joinRequest.getType())) {
            throw new RuntimeException("Lời mời không hợp lệ hoặc đã được xử lý");
        }

        Team team = joinRequest.getTeam();
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

        List<TeamMember> existingMembers = teamMemberRepository.findByTeamId(team.getId());

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build());
        joinRequest.setStatus("APPROVED");
        teamJoinRequestRepository.save(joinRequest);

        // Notify all existing team members (including leader)
        for (TeamMember existing : existingMembers) {
            notificationRepository.save(Notification.builder()
                    .title("Thành viên mới gia nhập đội")
                    .body(currentUser.getFullName() + " đã chấp nhận lời mời gia nhập đội " + team.getName() + ".")
                    .recipient(existing.getUser())
                    .sender(currentUser)
                    .actionUrl("/my-team")
                    .build());
        }

        // Notify joiner
        notificationRepository.save(Notification.builder()
                .title("Gia nhập đội thành công")
                .body("Bạn đã gia nhập đội " + team.getName() + " thành công.")
                .recipient(currentUser)
                .actionUrl("/my-team")
                .build());

        return toTeamResponse(team);
    }

    @Transactional
    public void rejectInvitation(Long requestId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TeamJoinRequest joinRequest = teamJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));

        if (!joinRequest.getUser().getId().equals(currentUser.getId()) || !"PENDING".equals(joinRequest.getStatus())) {
            throw new RuntimeException("Lời mời không hợp lệ");
        }

        joinRequest.setStatus("REJECTED");
        teamJoinRequestRepository.save(joinRequest);

        Team team = joinRequest.getTeam();
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());

        // Thông báo cho leader biết có người từ chối lời mời
        // Đội không bị giải tán — leader có thể mời lại từ trang chi tiết đội
        TeamMember leader = teamMembers.stream()
                .filter(m -> m.getRole() == MemberRole.LEADER)
                .findFirst()
                .orElse(null);

        if (leader != null) {
            notificationRepository.save(Notification.builder()
                    .title("Lời mời gia nhập đội bị từ chối")
                    .body(currentUser.getFullName() + " đã từ chối lời mời gia nhập đội " + team.getName()
                            + ". Bạn có thể mời lại từ trang chi tiết đội.")
                    .recipient(leader.getUser())
                    .sender(currentUser)
                    .actionUrl("/my-team?teamId=" + team.getId())
                    .build());
        }
    }

    @Transactional
    public TeamResponse approveJoinRequest(Long teamId, Long requestId) {
        TeamMember leader = getCurrentMembership(teamId);
        if (leader.getRole() != MemberRole.LEADER) {
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

        List<TeamMember> existingMembers = teamMemberRepository.findByTeamId(leader.getTeam().getId());

        teamMemberRepository.save(TeamMember.builder()
                .team(joinRequest.getTeam())
                .user(joinRequest.getUser())
                .role(MemberRole.MEMBER)
                .build());
        joinRequest.setStatus("APPROVED");
        teamJoinRequestRepository.save(joinRequest);

        // Notify applicant
        notificationRepository.save(Notification.builder()
                .title("Yêu cầu gia nhập đội đã được duyệt")
                .body("Yêu cầu gia nhập đội " + leader.getTeam().getName() + " của bạn đã được duyệt.")
                .recipient(joinRequest.getUser())
                .sender(leader.getUser())
                .actionUrl("/my-team")
                .build());

        // Notify all existing team members
        for (TeamMember existing : existingMembers) {
            notificationRepository.save(Notification.builder()
                    .title("Thành viên mới gia nhập đội")
                    .body(joinRequest.getUser().getFullName() + " đã gia nhập đội " + leader.getTeam().getName() + ".")
                    .recipient(existing.getUser())
                    .sender(joinRequest.getUser())
                    .actionUrl("/my-team")
                    .build());
        }

        return toTeamResponse(joinRequest.getTeam());
    }

    @Transactional
    public void rejectJoinRequest(Long teamId, Long requestId) {
        TeamMember leader = getCurrentMembership(teamId);
        if (leader.getRole() != MemberRole.LEADER) {
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

        List<TeamMember> existingMembers = teamMemberRepository.findByTeamId(team.getId());

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(currentUser)
                .role(MemberRole.MEMBER)
                .build();
        teamMemberRepository.save(newMember);

        // SEND NOTIFICATION TO ALL EXISTING MEMBERS (INCLUDING LEADER)
        for (TeamMember existing : existingMembers) {
            notificationRepository.save(Notification.builder()
                    .title("Thành viên mới gia nhập đội")
                    .body(currentUser.getFullName() + " đã gia nhập đội " + team.getName() + " bằng mã PIN.")
                    .recipient(existing.getUser())
                    .sender(currentUser)
                    .actionUrl("/my-team")
                    .build());
        }

        // SEND NOTIFICATION TO JOINER
        notificationRepository.save(Notification.builder()
                .title("Gia nhập đội thành công")
                .body("Bạn đã gia nhập đội " + team.getName() + " bằng mã PIN.")
                .recipient(currentUser)
                .actionUrl("/my-team")
                .build());
    }

    @Transactional
    public void disbandTeam(Team team) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        for (TeamMember member : members) {
            notificationRepository.save(Notification.builder()
                    .title("Giải tán đội " + team.getName())
                    .body("Đội " + team.getName() + " đã bị giải tán do không còn đủ tối thiểu 3 thành viên.")
                    .recipient(member.getUser())
                    .actionUrl("/my-team")
                    .build());
        }

        // Unlink prizes to avoid database constraint violations
        List<com.backend.entity.Prize> prizes = prizeRepository.findByTeamId(team.getId());
        for (com.backend.entity.Prize prize : prizes) {
            prize.setTeam(null);
            prizeRepository.save(prize);
        }

        // Delete submissions, scores, and their audit logs
        List<com.backend.entity.Submission> submissions = submissionRepository.findByTeamId(team.getId());
        for (com.backend.entity.Submission submission : submissions) {
            List<com.backend.entity.Score> scores = scoreRepository.findBySubmissionId(submission.getId());
            for (com.backend.entity.Score score : scores) {
                List<com.backend.entity.AuditLog> logs = auditLogRepository.findByScoreId(score.getId());
                auditLogRepository.deleteAll(logs);
            }
            scoreRepository.deleteAll(scores);
        }
        submissionRepository.deleteAll(submissions);

        List<com.backend.entity.ChatMessage> chatMessages = chatMessageRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
        chatMessageRepository.deleteAll(chatMessages);

        List<com.backend.entity.TeamJoinRequest> joinRequests = teamJoinRequestRepository.findByTeamId(team.getId());
        teamJoinRequestRepository.deleteAll(joinRequests);

        teamMemberRepository.deleteAll(members);

        teamRepository.delete(team);
    }

    @Transactional
    public void leaveTeam(Long teamId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TeamMember myMembership;
        if (teamId != null) {
            myMembership = teamMemberRepository.findByUserIdAndTeamId(currentUser.getId(), teamId)
                    .orElseThrow(() -> new RuntimeException("Bạn không ở trong đội này để rời."));
        } else {
            List<TeamMember> memberships = teamMemberRepository.findAllByUser(currentUser);
            if (memberships.isEmpty()) {
                throw new RuntimeException("Bạn không ở trong đội nào để rời.");
            }
            myMembership = memberships.stream()
                    .max(java.util.Comparator.comparing(m -> m.getTeam().getEvent().getId()))
                    .orElse(memberships.get(0));
        }

        Team team = myMembership.getTeam();
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        int currentSize = members.size();

        if (myMembership.getRole() == MemberRole.LEADER) {
            throw new RuntimeException("Trưởng nhóm phải chuyển quyền Trưởng nhóm cho thành viên khác trước khi rời đội.");
        }

        teamMemberRepository.delete(myMembership);

        for (TeamMember member : members) {
            if (!member.getUser().getId().equals(currentUser.getId())) {
                notificationRepository.save(Notification.builder()
                        .title("Thành viên rời đội")
                        .body(currentUser.getFullName() + " đã rời khỏi đội " + team.getName() + ".")
                        .recipient(member.getUser())
                        .sender(currentUser)
                        .actionUrl("/my-team")
                        .build());
            }
        }

        if (currentSize - 1 < 3) {
            disbandTeam(team);
        }
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

        boolean isLeader = members.stream()
                .anyMatch(m -> m.getEmail() != null && m.getEmail().equals(currentUserEmail) && m.getRole() == MemberRole.LEADER);

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .type(team.getType())
                .eventId(team.getEvent() == null ? null : team.getEvent().getId())
                .eventName(team.getEvent() == null ? null : team.getEvent().getName())
                .eventStartDate(team.getEvent() == null ? null : team.getEvent().getEventStartDate())
                .trackId(team.getTrack() == null ? null : team.getTrack().getId())
                .trackName(team.getTrack() == null ? null : team.getTrack().getName())
                .members(members)
                .memberCount(members.size())
                .joinPassword(isLeader ? team.getJoinPassword() : null)
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
            boolean exists = teamMemberRepository.existsByUserIdAndTeamId(currentUser.getId(), member.getTeam().getId());
            if (exists) {
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
        Team team = joinRequest.getTeam();
        User leaderUser = null;
        if (team != null) {
            leaderUser = teamMemberRepository.findByTeamId(team.getId()).stream()
                    .filter(m -> m.getRole() == MemberRole.LEADER)
                    .map(TeamMember::getUser)
                    .findFirst().orElse(null);
        }
        return TeamJoinRequestResponse.builder()
                .id(joinRequest.getId())
                .teamId(team == null ? null : team.getId())
                .teamName(team == null ? null : team.getName())
                .eventId(team == null || team.getEvent() == null ? null : team.getEvent().getId())
                .eventName(team == null || team.getEvent() == null ? null : team.getEvent().getName())
                .trackName(team == null || team.getTrack() == null ? null : team.getTrack().getName())
                .userId(user == null ? null : user.getId())
                .fullName(user == null ? null : user.getFullName())
                .email(user == null ? null : user.getEmail())
                .studentId(user == null ? null : user.getStudentId())
                .status(joinRequest.getStatus())
                .type(joinRequest.getType())
                .inviterName(leaderUser == null ? null : leaderUser.getFullName())
                .createdAt(joinRequest.getCreatedAt())
                .build();
    }

    private TeamMember getCurrentMembership(Long teamId) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (teamId != null) {
            return teamMemberRepository.findByUserIdAndTeamId(currentUser.getId(), teamId)
                    .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
        } else {
            List<TeamMember> memberships = teamMemberRepository.findAllByUser(currentUser);
            if (memberships.isEmpty()) {
                throw new AppException(ErrorCode.TEAM_NOT_FOUND);
            }
            return memberships.stream()
                    .max(java.util.Comparator.comparing(m -> m.getTeam().getEvent().getId()))
                    .orElse(memberships.get(0));
        }
    }
}
