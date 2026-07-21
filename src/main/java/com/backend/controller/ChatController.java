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
import com.backend.entity.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

        boolean teamMember = teamMemberRepository.findByTeamId(team.getId()).stream()
                .anyMatch(member -> member.getUser() != null && member.getUser().getId().equals(user.getId()));
        boolean trackMentor = team.getTrack() != null && team.getTrack().getMentors() != null
                && team.getTrack().getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId()));
        boolean assignedMentor = trackMentor || (team.getTrack() != null && matrixRepository.findByTrackId(team.getTrack().getId()).stream()
                .anyMatch(matrix -> matrix.getMentors() != null
                        && matrix.getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId()))));
        if (!teamMember && !assignedMentor) {
            throw new RuntimeException("Bạn không phải thành viên hoặc mentor được phân công của đội này");
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
