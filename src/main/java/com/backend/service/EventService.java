package com.backend.service;

import com.backend.dto.request.EventRequest;
import com.backend.dto.request.MatrixUpdateRequest;
import com.backend.dto.request.PrizeRequest;
import com.backend.dto.response.EventResponse;
import com.backend.dto.response.MatrixResponse;
import com.backend.dto.response.PrizeResponse;
import com.backend.dto.response.RoundResponse;
import com.backend.dto.response.TrackResponse;
import com.backend.dto.response.UserProfileResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Prize;
import com.backend.entity.Round;
import com.backend.entity.Team;
import com.backend.entity.Track;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.PrizeRepository;
import com.backend.repository.RoundRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PrizeRepository prizeRepository;

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

    @Transactional
    public MatrixResponse updateMatrix(Long matrixId, MatrixUpdateRequest request) {
        TrackRoundMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ô ma trận"));

        matrix.setGuidelineUrl(request.getGuidelineUrl());
        matrix.setSubmissionDeadline(request.getSubmissionDeadline());

        if (request.getMentorIds() != null) {
            matrix.setMentors(request.getMentorIds().stream()
                    .map(id -> userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy mentor")))
                    .collect(java.util.stream.Collectors.toSet()));
        }

        if (request.getJudgeIds() != null) {
            matrix.setJudges(request.getJudgeIds().stream()
                    .map(id -> userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy judge")))
                    .collect(java.util.stream.Collectors.toSet()));
        }

        return toMatrixResponse(matrixRepository.save(matrix));
    }

    public List<PrizeResponse> getPrizes(Long eventId) {
        return prizeRepository.findByEventId(eventId).stream().map(this::toPrizeResponse).toList();
    }

    @Transactional
    public PrizeResponse createPrize(Long eventId, PrizeRequest request) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        Team team = request.getTeamId() == null ? null : teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội nhận giải"));

        Prize prize = Prize.builder()
                .name(request.getName())
                .description(request.getDescription())
                .event(event)
                .team(team)
                .build();
        return toPrizeResponse(prizeRepository.save(prize));
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
                .mentors(matrix.getMentors() == null ? List.of() : matrix.getMentors().stream().map(this::toUserProfile).toList())
                .judges(matrix.getJudges() == null ? List.of() : matrix.getJudges().stream().map(this::toUserProfile).toList())
                .build();
    }

    private PrizeResponse toPrizeResponse(Prize prize) {
        return PrizeResponse.builder()
                .id(prize.getId())
                .name(prize.getName())
                .description(prize.getDescription())
                .eventId(prize.getEvent() == null ? null : prize.getEvent().getId())
                .eventName(prize.getEvent() == null ? null : prize.getEvent().getName())
                .teamId(prize.getTeam() == null ? null : prize.getTeam().getId())
                .teamName(prize.getTeam() == null ? null : prize.getTeam().getName())
                .build();
    }

    private UserProfileResponse toUserProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
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
