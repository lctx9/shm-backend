package com.backend.controller;

import com.backend.dto.request.EventRequest;
import com.backend.dto.request.MatrixUpdateRequest;
import com.backend.dto.request.PrizeRequest;
import com.backend.dto.response.ApiResponse;
import com.backend.dto.response.EventResponse;
import com.backend.dto.response.MatrixResponse;
import com.backend.dto.response.PrizeResponse;
import com.backend.dto.response.TrackResponse;
import com.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<EventResponse>> getEvents() {
        return ApiResponse.<List<EventResponse>>builder()
                .result(eventService.getEvents())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getEvent(@PathVariable Long id) {
        return ApiResponse.<EventResponse>builder()
                .result(eventService.getEvent(id))
                .build();
    }

    @GetMapping("/{id}/tracks")
    public ApiResponse<List<TrackResponse>> getTracks(@PathVariable Long id) {
        return ApiResponse.<List<TrackResponse>>builder()
                .result(eventService.getTracks(id))
                .build();
    }

    @GetMapping("/{id}/matrices")
    public ApiResponse<List<MatrixResponse>> getMatrices(@PathVariable Long id) {
        return ApiResponse.<List<MatrixResponse>>builder()
                .result(eventService.getMatrices(id))
                .build();
    }

    @PutMapping("/matrices/{matrixId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<MatrixResponse> updateMatrix(@PathVariable Long matrixId, @RequestBody MatrixUpdateRequest request) {
        return ApiResponse.<MatrixResponse>builder()
                .result(eventService.updateMatrix(matrixId, request))
                .build();
    }

    @GetMapping("/{id}/prizes")
    public ApiResponse<List<PrizeResponse>> getPrizes(@PathVariable Long id) {
        return ApiResponse.<List<PrizeResponse>>builder()
                .result(eventService.getPrizes(id))
                .build();
    }

    @PostMapping("/{id}/prizes")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<PrizeResponse> createPrize(@PathVariable Long id, @RequestBody PrizeRequest request) {
        return ApiResponse.<PrizeResponse>builder()
                .result(eventService.createPrize(id, request))
                .build();
    }

    @PutMapping("/prizes/{prizeId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<PrizeResponse> updatePrize(@PathVariable Long prizeId, @RequestBody PrizeRequest request) {
        return ApiResponse.<PrizeResponse>builder()
                .result(eventService.updatePrize(prizeId, request))
                .build();
    }

    @DeleteMapping("/prizes/{prizeId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<String> deletePrize(@PathVariable Long prizeId) {
        eventService.deletePrize(prizeId);
        return ApiResponse.<String>builder()
                .result("Đã xoá giải thưởng")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<EventResponse> createEvent(@RequestBody EventRequest request) {
        return ApiResponse.<EventResponse>builder()
                .result(eventService.createEvent(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<EventResponse> updateEvent(@PathVariable Long id, @RequestBody EventRequest request) {
        return ApiResponse.<EventResponse>builder()
                .result(eventService.updateEvent(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<String> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ApiResponse.<String>builder()
                .result("Event da duoc tat hoat dong")
                .build();
    }

    @PostMapping("/{id}/initialize-structure")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ApiResponse<EventResponse> initializeStructure(@PathVariable Long id) {
        return ApiResponse.<EventResponse>builder()
                .result(eventService.initializeStructure(id))
                .build();
    }
}
