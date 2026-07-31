package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Round;
import com.backend.entity.Track;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.repository.AuditLogRepository;
import com.backend.repository.NotificationRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.HackathonEventRepository;
import com.backend.service.ScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundAdvancementControllerTest {

    @Mock
    private TrackRoundMatrixRepository matrixRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HackathonEventRepository eventRepository;
    private FakeScoreService scoreService;

    private RoundAdvancementController controller;
    private User coordinator;
    private HackathonEvent event;

    @BeforeEach
    void setUp() {
        scoreService = new FakeScoreService();
        controller = new RoundAdvancementController(
                matrixRepository,
                submissionRepository,
                teamMemberRepository,
                notificationRepository,
                auditLogRepository,
                userRepository,
                eventRepository,
                scoreService
        );
        coordinator = User.builder()
                .email("coordinator@seal.dev")
                .fullName("Coordinator")
                .build();
        coordinator.setId(1L);
        event = HackathonEvent.builder().name("SEAL 2026").build();
        event.setId(10L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(coordinator.getEmail(), null, List.of())
        );
        when(userRepository.findByEmail(coordinator.getEmail())).thenReturn(Optional.of(coordinator));
    }

    @Test
    void publishAndAdvanceRound_RejectsIncompleteScoringWithoutMutation() {
        TrackRoundMatrix matrix = matrix(100L, 1, new Track());
        when(matrixRepository.findById(100L)).thenReturn(Optional.of(matrix));
        assertThrows(RuntimeException.class, () -> controller.publishAndAdvanceRound(100L));

        assertFalse(Boolean.TRUE.equals(matrix.getIsPublished()));
        verify(matrixRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void publishAndAdvanceRound_RejectsWhenPreviousRoundIsUnpublished() {
        TrackRoundMatrix previousMatrix = matrix(100L, 1, new Track());
        TrackRoundMatrix currentMatrix = matrix(101L, 2, new Track());
        when(matrixRepository.findById(currentMatrix.getId())).thenReturn(Optional.of(currentMatrix));
        when(matrixRepository.findByRoundEventId(event.getId()))
                .thenReturn(List.of(previousMatrix, currentMatrix));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> controller.publishAndAdvanceRound(currentMatrix.getId())
        );

        assertTrue(exception.getMessage().contains("các vòng trước"));
        assertFalse(Boolean.TRUE.equals(currentMatrix.getIsPublished()));
        verify(matrixRepository, never()).save(any());
    }

    @Test
    void publishAndAdvanceRound_RejectsQualifyingRoundWithoutTopN() {
        TrackRoundMatrix matrix = matrix(101L, 1, new Track());
        matrix.setTopN(null);
        when(matrixRepository.findById(matrix.getId())).thenReturn(Optional.of(matrix));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> controller.publishAndAdvanceRound(matrix.getId())
        );

        assertTrue(exception.getMessage().contains("Top N"));
        assertFalse(Boolean.TRUE.equals(matrix.getIsPublished()));
        verify(matrixRepository, never()).save(any());
    }

    @Test
    void publishAndAdvanceRound_PublishesEveryTrackInTheSameRound() {
        Track trackA = Track.builder().name("Track A").build();
        trackA.setId(21L);
        Track trackB = Track.builder().name("Track B").build();
        trackB.setId(22L);
        TrackRoundMatrix matrixA = matrix(101L, 1, trackA);
        TrackRoundMatrix matrixB = matrix(102L, 1, trackB);
        TrackRoundMatrix finalMatrix = matrix(201L, 2, null);

        when(matrixRepository.findByRoundEventId(10L)).thenReturn(List.of(matrixA, matrixB, finalMatrix));
        scoreService.fullyGradedMatrixIds.add(matrixA.getId());
        scoreService.fullyGradedMatrixIds.add(matrixB.getId());
        when(matrixRepository.findByTrackIdAndRoundOrderIndex(21L, 2)).thenReturn(Optional.empty());
        when(matrixRepository.findByTrackIdAndRoundOrderIndex(22L, 2)).thenReturn(Optional.empty());
        when(matrixRepository.findByRoundEventIdAndTrackIsNullAndRoundOrderIndex(10L, 2))
                .thenReturn(Optional.of(finalMatrix));
        when(submissionRepository.findByMatrixId(any())).thenReturn(List.of());

        ApiResponse<String> response = controller.publishAndAdvanceRound(10L, 1);

        assertTrue(Boolean.TRUE.equals(matrixA.getIsPublished()));
        assertTrue(Boolean.TRUE.equals(matrixB.getIsPublished()));
        assertTrue(response.getResult().contains("Track A") || response.getResult().contains("Vòng 1"));
        assertTrue(scoreService.promotedMatrices.contains(matrixA));
        assertTrue(scoreService.promotedMatrices.contains(matrixB));
        verify(matrixRepository, times(4)).save(any(TrackRoundMatrix.class));
        verify(auditLogRepository, times(2)).save(any());
    }

    @Test
    void publishAndAdvanceRound_FinalRoundUsesFinalResultMessage() {
        TrackRoundMatrix finalMatrix = matrix(201L, 2, null);
        when(matrixRepository.findById(201L)).thenReturn(Optional.of(finalMatrix));
        scoreService.fullyGradedMatrixIds.add(finalMatrix.getId());
        when(submissionRepository.findByMatrixId(201L)).thenReturn(List.of());

        ApiResponse<String> response = controller.publishAndAdvanceRound(201L);

        assertTrue(Boolean.TRUE.equals(finalMatrix.getIsPublished()));
        assertTrue(response.getResult().contains("chốt kết quả"));
    }

    private TrackRoundMatrix matrix(Long id, int order, Track track) {
        Round round = Round.builder()
                .name("Vòng " + order)
                .orderIndex(order)
                .event(event)
                .build();
        round.setId(30L + order);
        TrackRoundMatrix matrix = TrackRoundMatrix.builder()
                .round(round)
                .track(track)
                .topN(track == null ? null : 1)
                .submissionDeadline(LocalDateTime.now().minusHours(1))
                .build();
        matrix.setId(id);
        return matrix;
    }

    private static final class FakeScoreService extends ScoreService {
        private final Set<Long> fullyGradedMatrixIds = new HashSet<>();
        private final List<TrackRoundMatrix> promotedMatrices = new ArrayList<>();

        private FakeScoreService() {
            super(new ObjectMapper(), null, null, null, null, null, null, null);
        }

        @Override
        public boolean isMatrixFullyGraded(TrackRoundMatrix matrix) {
            return matrix != null && fullyGradedMatrixIds.contains(matrix.getId());
        }

        @Override
        public void promoteTopTeamsWhenRoundIsComplete(TrackRoundMatrix matrix) {
            promotedMatrices.add(matrix);
        }
    }
}
