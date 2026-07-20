package com.backend.config;

import com.backend.dto.request.ChatMessageRequest;
import com.backend.dto.response.ChatMessageResponse;
import com.backend.entity.ChatMessage;
import com.backend.entity.Team;
import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
import com.backend.repository.ChatMessageRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageRepository chatMessageRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TrackRoundMatrixRepository matrixRepository;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    // Quản lý các session kết nối theo teamId
    private final Map<Long, Set<WebSocketSession>> teamSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String query = session.getUri().getQuery();
            Map<String, String> queryParams = parseQueryParams(query);
            String token = queryParams.get("token");
            String teamIdStr = queryParams.get("teamId");

            if (token == null || token.isBlank() || teamIdStr == null || teamIdStr.isBlank()) {
                log.warn("WebSocket kết nối thiếu token hoặc teamId.");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            Long teamId = Long.valueOf(teamIdStr);

            // Xác thực token
            if (!jwtProvider.validateToken(token)) {
                log.warn("WebSocket kết nối với Token không hợp lệ hoặc hết hạn.");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            String email = jwtProvider.getEmailFromJWT(token);
            User user = userRepository.findByEmail(email).orElse(null);
            Team team = teamRepository.findById(teamId).orElse(null);

            if (user == null || team == null) {
                log.warn("WebSocket: Không tìm thấy User hoặc Team tương ứng.");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // Kiểm tra phân quyền truy cập chat của đội
            if (!canAccessTeamChat(team, user)) {
                log.warn("WebSocket: User {} không có quyền truy cập chat của đội {}", user.getEmail(), team.getName());
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            // Lưu thông tin vào session attributes
            session.getAttributes().put("teamId", teamId);
            session.getAttributes().put("user", user);

            // Đăng ký session vào danh sách của đội
            teamSessions.computeIfAbsent(teamId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("WebSocket: User {} đã tham gia phòng chat của đội {}", user.getEmail(), team.getName());

        } catch (Exception e) {
            log.error("Lỗi khi thiết lập kết nối WebSocket: ", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Long teamId = (Long) session.getAttributes().get("teamId");
            User sender = (User) session.getAttributes().get("user");

            if (teamId == null || sender == null) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            ChatMessageRequest request = objectMapper.readValue(message.getPayload(), ChatMessageRequest.class);
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return; // Bỏ qua tin nhắn trống
            }

            Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Không tìm thấy đội"));

            // Lưu tin nhắn vào Cơ sở dữ liệu
            ChatMessage chatMessage = ChatMessage.builder()
                    .team(team)
                    .sender(sender)
                    .content(request.getContent())
                    .build();
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            // Chuyển sang DTO và broadcast cho tất cả session của đội
            ChatMessageResponse response = toResponse(savedMessage);
            String jsonResponse = objectMapper.writeValueAsString(response);

            Set<WebSocketSession> sessions = teamSessions.get(teamId);
            if (sessions != null) {
                TextMessage broadcastMessage = new TextMessage(jsonResponse);
                for (WebSocketSession s : sessions) {
                    if (s.isOpen()) {
                        s.sendMessage(broadcastMessage);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý tin nhắn WebSocket: ", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long teamId = (Long) session.getAttributes().get("teamId");
        User user = (User) session.getAttributes().get("user");

        if (teamId != null && teamSessions.containsKey(teamId)) {
            teamSessions.get(teamId).remove(session);
            if (teamSessions.get(teamId).isEmpty()) {
                teamSessions.remove(teamId);
            }
        }
        if (user != null) {
            log.info("WebSocket: User {} đã ngắt kết nối.", user.getEmail());
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                params.put(entry[0], entry[1]);
            } else if (entry.length > 0) {
                params.put(entry[0], "");
            }
        }
        return params;
    }

    private boolean canAccessTeamChat(Team team, User user) {
        if (user.getRole() == RoleType.ADMIN || user.getRole() == RoleType.COORDINATOR) {
            return true;
        }
        boolean teamMember = teamMemberRepository.findByUser(user)
                .map(member -> member.getTeam().getId().equals(team.getId()))
                .orElse(false);
        boolean assignedMentor = team.getTrack() != null && matrixRepository.findByTrackId(team.getTrack().getId()).stream()
                .anyMatch(matrix -> matrix.getMentors() != null
                        && matrix.getMentors().stream().anyMatch(mentor -> mentor.getId().equals(user.getId())));
        return teamMember || assignedMentor;
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
