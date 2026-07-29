package com.backend.service;

import com.backend.entity.AuditLog;
import com.backend.entity.HackathonEvent;
import com.backend.entity.Notification;
import com.backend.entity.Submission;
import com.backend.entity.Team;
import com.backend.entity.TeamMember;
import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.User;
import com.backend.entity.enums.MemberRole;
import com.backend.entity.enums.RoleType;
import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import com.backend.repository.AuditLogRepository;
import com.backend.repository.ChatMessageRepository;
import com.backend.repository.HackathonEventRepository;
import com.backend.repository.NotificationRepository;
import com.backend.repository.PrizeRepository;
import com.backend.repository.ScoreRepository;
import com.backend.repository.SubmissionRepository;
import com.backend.repository.TeamJoinRequestRepository;
import com.backend.repository.TeamMemberRepository;
import com.backend.repository.TeamRepository;
import com.backend.repository.TrackRepository;
import com.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamJoinRequestRepository teamJoinRequestRepository;
    @Mock private HackathonEventRepository eventRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private PrizeRepository prizeRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeamService teamService;

    private User actingJudge;
    private User otherJudge;
    private User teamMemberUser;
    private Team team;
    private Submission submission;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("judge@seal.dev", null, List.of())
        );

        actingJudge = User.builder()
                .fullName("Judge One")
                .email("judge@seal.dev")
                .role(RoleType.STAFF)
                .build();
        actingJudge.setId(1L);

        otherJudge = User.builder()
                .fullName("Judge Two")
                .email("judge2@seal.dev")
                .role(RoleType.STAFF)
                .build();
        otherJudge.setId(2L);

        teamMemberUser = User.builder()
                .fullName("Team Member")
                .email("member@seal.dev")
                .role(RoleType.USER)
                .build();
        teamMemberUser.setId(3L);

        HackathonEvent event = HackathonEvent.builder().name("SEAL Hackathon").build();
        event.setId(10L);
        team = Team.builder().name("Team A").event(event).build();
        team.setId(20L);

        TrackRoundMatrix matrix = TrackRoundMatrix.builder()
                .judges(new HashSet<>(List.of(actingJudge, otherJudge)))
                .build();
        matrix.setId(30L);
        submission = Submission.builder().team(team).matrix(matrix).fileUrl("https://example.com").build();
        submission.setId(40L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disqualifyTeamDirectly_AssignedJudgeNotifiesRelatedUsersAndPreservesHistory() {
        TeamMember member = TeamMember.builder()
                .team(team)
                .user(teamMemberUser)
                .role(MemberRole.MEMBER)
                .build();

        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(actingJudge));
        when(submissionRepository.findByTeamId(20L)).thenReturn(List.of(submission));
        when(teamMemberRepository.findByTeamId(20L)).thenReturn(List.of(member));
        when(prizeRepository.findByTeamId(20L)).thenReturn(List.of());

        teamService.disqualifyTeamDirectly(20L, "Gian lận");

        assertEquals("APPROVED", team.getDisqualificationStatus());
        assertEquals("Gian lận", team.getDisqualificationReason());
        assertEquals("judge@seal.dev", team.getDisqualifierEmail());
        verify(teamRepository).save(team);
        verify(auditLogRepository).save(argThat((AuditLog log) ->
                log.getScore() == null
                        && log.getJudge() == actingJudge
                        && "Team A".equals(log.getTeamName())
                        && log.getReason().contains("Judge One")
                        && log.getReason().contains("Gian lận")
        ));
        verify(notificationRepository).save(argThat((Notification notification) ->
                notification.getRecipient() == otherJudge
                        && notification.getBody().contains("Judge One")
                        && notification.getBody().contains("Gian lận")
        ));
        verify(notificationRepository).save(argThat((Notification notification) ->
                notification.getRecipient() == teamMemberUser
                        && notification.getBody().contains("Gian lận")
        ));

        verify(teamMemberRepository, never()).deleteAll(any());
        verify(submissionRepository, never()).delete(any());
        verify(scoreRepository, never()).deleteAll(any());
        verify(auditLogRepository, never()).deleteAll(any());
    }

    @Test
    void disqualifyTeamDirectly_UnassignedJudgeIsRejected() {
        TrackRoundMatrix matrix = TrackRoundMatrix.builder()
                .judges(new HashSet<>(List.of(otherJudge)))
                .build();
        submission.setMatrix(matrix);

        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("judge@seal.dev")).thenReturn(Optional.of(actingJudge));
        when(submissionRepository.findByTeamId(20L)).thenReturn(List.of(submission));

        AppException exception = assertThrows(
                AppException.class,
                () -> teamService.disqualifyTeamDirectly(20L, "Gian lận")
        );

        assertEquals(ErrorCode.JUDGE_NOT_ASSIGNED, exception.getErrorCode());
        verify(teamRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }
}
