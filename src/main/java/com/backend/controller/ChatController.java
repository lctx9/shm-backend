package com.backend.controller;

import com.backend.dto.MessageResponse;
import com.backend.dto.SendMessageRequest;
import com.backend.entity.ChatRoom;
import com.backend.repository.ChatRoomRepository;
import com.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    // ==========================================
    // 1. LẤY CHAT ROOM ID TỪ TEAM ID
    // (Frontend gọi API này đầu tiên khi bấm vào nút "Chat với Mentor")
    // ==========================================
    @GetMapping("/rooms")
    public ResponseEntity<?> getChatRoomByTeamId(@RequestParam UUID teamId) {
        try {
            ChatRoom room = chatRoomRepository.findByTeamId(teamId)
                    .orElseThrow(() -> new RuntimeException("Phòng chat của đội này chưa được khởi tạo!"));
            return ResponseEntity.ok(room.getId());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 2. LẤY LỊCH SỬ TIN NHẮN CŨ
    // (Load lại danh sách tin nhắn khi vừa mở trang chat)
    // ==========================================
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<?> getMessages(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID chatRoomId) {
        try {
            List<MessageResponse> messages = chatService.getMessages(chatRoomId, userId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 3. KẾT NỐI REAL-TIME (SSE STREAM)
    // (Frontend mở kết nối này và để nó chạy ngầm để nhận tin nhắn mới)
    // ==========================================
    @GetMapping(value = "/rooms/{chatRoomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID chatRoomId) {
        // API này trả về SseEmitter trực tiếp, không cần try-catch ResponseEntity
        return chatService.connect(chatRoomId, userId);
    }

    // ==========================================
    // 4. GỬI TIN NHẮN MỚI
    // ==========================================
    @PostMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<?> sendMessage(@RequestHeader("X-User-Id") UUID userId,
                                         @PathVariable UUID chatRoomId,
                                         @RequestBody SendMessageRequest request) {
        try {
            MessageResponse response = chatService.sendMessage(userId, chatRoomId, request.getContent());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}