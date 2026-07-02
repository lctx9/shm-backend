package com.backend.service;

import com.backend.dto.CreateTeamRequest;
import com.backend.dto.TeamDetailsResponse;
import com.backend.dto.TeamMemberResponse;
import com.backend.entity.*;
import com.backend.entity.enums.*;
import com.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service // Đánh dấu đây là một Spring Bean để Controller có thể @Autowired mượt mà
public class TeamService {

    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JoinRequestRepository joinRequestRepository;

    @PersistenceContext private EntityManager entityManager;

    // ==========================================
    // 1. CHỨC NĂNG THÀNH LẬP ĐỘI THI (TỰ ĐỘNG APPROVED)
    // ==========================================
    @Transactional
    public TeamDetailsResponse createTeam(UUID creatorUserId, CreateTeamRequest request) {
        // Kiểm tra tên đội trùng lặp
        if (teamRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên đội thi này đã được đăng ký!");
        }

        // Kiểm tra số lượng thành viên (Người tạo + ít nhất 2 memberIds = tối thiểu 3 người)
        if (request.getMemberIds() == null || request.getMemberIds().size() < 2) {
            throw new RuntimeException("Cần thêm ít nhất 2 thành viên khác để đủ tối thiểu 3 người thành lập đội!");
        }
        if (request.getMemberIds().size() > 4) {
            throw new RuntimeException("Đội thi tối đa chỉ được phép có 5 thành viên!");
        }

        // Kiểm tra xem người tạo đã thuộc đội nào chưa
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản người tạo"));
        if (teamMemberRepository.existsByUser(creator)) {
            throw new RuntimeException("Bạn đã tham gia một đội thi khác rồi!");
        }

        // Tìm kiếm và kiểm tra Track thi đấu
        Track track = entityManager.find(Track.class, request.getTrackId());
        if (track == null) throw new RuntimeException("Lĩnh vực thi (Track) không hợp lệ!");

        // Khởi tạo thực thể Team với trạng thái APPROVED luôn không cần qua Coordinator phê duyệt
        Team team = Team.builder()
                .name(request.getName())
                .track(track)
                .status(TeamStatus.APPROVED)
                .visibility(request.getVisibility() != null ? request.getVisibility() : TeamVisibility.PUBLIC)
                .build();

        // Nếu chọn chế độ PRIVATE thì bắt buộc kiểm tra mã PIN 4 số
        if (TeamVisibility.PRIVATE.equals(team.getVisibility())) {
            if (request.getPinCode() == null || !request.getPinCode().matches("\\d{4}")) {
                throw new RuntimeException("Đội Private bắt buộc phải thiết lập mã PIN gồm đúng 4 chữ số!");
            }
            team.setPinCode(request.getPinCode());
        }

        Team savedTeam = teamRepository.save(team);

        // Lưu bản ghi Trưởng nhóm (Leader) vào bảng team_members
        teamMemberRepository.save(TeamMember.builder()
                .team(savedTeam)
                .user(creator)
                .role(TeamMemberRole.LEADER)
                .build());

        // Lưu bản ghi các thành viên đi kèm vào bảng team_members
        for (UUID memberUserId : request.getMemberIds()) {
            User memberUser = userRepository.findById(memberUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên mang ID: " + memberUserId));

            if (teamMemberRepository.existsByUser(memberUser)) {
                throw new RuntimeException("Thành viên " + memberUser.getFullName() + " đã ở trong một đội thi khác!");
            }

            teamMemberRepository.save(TeamMember.builder()
                    .team(savedTeam)
                    .user(memberUser)
                    .role(TeamMemberRole.MEMBER)
                    .build());
        }

        return getTeamDetails(savedTeam.getId());
    }

    // ==========================================
    // 2. CHỨC NĂNG YÊU CẦU GIA NHẬP NHÓM (PUBLIC/PRIVATE)
    // ==========================================
    @Transactional
    public String joinTeamRequest(UUID userId, UUID teamId, String pinCodeInput) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đội thi"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng"));

        if (teamMemberRepository.existsByUser(user)) {
            throw new RuntimeException("Bạn đã tham gia một đội thi khác rồi!");
        }
        if (teamMemberRepository.countByTeam(team) >= 5) {
            throw new RuntimeException("Đội thi này đã đạt giới hạn thành viên tối đa (5 người)!");
        }

        // Xử lý dựa trên chế độ hiển thị của nhóm
        if (TeamVisibility.PRIVATE.equals(team.getVisibility())) {
            if (team.getPinCode() == null || !team.getPinCode().equals(pinCodeInput)) {
                throw new RuntimeException("Mã PIN vào nhóm không chính xác!");
            }
            // Nếu là đội Private và nhập đúng mã PIN -> Cho phép vào thẳng luôn không cần duyệt
            teamMemberRepository.save(TeamMember.builder()
                    .team(team)
                    .user(user)
                    .role(TeamMemberRole.MEMBER)
                    .build());
            return "Vào nhóm Private thành công!";
        } else {
            // Nếu là đội PUBLIC -> Tạo bản ghi yêu cầu chờ Leader duyệt
            if (joinRequestRepository.findByTeamAndUserAndIsApprovedIsNull(team, user).isPresent()) {
                throw new RuntimeException("Bạn đã gửi yêu cầu vào đội này trước đó và đang chờ duyệt!");
            }
            joinRequestRepository.save(JoinRequest.builder()
                    .team(team)
                    .user(user)
                    .build());
            return "Gửi yêu cầu gia nhập nhóm thành công! Đang chờ Team Leader phê duyệt.";
        }
    }

    // ==========================================
    // 3. TEAM LEADER PHÊ DUYỆT THÀNH VIÊN VÀO ĐỘI (PUBLIC)
    // ==========================================
    @Transactional
    public void handleJoinRequest(UUID leaderUserId, UUID requestId, boolean accept) {
        JoinRequest joinRequest = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu gia nhập này"));

        // Ràng buộc bảo mật: Chỉ có Leader của đội đó mới có quyền duyệt yêu cầu
        validateAndGetTeamAsLeader(leaderUserId, joinRequest.getTeam().getId());

        if (joinRequest.getIsApproved() != null) {
            throw new RuntimeException("Yêu cầu này đã được xử lý trước đó rồi!");
        }

        if (accept) {
            // Kiểm tra lại slot trống đề phòng đội đầy trong thời gian chờ duyệt
            if (teamMemberRepository.countByTeam(joinRequest.getTeam()) >= 5) {
                joinRequest.setIsApproved(false);
                joinRequestRepository.save(joinRequest);
                throw new RuntimeException("Đội đã đầy thành viên, tự động từ chối yêu cầu!");
            }
            if (teamMemberRepository.existsByUser(joinRequest.getUser())) {
                joinRequest.setIsApproved(false);
                joinRequestRepository.save(joinRequest);
                throw new RuntimeException("Thí sinh này đã gia nhập một đội khác mất rồi!");
            }

            joinRequest.setIsApproved(true);
            teamMemberRepository.save(TeamMember.builder()
                    .team(joinRequest.getTeam())
                    .user(joinRequest.getUser())
                    .role(TeamMemberRole.MEMBER)
                    .build());
        } else {
            joinRequest.setIsApproved(false);
        }
        joinRequestRepository.save(joinRequest);
    }

    // ==========================================
    // 4. XÓA THÀNH VIÊN KHỎI ĐỘI (DUY TRÌ TỐI THIỂU 3 NGƯỜI)
    // ==========================================
    @Transactional
    public TeamDetailsResponse removeMember(UUID leaderUserId, UUID teamId, UUID targetUserId) {
        Team team = validateAndGetTeamAsLeader(leaderUserId, teamId);

        if (leaderUserId.equals(targetUserId)) {
            throw new RuntimeException("Trưởng nhóm không thể tự xóa mình ra khỏi đội! Hãy chuyển nhượng quyền trước.");
        }
        if (teamMemberRepository.countByTeam(team) <= 3) {
            throw new RuntimeException("Không thể xóa thành viên! Đội thi phải duy trì tối thiểu 3 người theo quy chế.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thành viên cần xóa"));
        TeamMember record = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new RuntimeException("Thành viên này không thuộc đội thi của bạn!"));

        teamMemberRepository.delete(record);
        return getTeamDetails(teamId);
    }

    // ==========================================
    // 5. CHUYỂN NHƯỢNG QUYỀN TRƯỞNG NHÓM (TEAM LEADER)
    // ==========================================
    @Transactional
    public TeamDetailsResponse transferLeader(UUID currentLeaderUserId, UUID teamId, UUID newLeaderUserId) {
        Team team = validateAndGetTeamAsLeader(currentLeaderUserId, teamId);

        User newLeader = userRepository.findById(newLeaderUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên chỉ định làm trưởng nhóm mới"));
        TeamMember targetMember = teamMemberRepository.findByTeamAndUser(team, newLeader)
                .orElseThrow(() -> new RuntimeException("Người nhận quyền phải là thành viên nằm trong đội thi của bạn!"));

        // 1. Hạ chức vụ Leader hiện tại xuống làm MEMBER
        User currentLeaderUser = userRepository.getReferenceById(currentLeaderUserId);
        TeamMember currentLeaderRecord = teamMemberRepository.findByTeamAndUser(team, currentLeaderUser).get();
        //  SỬA LẠI THÀNH CHUẨN CÚ PHÁP:
        currentLeaderRecord.setRole(TeamMemberRole.MEMBER);
        teamMemberRepository.save(currentLeaderRecord);

        // 2. Đôn thành viên mới chỉ định lên làm LEADER
        targetMember.setRole(TeamMemberRole.LEADER);
        teamMemberRepository.save(targetMember);

        return getTeamDetails(teamId);
    }

    // ==========================================
    // 6. XEM THÔNG TIN CHI TIẾT ĐỘI THI VÀ DANH SÁCH THÀNH VIÊN
    // ==========================================
    public TeamDetailsResponse getTeamDetails(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đội thi yêu cầu"));

        TeamDetailsResponse response = new TeamDetailsResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setTrackId(team.getTrack().getId());
        response.setStatus(team.getStatus().name());
        response.setVisibility(team.getVisibility().name());

        // Map danh sách thành viên thủ công từ bảng trung gian team_members sang thông tin UserResponse
        List<TeamMemberResponse> membersList = team.getMembers().stream().map(tm -> {
            TeamMemberResponse res = new TeamMemberResponse();
            res.setMemberRecordId(tm.getId());
            res.setUserId(tm.getUser().getId());
            res.setFullName(tm.getUser().getFullName());
            res.setEmail(tm.getUser().getEmail());
            res.setRole(tm.getRole().name());
            return res;
        }).collect(Collectors.toList());

        response.setMembers(membersList);
        return response;
    }

    // --- HÀM BỔ TRỢ DÙNG CHUNG: KIỂM TRA PHÂN QUYỀN LEADER ---
    private Team validateAndGetTeamAsLeader(UUID userId, UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đội thi"));

        TeamMember leader = teamMemberRepository.findByTeamIdAndRole(teamId, TeamMemberRole.LEADER)
                .orElseThrow(() -> new RuntimeException("Đội thi này hiện chưa được cấu hình trưởng nhóm"));

        if (!leader.getUser().getId().equals(userId)) {
            throw new RuntimeException("Quyền hạn bị từ chối! Thao tác quản trị này chỉ dành riêng cho Trưởng nhóm.");
        }
        return team;
    }
}