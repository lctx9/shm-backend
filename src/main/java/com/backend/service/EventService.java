package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.*;
import com.backend.entity.enums.EventStatus;
import com.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired private EventRepository eventRepository;
    @Autowired private TrackRepository trackRepository;
    @Autowired private RoundRepository roundRepository;

    // ==========================================
    // 1. LẤY DANH SÁCH GIẢI ĐẤU CHO TRANG CHỦ
    // (Chỉ lấy các giải đang mở đăng ký, đang diễn ra, hoặc đã hoàn thành)
    // ==========================================
    public List<EventResponse> getPublicEvents() {
        List<HackathonEvent> allEvents = new ArrayList<>();

        // Lấy 3 nhóm status cần hiển thị cho User
        allEvents.addAll(eventRepository.findByStatus(EventStatus.REGISTRATION_OPEN));
        allEvents.addAll(eventRepository.findByStatus(EventStatus.ONGOING));
        allEvents.addAll(eventRepository.findByStatus(EventStatus.COMPLETED));

        // Sắp xếp theo ngày bắt đầu mới nhất lên đầu (DESC)
        allEvents.sort(Comparator.comparing(HackathonEvent::getStartDate).reversed());

        // Map thủ công từ Entity sang DTO (đúng style của nhóm)
        return allEvents.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 2. LẤY CHI TIẾT MỘT GIẢI ĐẤU (KÈM TRACKS VÀ ROUNDS)
    // ==========================================
    public EventDetailResponse getEventDetail(UUID eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu!"));

        // Chỉ cho phép xem chi tiết các giải đã công bố (không phải DRAFT hay CANCELLED)
        if (event.getStatus() == EventStatus.DRAFT || event.getStatus() == EventStatus.CANCELLED) {
            throw new RuntimeException("Giải đấu này chưa được công bố hoặc đã bị hủy!");
        }

        // Lấy danh sách Tracks và Rounds của giải đấu này
        List<Track> tracks = trackRepository.findByEventId(eventId);
        List<Round> rounds = roundRepository.findByEventId(eventId);

        // Map sang DTO
        EventDetailResponse response = new EventDetailResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setSeason(event.getSeason());
        response.setAcademicYear(event.getAcademicYear());
        response.setDescription(event.getDescription());
        response.setStartDate(event.getStartDate());
        response.setEndDate(event.getEndDate());
        response.setStatus(event.getStatus().name());

        response.setTracks(tracks.stream()
                .map(this::mapToTrackResponse)
                .collect(Collectors.toList()));

        // Sắp xếp rounds theo thứ tự vòng (1, 2, 3...)
        response.setRounds(rounds.stream()
                .sorted(Comparator.comparing(Round::getRoundOrder))
                .map(this::mapToRoundResponse)
                .collect(Collectors.toList()));

        return response;
    }

    // ==========================================
    // CÁC HÀM MAP ENTITY SANG DTO (ĐÚNG STYLE TEAMSERVICE)
    // ==========================================
    private EventResponse mapToEventResponse(HackathonEvent event) {
        EventResponse res = new EventResponse();
        res.setId(event.getId());
        res.setName(event.getName());
        res.setSeason(event.getSeason());
        res.setAcademicYear(event.getAcademicYear());
        res.setDescription(event.getDescription());
        res.setStartDate(event.getStartDate());
        res.setEndDate(event.getEndDate());
        res.setStatus(event.getStatus().name());
        return res;
    }

    private TrackResponse mapToTrackResponse(Track track) {
        TrackResponse res = new TrackResponse();
        res.setId(track.getId());
        res.setName(track.getName());
        res.setDescription(track.getDescription());
        res.setAdvancementSlots(track.getAdvancementSlots());
        return res;
    }

    private RoundResponse mapToRoundResponse(Round round) {
        RoundResponse res = new RoundResponse();
        res.setId(round.getId());
        res.setName(round.getName());
        res.setRoundType(round.getRoundType().name());
        res.setRoundOrder(round.getRoundOrder());
        res.setSubmissionDeadline(round.getSubmissionDeadline());
        res.setStartTime(round.getStartTime());
        res.setEndTime(round.getEndTime());
        return res;
    }
}