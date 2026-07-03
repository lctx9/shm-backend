package com.backend.controller;

import com.backend.dto.request.EventRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.entity.HackathonEvent;
import com.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Chỉ có Coordinator hoặc Admin mới được tạo giải đấu
    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<HackathonEvent> createEvent(@RequestBody EventRequest request) {
        return ApiResponse.<HackathonEvent>builder()
                .result(eventService.createEvent(request))
                .build();
    }
}