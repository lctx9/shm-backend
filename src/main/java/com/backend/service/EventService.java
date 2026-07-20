package com.backend.service;

import com.backend.dto.request.EventRequest;
import com.backend.dto.request.TrackConfigRequest;
import com.backend.dto.request.MatrixUpdateRequest;
import com.backend.dto.request.PrizeRequest;
import com.backend.dto.response.EventResponse;
import com.backend.dto.response.MatrixResponse;
import com.backend.dto.response.PrizeResponse;
import com.backend.dto.response.RoundResponse;
import com.backend.dto.response.TrackResponse;
import com.backend.dto.response.PublicStaffResponse;
import com.backend.dto.response.UserProfileResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Prize;
import com.backend.entity.Round;
import com.backend.entity.Team;
import com.backend.entity.Track;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                .description(request.getDescription())
                .season(request.getSeason())
                .year(request.getYear())
                .regStartDate(request.getRegStartDate())
                .regEndDate(request.getRegEndDate())
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .defaultSubmissionDeadline(request.getSubmissionDeadline())
                .roundCount(request.getRoundCount() == null || request.getRoundCount() < 2 ? 2 : request.getRoundCount())
                .structureInitialized(false)
                .submissionFormSchema(request.getSubmissionFormSchema())
                .competitionRules(request.getCompetitionRules())
                .ruleDocumentUrl(request.getRuleDocumentUrl())
                .isActive(true)
                .resultsPublished(request.getResultsPublished() != null ? request.getResultsPublished() : false)
                .build();

        HackathonEvent savedEvent = eventRepository.save(newEvent);

        saveTracks(savedEvent, request);

        return getEvent(savedEvent.getId());
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y giáº£i Ä‘áº¥u"));

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setSeason(request.getSeason());
        event.setYear(request.getYear());
        event.setRegStartDate(request.getRegStartDate());
        event.setRegEndDate(request.getRegEndDate());
        event.setEventStartDate(request.getEventStartDate());
        event.setEventEndDate(request.getEventEndDate());
        event.setDefaultSubmissionDeadline(request.getSubmissionDeadline());
        event.setRoundCount(request.getRoundCount() == null || request.getRoundCount() < 2 ? 2 : request.getRoundCount());
        event.setSubmissionFormSchema(request.getSubmissionFormSchema());
        event.setCompetitionRules(request.getCompetitionRules());
        event.setRuleDocumentUrl(request.getRuleDocumentUrl());
        if (request.getActive() != null) {
            event.setActive(request.getActive());
        }
        if (request.getResultsPublished() != null) {
            event.setResultsPublished(request.getResultsPublished());
        }

        boolean hasStructure = Boolean.TRUE.equals(event.getStructureInitialized()) || matrixRepository.countByRoundEventId(eventId) > 0;
        if (!hasStructure && (request.getTracks() != null || request.getTrackConfigs() != null)) {
            trackRepository.deleteAll(trackRepository.findByEventId(eventId));
            saveTracks(event, request);
        }

        eventRepository.save(event);
        return getEvent(eventId);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        
        long teamCount = teamRepository.countByEventId(eventId);
        if (teamCount > 0) {
            event.setActive(false);
            eventRepository.save(event);
        } else {
            List<Prize> prizes = prizeRepository.findByEventId(eventId);
            if (prizes != null && !prizes.isEmpty()) {
                prizeRepository.deleteAll(prizes);
            }
            
            List<TrackRoundMatrix> matrices = matrixRepository.findByRoundEventId(eventId);
            if (matrices != null && !matrices.isEmpty()) {
                matrixRepository.deleteAll(matrices);
            }
            
            List<Round> rounds = roundRepository.findByEventIdOrderByOrderIndexAsc(eventId);
            if (rounds != null && !rounds.isEmpty()) {
                roundRepository.deleteAll(rounds);
            }
            
            List<Track> tracks = trackRepository.findByEventId(eventId);
            if (tracks != null && !tracks.isEmpty()) {
                trackRepository.deleteAll(tracks);
            }
            
            eventRepository.delete(event);
        }
    }

    @Transactional
    public EventResponse initializeStructure(Long eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));

        if (Boolean.TRUE.equals(event.getStructureInitialized()) || matrixRepository.countByRoundEventId(eventId) > 0) {
            throw new RuntimeException("Cấu trúc trận đấu đã được khởi tạo");
        }

        List<Track> tracks = trackRepository.findByEventId(eventId);
        if (tracks.isEmpty()) {
            throw new RuntimeException("Giải đấu chưa có track để khởi tạo ma trận");
        }

        int roundCount = event.getRoundCount() == null || event.getRoundCount() < 2 ? 2 : event.getRoundCount();
        event.setRoundCount(roundCount);
        List<Round> rounds = java.util.stream.IntStream.rangeClosed(1, roundCount)
                .mapToObj(index -> roundRepository.save(Round.builder()
                        .name(index == roundCount ? "Vòng chung kết" : "Vòng " + index)
                        .orderIndex(index)
                        .event(event)
                        .build()))
                .toList();

        List<Round> qualifyingRounds = rounds.subList(0, rounds.size() - 1);
        for (Track track : tracks) {
            for (Round round : qualifyingRounds) {
                matrixRepository.save(TrackRoundMatrix.builder()
                        .track(track)
                        .round(round)
                        .submissionDeadline(event.getDefaultSubmissionDeadline())
                        .mentors(track.getMentors() == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(track.getMentors()))
                        .build());
            }
        }

        Set<User> finalMentors = tracks.stream()
                .flatMap(track -> track.getMentors() == null ? java.util.stream.Stream.empty() : track.getMentors().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        matrixRepository.save(TrackRoundMatrix.builder()
                .round(rounds.get(rounds.size() - 1))
                .submissionDeadline(event.getDefaultSubmissionDeadline())
                .mentors(finalMentors)
                .build());

        event.setStructureInitialized(true);
        eventRepository.save(event);
        return getEvent(eventId);
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
        return matrixRepository.findByRoundEventId(eventId).stream()
                .sorted(Comparator.comparing(matrix -> matrix.getRound().getOrderIndex()))
                .map(this::toMatrixResponse)
                .toList();
    }

    @Transactional
    public MatrixResponse updateMatrix(Long matrixId, MatrixUpdateRequest request) {
        TrackRoundMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ô ma trận"));

        if (request.getJudgeIds() != null && (request.getJudgeIds().size() < 2 || request.getJudgeIds().size() > 4)) {
            throw new RuntimeException("Mỗi vòng đấu cần từ 2 đến 4 giám khảo");
        }
        if (matrix.getTrack() != null && (request.getTopN() == null || request.getTopN() < 1)) {
            throw new RuntimeException("Top N của vòng loại phải lớn hơn 0");
        }

        matrix.setGuidelineUrl(request.getGuidelineUrl());
        matrix.setSubmissionDeadline(request.getSubmissionDeadline());
        matrix.setScoringCriteriaJson(request.getScoringCriteriaJson());
        matrix.setTopN(request.getTopN());

        if (request.getMentorIds() != null) {
            matrix.setMentors(resolveUsers(request.getMentorIds(), "mentor"));
        }

        if (request.getJudgeIds() != null) {
            matrix.setJudges(resolveUsers(request.getJudgeIds(), "judge"));
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
        if (team != null && !team.getEvent().getId().equals(event.getId())) {
            throw new RuntimeException("Đội thi không thuộc giải đấu đã chọn");
        }

        Prize prize = Prize.builder()
                .name(request.getName())
                .description(request.getDescription())
                .event(event)
                .team(team)
                .build();
        return toPrizeResponse(prizeRepository.save(prize));
    }

    @Transactional
    public PrizeResponse updatePrize(Long prizeId, PrizeRequest request) {
        Prize prize = prizeRepository.findById(prizeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải thưởng"));
        Team team = request.getTeamId() == null ? null : teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội nhận giải"));

        if (team != null && !team.getEvent().getId().equals(prize.getEvent().getId())) {
            throw new RuntimeException("Đội thi không thuộc giải đấu đã chọn");
        }

        prize.setName(request.getName());
        prize.setDescription(request.getDescription());
        prize.setTeam(team);
        return toPrizeResponse(prizeRepository.save(prize));
    }

    @Transactional
    public void deletePrize(Long prizeId) {
        prizeRepository.deleteById(prizeId);
    }

    private EventResponse toEventResponse(HackathonEvent event) {
        List<TrackResponse> tracks = trackRepository.findByEventId(event.getId()).stream()
                .map(this::toTrackResponse)
                .toList();

        List<RoundResponse> rounds = roundRepository.findByEventIdOrderByOrderIndexAsc(event.getId()).stream()
                .map(this::toRoundResponse)
                .toList();

        List<MatrixResponse> matrices = matrixRepository.findByRoundEventId(event.getId()).stream()
                .sorted(Comparator.comparing(matrix -> matrix.getRound().getOrderIndex()))
                .map(this::toMatrixResponse)
                .toList();

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .season(event.getSeason())
                .year(event.getYear())
                .regStartDate(event.getRegStartDate())
                .regEndDate(event.getRegEndDate())
                .eventStartDate(event.getEventStartDate())
                .eventEndDate(event.getEventEndDate())
                .defaultSubmissionDeadline(event.getDefaultSubmissionDeadline())
                .roundCount(event.getRoundCount())
                .structureInitialized(Boolean.TRUE.equals(event.getStructureInitialized()) || !matrices.isEmpty())
                .active(event.isActive())
                .resultsPublished(Boolean.TRUE.equals(event.getResultsPublished()))
                .submissionFormSchema(event.getSubmissionFormSchema())
                .competitionRules(event.getCompetitionRules())
                .ruleDocumentUrl(event.getRuleDocumentUrl())
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
                .mentors(track.getMentors() == null ? List.of() : track.getMentors().stream().map(this::toPublicStaff).toList())
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
                .trackId(matrix.getTrack() == null ? null : matrix.getTrack().getId())
                .trackName(matrix.getTrack() == null ? "Chung kết" : matrix.getTrack().getName())
                .roundId(matrix.getRound().getId())
                .roundName(matrix.getRound().getName())
                .roundOrder(matrix.getRound().getOrderIndex())
                .finalRound(matrix.getTrack() == null)
                .topN(matrix.getTopN())
                .guidelineUrl(matrix.getGuidelineUrl())
                .submissionDeadline(matrix.getSubmissionDeadline())
                .scoringCriteriaJson(matrix.getScoringCriteriaJson())
                .mentors(matrix.getMentors() == null ? List.of() : matrix.getMentors().stream().map(this::toPublicStaff).toList())
                .judges(matrix.getJudges() == null ? List.of() : matrix.getJudges().stream().map(this::toPublicStaff).toList())
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

    private PublicStaffResponse toPublicStaff(User user) {
        return PublicStaffResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private List<String> safeTrackNames(EventRequest request) {
        if (request.getTrackConfigs() != null && !request.getTrackConfigs().isEmpty()) {
            List<String> configuredNames = request.getTrackConfigs().stream()
                    .map(TrackConfigRequest::getName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (!configuredNames.isEmpty()) return configuredNames;
        }

        if (request.getTracks() == null || request.getTracks().isEmpty()) {
            return List.of("Bảng chung");
        }

        List<String> names = request.getTracks().stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .toList();
        return names.isEmpty() ? List.of("Bảng chung") : names;
    }

    private void saveTracks(HackathonEvent event, EventRequest request) {
        if (request.getTrackConfigs() != null && !request.getTrackConfigs().isEmpty()) {
            long uniqueNames = request.getTrackConfigs().stream()
                    .map(TrackConfigRequest::getName)
                    .filter(java.util.Objects::nonNull)
                    .map(name -> name.trim().toLowerCase())
                    .distinct()
                    .count();
            if (uniqueNames != request.getTrackConfigs().size()) {
                throw new RuntimeException("Tên các bảng đấu không được trùng nhau hoặc để trống");
            }
            if (request.getTrackConfigs().stream().anyMatch(config -> config.getMentorIds() == null
                    || config.getMentorIds().size() < 1 || config.getMentorIds().size() > 2)) {
                throw new RuntimeException("Mỗi bảng đấu cần từ 1 đến 2 mentor");
            }
            request.getTrackConfigs().stream()
                    .filter(config -> config.getName() != null && !config.getName().trim().isEmpty())
                    .forEach(config -> trackRepository.save(Track.builder()
                            .name(config.getName().trim())
                            .description("")
                            .event(event)
                            .mentors(resolveUsers(config.getMentorIds(), "mentor"))
                            .build()));
            return;
        }

        safeTrackNames(request).forEach(name -> trackRepository.save(Track.builder()
                .name(name.trim())
                .description("")
                .event(event)
                .mentors(new LinkedHashSet<>())
                .build()));
    }

    private Set<User> resolveUsers(Set<Long> ids, String label) {
        if (ids == null) return new LinkedHashSet<>();
        return ids.stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy " + label)))
                .peek(user -> {
                    if (user.getRole() != RoleType.STAFF && user.getRole() != RoleType.MENTOR && user.getRole() != RoleType.JUDGE) {
                        throw new RuntimeException("Chỉ có tài khoản STAFF mới được phân công mentor/judge");
                    }
                })
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
