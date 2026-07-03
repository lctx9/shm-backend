package com.backend.service;

import com.backend.dto.MessageResponse;
import com.backend.entity.*;
import com.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private TrackMentorAssignmentRepository trackMentorAssignmentRepository;
    @Autowired private UserRepository userRepository;

    // Lưu trữ danh sách kết nối SSE theo ChatRoomId
    // Key: chatRoomId, Value: Danh sách các emitter đang kết nối
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // ==========================================
    // 1. QUẢN LÝ KẾT NỐI REAL-TIME (SSE)
    // ==========================================
    public SseEmitter connect(UUID chatRoomId, UUID userId) {
        // Kiểm tra quyền trước khi cho kết nối
        checkAccess(userId, chatRoomId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Kết nối lâu dài

        // Xóa emitter khỏi map khi kết thúc
        emitter.onCompletion(() -> removeEmitter(chatRoomId, emitter));
        emitter.onTimeout(() -> removeEmitter(chatRoomId, emitter));
        emitter.onError(e -> removeEmitter(chatRoomId, emitter));

        emitters.computeIfAbsent(chatRoomId, k -> new ArrayList<>()).add(emitter);
        return emitter;
    }

    private void removeEmitter(UUID chatRoomId, SseEmitter emitter) {
        List<SseEmitter> roomEmitters = emitters.get(chatRoomId);
        if (roomEmitters != null) {
            roomEmitters.remove(emitter);
        }
    }

    // ==========================================
    // 2. GỬI VÀ LƯU TIN NHẮN
    // ==========================================
    @Transactional
    public MessageResponse sendMessage(UUID senderId, UUID chatRoomId, String content) {
        // 1. Kiểm tra quyền gửi tin
        checkAccess(senderId, chatRoomId);

        if (content == null || content.isBlank()) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống!");
        }

        // 2. Lấy thông tin người gửi và phòng chat
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người gửi!"));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chat!"));

        // 3. Lưu tin nhắn vào DB
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .build();
        messageRepository.save(message);

        // 4. Tạo response để trả về và đẩy đi
        MessageResponse response = mapToResponse(message);

        // 5. Đẩy tin nhắn đến TẤT CẢ người đang kết nối trong phòng (Real-time)
        pushToRoom(chatRoomId, response);

        return response;
    }

    private void pushToRoom(UUID chatRoomId, MessageResponse response) {
        List<SseEmitter> roomEmitters = emitters.get(chatRoomId);
        if (roomEmitters != null) {
            // Lọc ra các emitter còn hoạt động
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : roomEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("message").data(response));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            // Xóa các kết nối bị lỗi
            roomEmitters.removeAll(deadEmitters);
        }
    }

    // ==========================================
    // 3. LẤY LỊCH SỬ TIN NHẮN
    // ==========================================
    public List<MessageResponse> getMessages(UUID chatRoomId, UUID userId) {
        checkAccess(userId, chatRoomId);

        List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
        return messages.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ==========================================
    // 4. HÀM BỔ TRỢ: KIỂM TRA QUYỀN TRUY CẬP
    // ==========================================
    private void checkAccess(UUID userId, UUID chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Phòng chat không tồn tại!"));
        Team team = chatRoom.getTeam();

        // Case 1: User là thành viên của Team (Member hoặc Leader)
        boolean isTeamMember = teamMemberRepository.findByTeamAndUser(team, userRepository.findById(userId).orElse(null)).isPresent();
        if (isTeamMember) return;

        // Case 2: User là Mentor được phân công cho Track của Team
        List<TrackMentorAssignment> assignments = trackMentorAssignmentRepository.findByTrackId(team.getTrack().getId());
        boolean isMentor = assignments.stream().anyMatch(a -> a.getMentor().getId().equals(userId));
        if (isMentor) return;

        throw new RuntimeException("Bạn không có quyền truy cập phòng chat này!");
    }

    // Map Entity sang DTO thủ công (đúng style nhóm)
    private MessageResponse mapToResponse(Message message) {
        MessageResponse res = new MessageResponse();
        res.setId(message.getId());
        res.setSenderId(message.getSender().getId());
        res.setSenderName(message.getSender().getFullName());
        res.setContent(message.getContent());
        res.setCreatedAt(message.getCreatedAt());
        return res;
    }
}