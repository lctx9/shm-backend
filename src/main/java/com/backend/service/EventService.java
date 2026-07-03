package com.backend.service;

import com.backend.dto.request.EventRequest;
import com.backend.entity.HackathonEvent;
import com.backend.repository.HackathonEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final HackathonEventRepository eventRepository;

    public HackathonEvent createEvent(EventRequest request) {
        HackathonEvent newEvent = HackathonEvent.builder()
                .name(request.getName())
                .season(request.getSeason())
                .year(request.getYear())
                .regStartDate(request.getRegStartDate())
                .regEndDate(request.getRegEndDate())
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .isActive(true)
                .build();

        return eventRepository.save(newEvent);
    }
}