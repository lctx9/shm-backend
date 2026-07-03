package com.backend.service;

import com.backend.dto.request.EventRequest;
import com.backend.dto.response.EventResponse;
import com.backend.dto.response.MatrixResponse;
import com.backend.dto.response.RoundResponse;
import com.backend.dto.response.TrackResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Round;
import com.backend.entity.Track;
import com.backend.entity.TrackRoundMatrix;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.RoundRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final HackathonEventRepository eventRepository;
    private final TrackRepository trackRepository;
    private final RoundRepository roundRepository;
    private final TrackRoundMatrixRepository matrixRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public EventResponse createEvent(EventRequest request) {
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

        HackathonEvent savedEvent = eventRepository.save(newEvent);

        List<Track> tracks = safeTrackNames(request).stream()
                .map(name -> trackRepository.save(Track.builder()
                        .name(name.trim())
                        .description("")
                        .event(savedEvent)
                        .build()))
                .toList();

        int roundCount = request.getRoundCount() == null || request.getRoundCount() < 1 ? 1 : request.getRoundCount();
        List<Round> rounds = java.util.stream.IntStream.rangeClosed(1, roundCount)
                .mapToObj(index -> roundRepository.save(Round.builder()
                        .name("Vòng " + index)
                        .orderIndex(index)
                        .event(savedEvent)
                        .build()))
                .toList();

        for (Track track : tracks) {
            for (Round round : rounds) {
                matrixRepository.save(TrackRoundMatrix.builder()
                        .track(track)
                        .round(round)
                        .submissionDeadline(request.getSubmissionDeadline())
                        .build());
            }
        }

        return getEvent(savedEvent.getId());
    }

    public List<EventResponse> getEvents() {
        return eventRepository.findAll().stream()
                .sorted(Comparator.comparing(HackathonEvent::getYear).reversed())
                .map(this::toEventResponse)
                .toList();
    }

    public EventResponse getEvent(Long id) {
        HackathonEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        return toEventResponse(event);
    }

    public List<TrackResponse> getTracks(Long eventId) {
        return trackRepository.findByEventId(eventId).stream().map(this::toTrackResponse).toList();
    }

    public List<MatrixResponse> getMatrices(Long eventId) {
        return matrixRepository.findByTrackEventId(eventId).stream()
                .sorted(Comparator.comparing(matrix -> matrix.getRound().getOrderIndex()))
                .map(this::toMatrixResponse)
                .toList();
    }

    private EventResponse toEventResponse(HackathonEvent event) {
        List<TrackResponse> tracks = trackRepository.findByEventId(event.getId()).stream()
                .map(this::toTrackResponse)
                .toList();

        List<RoundResponse> rounds = roundRepository.findByEventIdOrderByOrderIndexAsc(event.getId()).stream()
                .map(this::toRoundResponse)
                .toList();

        List<MatrixResponse> matrices = matrixRepository.findByTrackEventId(event.getId()).stream()
                .sorted(Comparator.comparing(matrix -> matrix.getRound().getOrderIndex()))
                .map(this::toMatrixResponse)
                .toList();

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .season(event.getSeason())
                .year(event.getYear())
                .regStartDate(event.getRegStartDate())
                .regEndDate(event.getRegEndDate())
                .eventStartDate(event.getEventStartDate())
                .eventEndDate(event.getEventEndDate())
                .active(event.isActive())
                .teamCount(teamRepository.countByEventId(event.getId()))
                .tracks(tracks)
                .rounds(rounds)
                .matrices(matrices)
                .build();
    }

    private TrackResponse toTrackResponse(Track track) {
        return TrackResponse.builder()
                .id(track.getId())
                .name(track.getName())
                .description(track.getDescription())
                .build();
    }

    private RoundResponse toRoundResponse(Round round) {
        return RoundResponse.builder()
                .id(round.getId())
                .name(round.getName())
                .orderIndex(round.getOrderIndex())
                .build();
    }

    private MatrixResponse toMatrixResponse(TrackRoundMatrix matrix) {
        return MatrixResponse.builder()
                .id(matrix.getId())
                .trackId(matrix.getTrack().getId())
                .trackName(matrix.getTrack().getName())
                .roundId(matrix.getRound().getId())
                .roundName(matrix.getRound().getName())
                .roundOrder(matrix.getRound().getOrderIndex())
                .guidelineUrl(matrix.getGuidelineUrl())
                .submissionDeadline(matrix.getSubmissionDeadline())
                .build();
    }

    private List<String> safeTrackNames(EventRequest request) {
        if (request.getTracks() == null || request.getTracks().isEmpty()) {
            return List.of("Bảng chung");
        }

        List<String> names = request.getTracks().stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .toList();
        return names.isEmpty() ? List.of("Bảng chung") : names;
    }
}
