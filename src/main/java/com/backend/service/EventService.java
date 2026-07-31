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
import com.backend.entity.Notification;
import com.backend.entity.Prize;
import com.backend.entity.Round;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.Track;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.entity.enums.RoleType;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.NotificationRepository;
import com.backend.repository.PrizeRepository;
import com.backend.repository.RoundRepository;
import com.backend.repository.ScoreRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
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
    private final ScoreRepository scoreRepository;
    private final NotificationRepository notificationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ObjectMapper objectMapper;

    private void validateEventRequest(EventRequest request) {
        if (request.getRoundCount() == null) {
            throw new RuntimeException("Số lượng vòng thi không được để trống");
        }
        if (request.getRoundCount() < 2) {
            throw new RuntimeException("Giải đấu phải có ít nhất 2 vòng thi (Vòng loại và Vòng chung kết)");
        }

        if (request.getRegStartDate() == null) {
            throw new RuntimeException("Thời gian bắt đầu đăng ký không được để trống");
        }
        if (request.getRegEndDate() == null) {
            throw new RuntimeException("Thời gian kết thúc đăng ký không được để trống");
        }
        if (request.getEventStartDate() == null) {
            throw new RuntimeException("Thời gian bắt đầu thi không được để trống");
        }
        if (request.getEventEndDate() == null) {
            throw new RuntimeException("Thời gian kết thúc thi không được để trống");
        }

        if (request.getRegStartDate().isAfter(request.getRegEndDate())) {
            throw new RuntimeException("Thời gian bắt đầu đăng ký phải trước thời gian kết thúc đăng ký");
        }
        if (request.getRegEndDate().isAfter(request.getEventStartDate())) {
            throw new RuntimeException("Thời gian kết thúc đăng ký phải trước hoặc bằng thời gian bắt đầu thi");
        }
        if (request.getEventStartDate().isAfter(request.getEventEndDate())) {
            throw new RuntimeException("Thời gian bắt đầu thi phải trước thời gian kết thúc giải đấu");
        }

        if (request.getSubmissionDeadline() != null) {
            if (request.getSubmissionDeadline().isBefore(request.getEventStartDate()) 
                    || request.getSubmissionDeadline().isAfter(request.getEventEndDate())) {
                throw new RuntimeException("Hạn nộp bài mặc định phải nằm trong thời gian diễn ra giải đấu");
            }
        }
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        validateEventRequest(request);

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
                .roundCount(request.getRoundCount())
                .structureInitialized(false)
                .submissionFormSchema(request.getSubmissionFormSchema())
                .competitionRules(request.getCompetitionRules())
                .ruleDocumentUrl(request.getRuleDocumentUrl())
                .isActive(true)
                .resultsPublished(false)
                .build();

        HackathonEvent savedEvent = eventRepository.save(newEvent);

        saveTracks(savedEvent, request);

        return getEvent(savedEvent.getId());
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request) {
        validateEventRequest(request);

        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setSeason(request.getSeason());
        event.setYear(request.getYear());
        event.setRegStartDate(request.getRegStartDate());
        event.setRegEndDate(request.getRegEndDate());
        event.setEventStartDate(request.getEventStartDate());
        event.setEventEndDate(request.getEventEndDate());
        event.setDefaultSubmissionDeadline(request.getSubmissionDeadline());
        event.setRoundCount(request.getRoundCount());
        event.setSubmissionFormSchema(request.getSubmissionFormSchema());
        event.setCompetitionRules(request.getCompetitionRules());
        event.setRuleDocumentUrl(request.getRuleDocumentUrl());
        if (request.getActive() != null) {
            event.setActive(request.getActive());
        }
        if (request.getResultsPublished() != null) {
            if (Boolean.TRUE.equals(request.getResultsPublished())
                    && !Boolean.TRUE.equals(event.getResultsPublished())) {
                List<TrackRoundMatrix> finalMatrices = matrixRepository.findByRoundEventId(eventId).stream()
                        .filter(matrix -> matrix.getTrack() == null)
                        .toList();
                if (finalMatrices.isEmpty()
                        || finalMatrices.stream().anyMatch(matrix -> !Boolean.TRUE.equals(matrix.getIsPublished()))) {
                    throw new RuntimeException("Phải chốt kết quả vòng chung kết trước khi công bố kết quả sự kiện");
                }
            }
            event.setResultsPublished(request.getResultsPublished());
        }

        if (request.getTrackConfigs() != null) {
            updateEventTracks(event, request.getTrackConfigs());
        } else if (request.getTracks() != null) {
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
                int duration = 60;
                java.time.LocalDateTime start = round.getOrderIndex() == 1 ? event.getEventStartDate() : null;
                java.time.LocalDateTime end = start != null ? start.plusMinutes(duration) : event.getDefaultSubmissionDeadline();
                matrixRepository.save(TrackRoundMatrix.builder()
                        .track(track)
                        .round(round)
                        .durationMinutes(duration)
                        .submissionStartDate(start)
                        .submissionDeadline(end)
                        .mentors(track.getMentors() == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(track.getMentors()))
                        .build());
            }
        }

        Set<User> finalMentors = tracks.stream()
                .flatMap(track -> track.getMentors() == null ? java.util.stream.Stream.empty() : track.getMentors().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Round finalRoundObj = rounds.get(rounds.size() - 1);
        java.time.LocalDateTime finalStart = finalRoundObj.getOrderIndex() == 1 ? event.getEventStartDate() : null;
        java.time.LocalDateTime finalEnd = finalStart != null ? finalStart.plusMinutes(60) : event.getDefaultSubmissionDeadline();

        matrixRepository.save(TrackRoundMatrix.builder()
                .round(finalRoundObj)
                .durationMinutes(60)
                .submissionStartDate(finalStart)
                .submissionDeadline(finalEnd)
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

        if (Boolean.TRUE.equals(matrix.getIsPublished())) {
            throw new RuntimeException("Vòng đấu đã được công bố, không thể thay đổi cấu hình chấm điểm");
        }
        if (scoreRepository.existsBySubmissionMatrixId(matrixId)) {
            throw new RuntimeException("Vòng đấu đã phát sinh điểm. Hãy xử lý điểm hiện có trước khi thay đổi cấu hình");
        }

        if (request.getJudgeIds() != null && (request.getJudgeIds().size() < 2 || request.getJudgeIds().size() > 4)) {
            throw new RuntimeException("Mỗi vòng đấu cần từ 2 đến 4 giám khảo");
        }
        if (matrix.getTrack() != null && (request.getTopN() == null || request.getTopN() < 1)) {
            throw new RuntimeException("Top N của vòng loại phải lớn hơn 0");
        }

        // Validate scoring criteria weights
        if (request.getScoringCriteriaJson() != null) {
            validateScoringCriteria(request.getScoringCriteriaJson());
        }

        // Validate mentor and judge intersection
        java.util.Set<Long> finalMentorIds;
        if (request.getMentorIds() != null) {
            finalMentorIds = request.getMentorIds();
        } else {
            finalMentorIds = matrix.getMentors() != null
                    ? matrix.getMentors().stream().map(User::getId).collect(java.util.stream.Collectors.toSet())
                    : new java.util.HashSet<>();
        }

        java.util.Set<Long> finalJudgeIds;
        if (request.getJudgeIds() != null) {
            finalJudgeIds = request.getJudgeIds();
        } else {
            finalJudgeIds = matrix.getJudges() != null
                    ? matrix.getJudges().stream().map(User::getId).collect(java.util.stream.Collectors.toSet())
                    : new java.util.HashSet<>();
        }

        java.util.Set<Long> allTrackMentorIds = new java.util.HashSet<>(finalMentorIds);
        if (matrix.getTrack() != null && matrix.getTrack().getMentors() != null) {
            matrix.getTrack().getMentors().stream()
                    .map(User::getId)
                    .forEach(allTrackMentorIds::add);
        }

        java.util.Set<Long> finalIntersection = new java.util.HashSet<>(allTrackMentorIds);
        finalIntersection.retainAll(finalJudgeIds);
        if (!finalIntersection.isEmpty()) {
            throw new RuntimeException("Một tài khoản không thể vừa làm Mentor vừa làm Judge cho cùng một bảng đấu");
        }

        if (request.getSubmissionStartDate() != null && request.getSubmissionDeadline() != null
                && request.getSubmissionStartDate().isAfter(request.getSubmissionDeadline())) {
            throw new RuntimeException("Thời gian mở nộp bài không được sau hạn nộp bài");
        }

        // Validate round order dates (no overlap, sequence order)
        int currentOrder = matrix.getRound().getOrderIndex();
        List<TrackRoundMatrix> allEventMatrices = matrixRepository.findByRoundEventId(matrix.getRound().getEvent().getId());

        java.time.LocalDateTime reqStart = request.getSubmissionStartDate();
        java.time.LocalDateTime reqEnd = request.getSubmissionDeadline();

        for (TrackRoundMatrix other : allEventMatrices) {
            if (other.getId().equals(matrix.getId())) continue;

            // Preceding round validation
            if (other.getRound().getOrderIndex() == currentOrder - 1) {
                boolean isPreceding = false;
                if (matrix.getTrack() == null) {
                    isPreceding = true;
                } else if (other.getTrack() != null && other.getTrack().getId().equals(matrix.getTrack().getId())) {
                    isPreceding = true;
                }

                if (isPreceding && other.getSubmissionDeadline() != null && reqStart != null) {
                    if (reqStart.isBefore(other.getSubmissionDeadline())) {
                        throw new RuntimeException("Thời gian mở nộp của vòng này (" + matrix.getRound().getName() + ") không được trước deadline của vòng trước (" + other.getRound().getName() + ")");
                    }
                }
            }

            // Succeeding round validation
            if (other.getRound().getOrderIndex() == currentOrder + 1) {
                boolean isSucceeding = false;
                if (other.getTrack() == null) {
                    isSucceeding = true;
                } else if (matrix.getTrack() != null && other.getTrack().getId().equals(matrix.getTrack().getId())) {
                    isSucceeding = true;
                }

                if (isSucceeding && other.getSubmissionStartDate() != null && reqEnd != null) {
                    if (reqEnd.isAfter(other.getSubmissionStartDate())) {
                        throw new RuntimeException("Deadline của vòng này (" + matrix.getRound().getName() + ") không được sau thời gian mở nộp của vòng sau (" + other.getRound().getName() + ")");
                    }
                }
            }
        }

        matrix.setGuidelineUrl(request.getGuidelineUrl());
        if (request.getDurationMinutes() != null && request.getDurationMinutes() > 0) {
            matrix.setDurationMinutes(request.getDurationMinutes());
        }
        matrix.setSubmissionStartDate(request.getSubmissionStartDate());

        int durationMinutes = matrix.getDurationMinutes() != null ? matrix.getDurationMinutes() : 60;
        if (matrix.getSubmissionStartDate() != null) {
            matrix.setSubmissionDeadline(matrix.getSubmissionStartDate().plusMinutes(durationMinutes));
        } else if (request.getSubmissionDeadline() != null) {
            matrix.setSubmissionDeadline(request.getSubmissionDeadline());
        }

        if (request.getGradingDurationMinutes() != null) {
            matrix.setGradingDurationMinutes(request.getGradingDurationMinutes());
        }
        if (request.getBreakDurationMinutes() != null) {
            matrix.setBreakDurationMinutes(request.getBreakDurationMinutes());
        }
        matrix.setScoringCriteriaJson(request.getScoringCriteriaJson());
        matrix.setTopN(request.getTopN());
        matrix.setGradingCompletionNotified(false);

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

    @Transactional
    public EventResponse endEventEarly(Long eventId) {
        HackathonEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giải đấu"));
        event.setEndedEarly(true);
        event.setEventEndDate(java.time.LocalDateTime.now());
        event.setActive(false);
        HackathonEvent savedEvent = eventRepository.save(event);

        String eventName = event.getName();
        String thankTitle = "💌 Thư cảm ơn từ Ban Tổ Chức " + eventName;
        String thankBody = "Ban Tổ Chức xin chân thành cảm ơn bạn đã tham gia và đồng hành cùng sự kiện \"" + eventName + "\" của chúng tôi! Hẹn gặp lại bạn ở những mùa giải tiếp theo.";

        Set<User> recipients = new java.util.HashSet<>();
        List<TrackRoundMatrix> matrices = matrixRepository.findByRoundEventId(eventId);
        for (TrackRoundMatrix m : matrices) {
            if (m.getJudges() != null) recipients.addAll(m.getJudges());
            if (m.getMentors() != null) recipients.addAll(m.getMentors());
        }

        List<Team> teams = teamRepository.findByEventId(eventId);
        for (Team t : teams) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(t.getId());
            for (TeamMember tm : members) {
                if (tm.getUser() != null) recipients.add(tm.getUser());
            }
        }

        for (User recipient : recipients) {
            notificationRepository.save(Notification.builder()
                    .title(thankTitle)
                    .body(thankBody)
                    .recipient(recipient)
                    .actionUrl("/events/" + eventId)
                    .build());
        }

        return toEventResponse(savedEvent);
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

        String currentUserRole = "";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                currentUserRole = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst().orElse("");
            }
        } catch (Exception ignored) {}

        boolean isCoordinatorOrAdmin = currentUserRole.contains("COORDINATOR") || currentUserRole.contains("ADMIN");
        String formattedEndDate = null;
        if (event.getEventEndDate() != null) {
            if (isCoordinatorOrAdmin) {
                formattedEndDate = event.getEventEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                formattedEndDate = event.getEventEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        }

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
                .formattedEventEndDate(formattedEndDate)
                .defaultSubmissionDeadline(event.getDefaultSubmissionDeadline())
                .roundCount(event.getRoundCount())
                .structureInitialized(Boolean.TRUE.equals(event.getStructureInitialized()) || !matrices.isEmpty())
                .active(event.isActive())
                .resultsPublished(Boolean.TRUE.equals(event.getResultsPublished()))
                .endedEarly(Boolean.TRUE.equals(event.getEndedEarly()))
                .submissionFormSchema(event.getSubmissionFormSchema())
                .competitionRules(event.getCompetitionRules())
                .ruleDocumentUrl(event.getRuleDocumentUrl())
                .teamCount(teamRepository.countEligibleTeamsByEventId(event.getId()))
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
                .maxTeams(track.getMaxTeams())
                .currentTeamsCount(teamRepository.countByTrackId(track.getId()))
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
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int gradingDuration = matrix.getGradingDurationMinutes() != null ? matrix.getGradingDurationMinutes() : 10;
        java.time.LocalDateTime gradingDeadline = matrix.getGradingDeadline();
        if (gradingDeadline == null && matrix.getSubmissionDeadline() != null && now.isAfter(matrix.getSubmissionDeadline())) {
            gradingDeadline = matrix.getSubmissionDeadline().plusMinutes(gradingDuration);
        }

        Long gradingRemainingSeconds = null;
        if (gradingDeadline != null) {
            long sec = Duration.between(now, gradingDeadline).getSeconds();
            gradingRemainingSeconds = sec > 0 ? sec : 0L;
        }

        int breakDuration = matrix.getBreakDurationMinutes() != null ? matrix.getBreakDurationMinutes() : 5;
        java.time.LocalDateTime breakEndTime = matrix.getBreakEndTime();
        Long breakRemainingSeconds = null;
        if (breakEndTime != null) {
            long sec = Duration.between(now, breakEndTime).getSeconds();
            breakRemainingSeconds = sec > 0 ? sec : 0L;
        }

        int duration = matrix.getDurationMinutes() != null ? matrix.getDurationMinutes() : 60;
        if (matrix.getSubmissionStartDate() != null && matrix.getSubmissionDeadline() != null) {
            long diff = java.time.Duration.between(matrix.getSubmissionStartDate(), matrix.getSubmissionDeadline()).toMinutes();
            if (diff > 0) duration = (int) diff;
        }

        return MatrixResponse.builder()
                .id(matrix.getId())
                .trackId(matrix.getTrack() == null ? null : matrix.getTrack().getId())
                .trackName(matrix.getTrack() == null ? "Chung kết" : matrix.getTrack().getName())
                .roundId(matrix.getRound().getId())
                .roundName(matrix.getRound().getName())
                .roundOrder(matrix.getRound().getOrderIndex())
                .finalRound(matrix.getRound() != null && matrix.getRound().getEvent() != null && java.util.Objects.equals(matrix.getRound().getOrderIndex(), matrix.getRound().getEvent().getRoundCount()))
                .isPublished(Boolean.TRUE.equals(matrix.getIsPublished()))
                .topN(matrix.getTopN())
                .durationMinutes(duration)
                .guidelineUrl(matrix.getGuidelineUrl())
                .submissionStartDate(matrix.getSubmissionStartDate())
                .submissionDeadline(matrix.getSubmissionDeadline())
                .gradingDurationMinutes(gradingDuration)
                .gradingDeadline(gradingDeadline)
                .gradingRemainingSeconds(gradingRemainingSeconds)
                .gradingExtensionNotified(Boolean.TRUE.equals(matrix.getGradingExtensionNotified()))
                .breakDurationMinutes(breakDuration)
                .breakEndTime(breakEndTime)
                .breakRemainingSeconds(breakRemainingSeconds)
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
        if (user == null) return null;
        return PublicStaffResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
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
                            .maxTeams(config.getMaxTeams())
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

    private void updateEventTracks(HackathonEvent event, List<TrackConfigRequest> configs) {
        long uniqueNames = configs.stream()
                .map(TrackConfigRequest::getName)
                .filter(java.util.Objects::nonNull)
                .map(name -> name.trim().toLowerCase())
                .distinct()
                .count();
        if (uniqueNames != configs.size()) {
            throw new RuntimeException("Tên các bảng đấu không được trùng nhau hoặc để trống");
        }
        if (configs.stream().anyMatch(config -> config.getMentorIds() == null
                || config.getMentorIds().size() < 1 || config.getMentorIds().size() > 2)) {
            throw new RuntimeException("Mỗi bảng đấu cần từ 1 đến 2 mentor");
        }

        List<Track> existingTracks = trackRepository.findByEventId(event.getId());
        java.util.Set<Long> updatedTrackIds = new java.util.HashSet<>();

        for (TrackConfigRequest config : configs) {
            Track track;
            if (config.getId() != null) {
                track = trackRepository.findById(config.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng đấu để cập nhật"));
                track.setName(config.getName().trim());
                track.setMaxTeams(config.getMaxTeams());
                java.util.Set<User> newMentors = resolveUsers(config.getMentorIds(), "mentor");
                track.setMentors(newMentors);
                trackRepository.save(track);
                updatedTrackIds.add(track.getId());

                // Sync mentors to existing qualifying round matrices for this track
                if (Boolean.TRUE.equals(event.getStructureInitialized())) {
                    List<TrackRoundMatrix> trackMatrices = matrixRepository.findByTrackId(track.getId());
                    for (TrackRoundMatrix matrix : trackMatrices) {
                        matrix.setMentors(new java.util.LinkedHashSet<>(newMentors));
                        matrixRepository.save(matrix);
                    }
                }
            } else {
                java.util.Optional<Track> matchOpt = existingTracks.stream()
                        .filter(t -> t.getName().trim().equalsIgnoreCase(config.getName().trim()))
                        .findFirst();
                if (matchOpt.isPresent()) {
                    track = matchOpt.get();
                    track.setMaxTeams(config.getMaxTeams());
                    java.util.Set<User> newMentors = resolveUsers(config.getMentorIds(), "mentor");
                    track.setMentors(newMentors);
                    trackRepository.save(track);
                    updatedTrackIds.add(track.getId());

                    // Sync mentors to existing qualifying round matrices for this track
                    if (Boolean.TRUE.equals(event.getStructureInitialized())) {
                        List<TrackRoundMatrix> trackMatrices = matrixRepository.findByTrackId(track.getId());
                        for (TrackRoundMatrix matrix : trackMatrices) {
                            matrix.setMentors(new java.util.LinkedHashSet<>(newMentors));
                            matrixRepository.save(matrix);
                        }
                    }
                } else {
                    Track newTrack = Track.builder()
                            .name(config.getName().trim())
                            .description("")
                            .event(event)
                            .mentors(resolveUsers(config.getMentorIds(), "mentor"))
                            .maxTeams(config.getMaxTeams())
                            .build();
                    trackRepository.save(newTrack);

                    // If event structure is already initialized, generate matrices for this new track!
                    if (Boolean.TRUE.equals(event.getStructureInitialized())) {
                        List<Round> rounds = roundRepository.findByEventIdOrderByOrderIndexAsc(event.getId());
                        if (rounds.size() > 1) {
                            List<Round> qualifyingRounds = rounds.subList(0, rounds.size() - 1);
                            for (Round round : qualifyingRounds) {
                                matrixRepository.save(TrackRoundMatrix.builder()
                                        .track(newTrack)
                                        .round(round)
                                        .submissionDeadline(event.getDefaultSubmissionDeadline())
                                        .mentors(newTrack.getMentors() == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(newTrack.getMentors()))
                                        .build());
                            }
                        }
                    }
                }
            }
        }

        // Delete tracks that are not in the new configuration
        for (Track existing : existingTracks) {
            if (!updatedTrackIds.contains(existing.getId())) {
                if (teamRepository.countByTrackId(existing.getId()) > 0) {
                    throw new RuntimeException("Không thể xóa bảng đấu " + existing.getName() + " vì đã có đội thi đăng ký vào bảng này");
                }
                trackRepository.delete(existing);
            }
        }
    }

    private void validateScoringCriteria(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonStr);
            if (!root.isArray()) {
                throw new RuntimeException("Tiêu chí chấm điểm phải là một danh sách (array)");
            }
            double totalWeight = 0;
            for (com.fasterxml.jackson.databind.JsonNode node : root) {
                String label = node.path("label").asText();
                if (label == null || label.trim().isEmpty()) {
                    throw new RuntimeException("Tên tiêu chí chấm điểm không được để trống");
                }
                double maxScore = node.path("maxScore").asDouble(0);
                if (maxScore <= 0) {
                    throw new RuntimeException("Điểm tối đa của tiêu chí phải lớn hơn 0");
                }
                double weight = node.path("weight").asDouble(0);
                if (weight <= 0) {
                    throw new RuntimeException("Trọng số của tiêu chí phải lớn hơn 0");
                }
                totalWeight += weight;
            }
            if (Math.abs(totalWeight - 100.0) > 0.001) {
                throw new RuntimeException("Tổng trọng số của các tiêu chí phải bằng chính xác 100%");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Định dạng tiêu chí chấm điểm không hợp lệ");
        }
    }
}
