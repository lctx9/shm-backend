package com.backend.service;

import com.backend.dto.request.ScoreRequest;
import com.backend.entity.*;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {

    private ObjectMapper objectMapper;

    @Mock
    private ScoreRepository scoreRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrackRoundMatrixRepository matrixRepository;

    private ScoreService scoreService;

    private User judge;
    private Submission submission;
    private TrackRoundMatrix matrix;
    private Team team;
    private HackathonEvent event;
    private Round round;
    private Track track;

    @BeforeEach
    void setUp() {
        // Set SecurityContext with dummy authentication
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("judge@seal.dev", null, new java.util.ArrayList<>());
        org.springframework.security.core.context.SecurityContext securityContext =
                new org.springframework.security.core.context.SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Setup base entities
        judge = User.builder()
                .email("judge@seal.dev")
                .fullName("Test Judge")
                .role(RoleType.STAFF)
                .build();
        judge.setId(1L);

        event = HackathonEvent.builder()
                .name("SEAL Summer 2026")
                .build();
        event.setId(10L);

        round = Round.builder()
                .name("Round 1")
                .orderIndex(1)
                .event(event)
                .build();
        round.setId(20L);

        track = Track.builder()
                .name("Web Dev")
                .build();
        track.setId(30L);

        matrix = TrackRoundMatrix.builder()
                .round(round)
                .track(track)
                .submissionDeadline(LocalDateTime.now().minusDays(1)) // past deadline
                .judges(new HashSet<>(Collections.singletonList(judge)))
                .topN(2)
                .build();
        matrix.setId(40L);

        team = Team.builder()
                .name("Team A")
                .build();
        team.setId(50L);

        submission = Submission.builder()
                .team(team)
                .matrix(matrix)
                .fileUrl("http://github.com/team-a/submission")
                .isGraded(false)
                .build();
        submission.setId(60L);

        // Initialize dependencies manually
        objectMapper = new ObjectMapper();
        scoreService = new ScoreService(
                objectMapper,
                scoreRepository,
                submissionRepository,
                auditLogRepository,
                userRepository,
                matrixRepository
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // 1. HAPPY PATHS
    // ─────────────────────────────────────────────────────────────────

    @Test
    void gradeSubmission_Success_DirectScore() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(85.5);
        request.setComment("Excellent work");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());

        // For average calculation
        Score savedScoreMock = Score.builder()
                .submission(submission)
                .judge(judge)
                .scoreValue(85.5)
                .comment("Excellent work")
                .build();
        when(scoreRepository.save(any(Score.class))).thenReturn(savedScoreMock);
        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(savedScoreMock));

        Score result = scoreService.gradeSubmission(request);

        assertNotNull(result);
        assertEquals(85.5, result.getScoreValue());
        assertEquals("Excellent work", result.getComment());
        assertTrue(submission.getIsGraded());
        assertEquals(85.5, submission.getScore());
        assertEquals("Excellent work", submission.getFeedback());

        verify(scoreRepository, times(1)).save(any(Score.class));
        verify(submissionRepository, times(1)).save(submission);
    }

    @Test
    void gradeSubmission_Success_CriteriaScores() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setCriteriaScoresJson("[{\"name\":\"Design\",\"score\":90.0,\"weight\":2.0},{\"name\":\"Coding\",\"score\":80.0,\"weight\":1.0}]");
        request.setComment("Good code structure");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());

        // Expected score: (90.0 * 2.0 + 80.0 * 1.0) / 3.0 = 86.6666... -> rounded to 86.7
        Score savedScoreMock = Score.builder()
                .submission(submission)
                .judge(judge)
                .scoreValue(86.7)
                .criteriaScoresJson(request.getCriteriaScoresJson())
                .comment("Good code structure")
                .build();

        when(scoreRepository.save(any(Score.class))).thenReturn(savedScoreMock);
        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(savedScoreMock));

        Score result = scoreService.gradeSubmission(request);

        assertNotNull(result);
        assertEquals(86.7, result.getScoreValue());
        assertTrue(submission.getIsGraded());
        assertEquals(86.7, submission.getScore());
        assertEquals(request.getCriteriaScoresJson(), submission.getCriteriaScoresJson());

        verify(scoreRepository, times(1)).save(any(Score.class));
    }

    @Test
    void gradeSubmission_Success_EditScore_WithReason() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(95.0);
        request.setComment("Updated score after review");
        request.setEditReason("Correction of grading scale error");

        Score existingScore = Score.builder()
                .submission(submission)
                .judge(judge)
                .scoreValue(80.0)
                .comment("Initial score")
                .build();
        existingScore.setId(100L);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.of(existingScore));

        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(existingScore));

        Score result = scoreService.gradeSubmission(request);

        assertNotNull(result);
        assertEquals(95.0, result.getScoreValue());
        assertEquals("Correction of grading scale error", request.getEditReason());
        
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        verify(scoreRepository, times(1)).save(existingScore);
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. EXCEPTION / FAILURE CASES
    // ─────────────────────────────────────────────────────────────────

    @Test
    void gradeSubmission_Error_UserNotFound() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_SubmissionNotFound() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(999L);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.SUBMISSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_JudgeNotAssigned() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);

        User otherJudge = User.builder().email("other@seal.dev").build();
        otherJudge.setId(2L);
        matrix.setJudges(new HashSet<>(Collections.singletonList(otherJudge))); // Judge not in list

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.JUDGE_NOT_ASSIGNED, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_BeforeDeadline() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);

        // Set deadline in future
        matrix.setSubmissionDeadline(LocalDateTime.now().plusDays(1));

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> scoreService.gradeSubmission(request));
        assertTrue(exception.getMessage().contains("Hạn nộp bài của vòng đấu này chưa kết thúc"));
    }

    @Test
    void gradeSubmission_Error_InvalidDirectScoreRange_Negative() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(-5.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.INVALID_SCORE_RANGE, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_InvalidDirectScoreRange_TooHigh() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(105.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.INVALID_SCORE_RANGE, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_CriteriaScores_MissingScoreAndValue() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(null);
        request.setCriteriaScoresJson(null);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.SCORE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_CriteriaScores_InvalidJson() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setCriteriaScoresJson("{invalid_json}");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.CRITERIA_SCORE_PARSE_FAILED, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_CriteriaScores_InvalidRange_Negative() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setCriteriaScoresJson("[{\"name\":\"Design\",\"score\":-10.0,\"weight\":1.0}]");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.INVALID_CRITERIA_SCORE, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_CriteriaScores_InvalidRange_TooHigh() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setCriteriaScoresJson("[{\"name\":\"Design\",\"score\":110.0,\"weight\":1.0}]");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.INVALID_CRITERIA_SCORE, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_CriteriaScores_InvalidTotalWeight() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setCriteriaScoresJson("[{\"name\":\"Design\",\"score\":80.0,\"weight\":0.0}]");

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.INVALID_CRITERIA_WEIGHT, exception.getErrorCode());
    }

    @Test
    void gradeSubmission_Error_EditScore_MissingReason() {
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(90.0);
        request.setEditReason(null); // Missing reason

        Score existingScore = Score.builder()
                .submission(submission)
                .judge(judge)
                .scoreValue(80.0)
                .build();

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(submission));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.of(existingScore));

        AppException exception = assertThrows(AppException.class, () -> scoreService.gradeSubmission(request));
        assertEquals(ErrorCode.EDIT_REASON_REQUIRED, exception.getErrorCode());
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. PROMOTION & DEMOTION RULES (C-02, C-03)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void gradeSubmission_Promotion_WhenFullyGraded() {
        matrix.setTopN(1);
        // Setup next Round/Matrix
        Round nextRound = Round.builder()
                .name("Round 2")
                .orderIndex(2)
                .event(event)
                .build();
        nextRound.setId(21L);

        TrackRoundMatrix nextMatrix = TrackRoundMatrix.builder()
                .round(nextRound)
                .track(track)
                .topN(1)
                .build();
        nextMatrix.setId(41L);

        when(matrixRepository.findByTrackIdAndRoundOrderIndex(30L, 2)).thenReturn(Optional.of(nextMatrix));

        // Submissions to evaluate
        Team teamB = Team.builder().name("Team B").build();
        teamB.setId(51L);

        Submission subA = submission; // Team A, score will be 90
        subA.setCreatedAt(LocalDateTime.now().minusHours(2)); // submitted earlier

        Submission subB = Submission.builder()
                .team(teamB)
                .matrix(matrix)
                .isGraded(true)
                .build();
        subB.setId(61L);
        subB.setCreatedAt(LocalDateTime.now().minusHours(1)); // submitted later

        when(submissionRepository.findByMatrixId(40L)).thenReturn(Arrays.asList(subA, subB));

        // Score data
        Score scoreA = Score.builder().submission(subA).judge(judge).scoreValue(90.0).build();
        Score scoreB = Score.builder().submission(subB).judge(judge).scoreValue(80.0).build();

        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(scoreA));
        when(scoreRepository.findBySubmissionId(61L)).thenReturn(Collections.singletonList(scoreB));

        // Grade request
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(90.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(subA));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenReturn(scoreA);

        // Verification of promotion persistence
        when(submissionRepository.existsByTeamIdAndMatrixId(50L, 41L)).thenReturn(false);

        scoreService.gradeSubmission(request);

        // Verify Team A (highest score) promoted
        verify(submissionRepository, times(1)).save(argThat(sub -> 
            sub.getTeam().getId().equals(50L) && sub.getMatrix().getId().equals(41L) && !sub.getIsGraded()
        ));
        // Verify Team B (lower score) NOT promoted
        verify(submissionRepository, never()).save(argThat(sub -> 
            sub.getTeam().getId().equals(51L) && sub.getMatrix().getId().equals(41L)
        ));
    }

    @Test
    void gradeSubmission_Promotion_TieBreaker() {
        matrix.setTopN(1);
        // Setup next Round/Matrix
        Round nextRound = Round.builder()
                .name("Round 2")
                .orderIndex(2)
                .event(event)
                .build();
        nextRound.setId(21L);

        TrackRoundMatrix nextMatrix = TrackRoundMatrix.builder()
                .round(nextRound)
                .track(track)
                .topN(1) // topN is 1
                .build();
        nextMatrix.setId(41L);

        when(matrixRepository.findByTrackIdAndRoundOrderIndex(30L, 2)).thenReturn(Optional.of(nextMatrix));

        // Submissions to evaluate - BOTH have average score of 90.0
        Team teamB = Team.builder().name("Team B").build();
        teamB.setId(51L);

        Submission subA = submission; // Team A
        subA.setCreatedAt(LocalDateTime.now().minusHours(2)); // submitted earlier

        Submission subB = Submission.builder()
                .team(teamB)
                .matrix(matrix)
                .isGraded(true)
                .build();
        subB.setId(61L);
        subB.setCreatedAt(LocalDateTime.now().minusHours(1)); // submitted later

        when(submissionRepository.findByMatrixId(40L)).thenReturn(Arrays.asList(subA, subB));

        Score scoreA = Score.builder().submission(subA).judge(judge).scoreValue(90.0).build();
        Score scoreB = Score.builder().submission(subB).judge(judge).scoreValue(90.0).build();

        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(scoreA));
        when(scoreRepository.findBySubmissionId(61L)).thenReturn(Collections.singletonList(scoreB));

        // Grade request
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(90.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(subA));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenReturn(scoreA);

        // Team A promoted
        when(submissionRepository.existsByTeamIdAndMatrixId(50L, 41L)).thenReturn(false);

        scoreService.gradeSubmission(request);

        // Verify Team A (submitted earlier) promoted
        verify(submissionRepository, times(1)).save(argThat(sub -> 
            sub.getTeam().getId().equals(50L) && sub.getMatrix().getId().equals(41L)
        ));
        // Verify Team B (submitted later) NOT promoted
        verify(submissionRepository, never()).save(argThat(sub -> 
            sub.getTeam().getId().equals(51L) && sub.getMatrix().getId().equals(41L)
        ));
    }

    @Test
    void gradeSubmission_Demotion_DemoteOutOfTopN() {
        matrix.setTopN(1);
        // Setup next Round/Matrix
        Round nextRound = Round.builder()
                .name("Round 2")
                .orderIndex(2)
                .event(event)
                .build();
        nextRound.setId(21L);

        TrackRoundMatrix nextMatrix = TrackRoundMatrix.builder()
                .round(nextRound)
                .track(track)
                .topN(1) // topN is 1
                .build();
        nextMatrix.setId(41L);

        when(matrixRepository.findByTrackIdAndRoundOrderIndex(30L, 2)).thenReturn(Optional.of(nextMatrix));

        // Submissions to evaluate
        Team teamB = Team.builder().name("Team B").build();
        teamB.setId(51L);

        Submission subA = submission; // Team A
        subA.setCreatedAt(LocalDateTime.now().minusHours(2));

        Submission subB = Submission.builder()
                .team(teamB)
                .matrix(matrix)
                .isGraded(true)
                .build();
        subB.setId(61L);
        subB.setCreatedAt(LocalDateTime.now().minusHours(1));

        // Mock list of submissions
        when(submissionRepository.findByMatrixId(40L)).thenReturn(Arrays.asList(subA, subB));

        // Score values: Team B has 95.0, Team A has 90.0 -> Team B is now #1, Team A drops to #2
        Score scoreA = Score.builder().submission(subA).judge(judge).scoreValue(90.0).build();
        Score scoreB = Score.builder().submission(subB).judge(judge).scoreValue(95.0).build();

        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(scoreA));
        when(scoreRepository.findBySubmissionId(61L)).thenReturn(Collections.singletonList(scoreB));

        // Setup existing next round submissions: Team A is already there (was previously promoted)
        Submission nextRoundSubA = Submission.builder()
                .team(team)
                .matrix(nextMatrix)
                .fileUrl(null) // hasn't submitted yet
                .isGraded(false) // hasn't been graded yet
                .build();
        nextRoundSubA.setId(70L);

        when(submissionRepository.findByMatrixId(41L)).thenReturn(Collections.singletonList(nextRoundSubA));

        // Grade request
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(90.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(subA));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenReturn(scoreA);

        scoreService.gradeSubmission(request);

        // Verify Team A is demoted (deleted from next matrix)
        verify(submissionRepository, times(1)).delete(nextRoundSubA);

        // Verify Team B is promoted (saved as placeholder)
        verify(submissionRepository, times(1)).save(argThat(sub -> 
            sub.getTeam().getId().equals(51L) && sub.getMatrix().getId().equals(41L)
        ));
    }

    @Test
    void gradeSubmission_Demotion_SkipDemotionIfSubmittedOrGraded() {
        matrix.setTopN(1);
        // Setup next Round/Matrix
        Round nextRound = Round.builder()
                .name("Round 2")
                .orderIndex(2)
                .event(event)
                .build();
        nextRound.setId(21L);

        TrackRoundMatrix nextMatrix = TrackRoundMatrix.builder()
                .round(nextRound)
                .track(track)
                .topN(1) // topN is 1
                .build();
        nextMatrix.setId(41L);

        when(matrixRepository.findByTrackIdAndRoundOrderIndex(30L, 2)).thenReturn(Optional.of(nextMatrix));

        // Submissions to evaluate
        Team teamB = Team.builder().name("Team B").build();
        teamB.setId(51L);

        Submission subA = submission; // Team A
        subA.setCreatedAt(LocalDateTime.now().minusHours(2));

        Submission subB = Submission.builder()
                .team(teamB)
                .matrix(matrix)
                .isGraded(true)
                .build();
        subB.setId(61L);
        subB.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(submissionRepository.findByMatrixId(40L)).thenReturn(Arrays.asList(subA, subB));

        // Score values: Team B has 95.0, Team A has 90.0 -> Team B is #1, Team A drops to #2
        Score scoreA = Score.builder().submission(subA).judge(judge).scoreValue(90.0).build();
        Score scoreB = Score.builder().submission(subB).judge(judge).scoreValue(95.0).build();

        when(scoreRepository.findBySubmissionId(60L)).thenReturn(Collections.singletonList(scoreA));
        when(scoreRepository.findBySubmissionId(61L)).thenReturn(Collections.singletonList(scoreB));

        // Setup existing next round submissions: Team A is already there and HAS submitted a file
        Submission nextRoundSubA = Submission.builder()
                .team(team)
                .matrix(nextMatrix)
                .fileUrl("http://github.com/team-a/round-2-submission") // already submitted
                .isGraded(false)
                .build();
        nextRoundSubA.setId(70L);

        when(submissionRepository.findByMatrixId(41L)).thenReturn(Collections.singletonList(nextRoundSubA));

        // Grade request
        ScoreRequest request = new ScoreRequest();
        request.setSubmissionId(60L);
        request.setScoreValue(90.0);

        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(judge));
        when(submissionRepository.findById(60L)).thenReturn(Optional.of(subA));
        when(scoreRepository.findBySubmissionIdAndJudgeId(60L, 1L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenReturn(scoreA);

        scoreService.gradeSubmission(request);

        // Verify Team A is NOT demoted (delete is NOT called because fileUrl is present)
        verify(submissionRepository, never()).delete(nextRoundSubA);

        // Verify Team B is promoted (saved as placeholder)
        verify(submissionRepository, times(1)).save(argThat(sub -> 
            sub.getTeam().getId().equals(51L) && sub.getMatrix().getId().equals(41L)
        ));
    }
}
