package com.backend.controller;

import com.backend.dto.EventDetailResponse;
import com.backend.dto.EventResponse;
import com.backend.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    // ==========================================
    // 1. LẤY DANH SÁCH GIẢI ĐẤU CHO TRANG CHỦ
    // (Chỉ lấy các giải đang mở đăng ký, đang diễn ra, hoặc đã hoàn thành)
    // ==========================================
    @GetMapping
    public ResponseEntity<?> getPublicEvents(@RequestHeader("X-User-Id") UUID userId) {
        try {
            List<EventResponse> events = eventService.getPublicEvents();
            return ResponseEntity.ok(events);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // 2. LẤY CHI TIẾT MỘT GIẢI ĐẤU (KÈM TRACKS VÀ ROUNDS)
    // ==========================================
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventDetail(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID eventId) {
        try {
            EventDetailResponse detail = eventService.getEventDetail(eventId);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}