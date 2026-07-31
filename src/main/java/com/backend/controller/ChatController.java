package com.backend.controller;

import com.backend.dto.request.ChatMessageRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.ChatMessageResponse;
import com.backend.entity.ChatMessage;
import com.backend.entity.Team;
import com.backend.entity.User;
import com.backend.repository.ChatMessageRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.entity.enums.RoleType;
import com.backend.entity.HackathonEvent;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TrackRoundMatrixRepository matrixRepository;
    private final SubmissionRepository submissionRepository;

    @GetMapping("/teams/{teamId}")
    public ApiResponse<List<ChatMessageResponse>> getTeamMessages(@PathVariable Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Không tìm thấy đội"));
        assertCanAccessTeamChat(team, getCurrentUser());
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(chatMessageRepository.findByTeamIdOrderByCreatedAtAsc(teamId).stream().map(this::toResponse).toList())
                .build();
    }

    @PostMapping("/teams/{teamId}")
    public ApiResponse<ChatMessageResponse> sendTeamMessage(@PathVariable Long teamId, @RequestBody ChatMessageRequest request) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Không tìm thấy đội"));
        User sender = getCurrentUser();
        assertCanAccessTeamChat(team, sender);
        ChatMessage message = ChatMessage.builder()
                .team(team)
                .sender(sender)
                .content(request.getContent())
                .build();
        return ApiResponse.<ChatMessageResponse>builder()
                .result(toResponse(chatMessageRepository.save(message)))
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private void assertCanAccessTeamChat(Team team, User user) {
        if (user.getRole() == RoleType.ADMIN || user.getRole() == RoleType.COORDINATOR) return;

        HackathonEvent event = team.getEvent();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check Event Start Time (Must be started)
        if (event != null && event.getEventStartDate() != null && now.isBefore(event.getEventStartDate())) {
            throw new RuntimeException("Cuộc thi chưa chính thức bắt đầu. Kênh chat cố vấn sẽ mở khi cuộc thi diễn ra.");
        }

        // 2. Check Event Ended Status (Ended Early or past EventEndDate)
        if (event != null && (Boolean.TRUE.equals(event.getEndedEarly())
                || (event.getEventEndDate() != null && now.isAfter(event.getEventEndDate())))) {
            throw new RuntimeException("Sự kiện đã kết thúc. Quyền tương tác cố vấn đã được thu hồi.");
        }

        // 3. Check Disqualification Status
        if ("APPROVED".equalsIgnoreCase(team.getDisqualificationStatus())) {
            throw new RuntimeException("Đội thi đã bị truất quyền thi đấu. Kênh chat đã đóng.");
        }

        // 4. Check Team Minimum Members (must have >= 3 members)
        if (teamMemberRepository.findByTeamId(team.getId()).size() < 3) {
            throw new RuntimeException("Đội thi chưa đủ 3 thành viên tối thiểu. Kênh chat chưa kích hoạt.");
        }

        // 5. Check Membership vs Mentor Assignment
        boolean teamMember = teamMemberRepository.findByTeamId(team.getId()).stream()
                .anyMatch(member -> member.getUser() != null && member.getUser().getId().equals(user.getId()));

        boolean trackMentor = team.getTrack() != null && team.getTrack().getMentors() != null
                && team.getTrack().getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId()));

        boolean assignedMentor = trackMentor || (team.getTrack() != null && matrixRepository.findByTrackId(team.getTrack().getId()).stream()
                .anyMatch(matrix -> matrix.getMentors() != null
                        && matrix.getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId()))));

        boolean eventMentor = team.getEvent() != null && matrixRepository.findByRoundEventId(team.getEvent().getId()).stream()
                .anyMatch(matrix -> matrix.getMentors() != null
                        && matrix.getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId())));

        if (!teamMember && !assignedMentor && !eventMentor) {
            throw new RuntimeException("Bạn không phải thành viên hoặc mentor được phân công của đội này");
        }

        // 6. Check Active Round Qualification for Mentors (If previous round published, team must be promoted)
        if (!teamMember && team.getTrack() != null) {
            List<TrackRoundMatrix> matrices = matrixRepository.findByTrackId(team.getTrack().getId()).stream()
                    .sorted(Comparator.comparing(m -> m.getRound().getOrderIndex()))
                    .toList();
            for (int i = 0; i < matrices.size() - 1; i++) {
                TrackRoundMatrix currentM = matrices.get(i);
                TrackRoundMatrix nextM = matrices.get(i + 1);
                if (Boolean.TRUE.equals(currentM.getIsPublished())) {
                    boolean inNextM = submissionRepository.findByMatrixId(nextM.getId()).stream()
                            .anyMatch(s -> s.getTeam() != null && s.getTeam().getId().equals(team.getId()));
                    if (!inNextM) {
                        throw new RuntimeException("Đội thi đã dừng bước ở vòng đấu trước. Kênh chat cố vấn đã đóng.");
                    }
                }
            }
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .teamId(message.getTeam() == null ? null : message.getTeam().getId())
                .teamName(message.getTeam() == null ? null : message.getTeam().getName())
                .senderName(message.getSender() == null ? null : message.getSender().getFullName())
                .senderEmail(message.getSender() == null ? null : message.getSender().getEmail())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
