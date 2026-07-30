package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.entity.*;
import com.backend.repository.*;
import com.backend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/matrices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RoundAdvancementController {

    private final TrackRoundMatrixRepository matrixRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ScoreService scoreService;

    @PostMapping("/{matrixId}/publish-and-advance")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Transactional
    public ApiResponse<String> publishAndAdvanceRound(@PathVariable Long matrixId) {
        User currentUser = getCurrentUser();
        TrackRoundMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ma trận vòng đấu"));
        if (Boolean.TRUE.equals(matrix.getIsPublished())) {
            return ApiResponse.<String>builder()
                    .result("Kết quả vòng đấu này đã được công bố trước đó.")
                    .build();
        }

        validateReadyToPublish(matrix);
        return ApiResponse.<String>builder()
                .result(publishMatrix(matrix, currentUser))
                .build();
    }

    @PostMapping("/events/{eventId}/rounds/{roundOrder}/publish-and-advance")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Transactional
    public ApiResponse<String> publishAndAdvanceRound(
            @PathVariable Long eventId,
            @PathVariable Integer roundOrder) {
        User currentUser = getCurrentUser();
        List<TrackRoundMatrix> matrices = matrixRepository.findByRoundEventId(eventId).stream()
                .filter(matrix -> matrix.getRound() != null
                        && Objects.equals(matrix.getRound().getOrderIndex(), roundOrder))
                .toList();
        if (matrices.isEmpty()) {
            throw new RuntimeException("Không tìm thấy vòng đấu cần công bố");
        }

        List<TrackRoundMatrix> unpublished = matrices.stream()
                .filter(matrix -> !Boolean.TRUE.equals(matrix.getIsPublished()))
                .toList();
        if (unpublished.isEmpty()) {
            return ApiResponse.<String>builder()
                    .result("Tất cả bảng đấu trong vòng này đã được công bố trước đó.")
                    .build();
        }

        unpublished.forEach(this::validateReadyToPublish);
        List<String> results = unpublished.stream()
                .map(matrix -> publishMatrix(matrix, currentUser))
                .toList();
        return ApiResponse.<String>builder()
                .result(String.join(" ", results))
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại"));
    }

    private void validateReadyToPublish(TrackRoundMatrix matrix) {
        if (matrix.getSubmissionDeadline() != null
                && java.time.LocalDateTime.now().isBefore(matrix.getSubmissionDeadline())) {
            throw new RuntimeException("Chưa đến hạn kết thúc nộp bài của " + matrix.getRound().getName());
        }
        Long eventId = matrix.getRound() == null || matrix.getRound().getEvent() == null
                ? null
                : matrix.getRound().getEvent().getId();
        Integer roundOrder = matrix.getRound() == null ? null : matrix.getRound().getOrderIndex();
        if (eventId != null && roundOrder != null) {
            boolean hasUnpublishedPreviousRound = matrixRepository.findByRoundEventId(eventId).stream()
                    .anyMatch(other -> other.getRound() != null
                            && other.getRound().getOrderIndex() < roundOrder
                            && !Boolean.TRUE.equals(other.getIsPublished()));
            if (hasUnpublishedPreviousRound) {
                throw new RuntimeException("Phải công bố đầy đủ các vòng trước khi công bố " + matrix.getRound().getName());
            }
        }
        if (matrix.getTrack() != null && (matrix.getTopN() == null || matrix.getTopN() < 1)) {
            throw new RuntimeException("Chưa cấu hình Top N cho " + matrix.getRound().getName()
                    + " - " + matrix.getTrack().getName());
        }
        if (!scoreService.isMatrixFullyGraded(matrix)) {
            String label = matrix.getTrack() == null
                    ? matrix.getRound().getName()
                    : matrix.getRound().getName() + " - " + matrix.getTrack().getName();
            throw new RuntimeException(label + " chưa được tất cả giám khảo chấm xong");
        }
        if (!isFinalRound(matrix) && findNextMatrix(matrix) == null) {
            throw new RuntimeException("Chưa cấu hình vòng tiếp theo cho " + matrix.getRound().getName()
                    + " - " + matrix.getTrack().getName());
        }
    }

    private boolean isFinalRound(TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getRound() == null) return false;
        HackathonEvent event = matrix.getRound().getEvent();
        if (event != null && event.getRoundCount() != null) {
            return Objects.equals(matrix.getRound().getOrderIndex(), event.getRoundCount());
        }
        return false;
    }

    private String publishMatrix(TrackRoundMatrix matrix, User currentUser) {
        // Promote before locking the matrix so a failure rolls back the entire publication.
        scoreService.promoteTopTeamsWhenRoundIsComplete(matrix);

        TrackRoundMatrix nextMatrix = findNextMatrix(matrix);

        int breakDuration = matrix.getBreakDurationMinutes() != null ? matrix.getBreakDurationMinutes() : 5;
        java.time.LocalDateTime breakEnd = java.time.LocalDateTime.now().plusMinutes(breakDuration);
        matrix.setBreakEndTime(breakEnd);
        matrix.setIsPublished(true);
        matrixRepository.save(matrix);

        if (nextMatrix != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            nextMatrix.setSubmissionStartDate(now);
            int nextDuration = nextMatrix.getDurationMinutes() != null ? nextMatrix.getDurationMinutes() : 60;
            nextMatrix.setSubmissionDeadline(now.plusMinutes(nextDuration));
            nextMatrix.setBreakEndTime(breakEnd);
            matrixRepository.save(nextMatrix);
        }

        Set<Long> promotedTeamIds = new HashSet<>();
        if (nextMatrix != null) {
            List<Submission> nextSubmissions = submissionRepository.findByMatrixId(nextMatrix.getId());
            for (Submission s : nextSubmissions) {
                if (s.getTeam() != null) {
                    promotedTeamIds.add(s.getTeam().getId());
                }
            }
        }

        String currentRoundName = matrix.getRound() != null ? matrix.getRound().getName() : "Vòng đấu";
        boolean finalRound = isFinalRound(matrix);
        String nextRoundName = nextMatrix != null && nextMatrix.getRound() != null
                ? nextMatrix.getRound().getName()
                : null;

        if (finalRound && matrix.getJudges() != null) {
            for (User judge : matrix.getJudges()) {
                notificationRepository.save(Notification.builder()
                        .title("🏆 Kết quả & Bảng xếp hạng " + currentRoundName + " đã được công bố!")
                        .body("Coordinator đã chính thức công bố điểm số và bảng xếp hạng chung cuộc của giải đấu cho " + currentRoundName + ".")
                        .recipient(judge)
                        .sender(currentUser)
                        .actionUrl("/dashboard/grading")
                        .build());
            }
        }

        List<Submission> currentSubmissions = submissionRepository.findByMatrixId(matrix.getId());
        for (Submission sub : currentSubmissions) {
            Team team = sub.getTeam();
            if (team == null) continue;

            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            boolean isPromoted = promotedTeamIds.contains(team.getId());

            for (TeamMember m : members) {
                if (m.getUser() == null) continue;

                if (finalRound) {
                    notificationRepository.save(Notification.builder()
                            .title("🏆 Kết quả & Bảng xếp hạng " + currentRoundName + " đã được công bố!")
                            .body("Coordinator đã chính thức công bố điểm số và bảng xếp hạng chung cuộc của giải đấu cho " + currentRoundName + ". Nhấn để xem bảng xếp hạng!")
                            .recipient(m.getUser())
                            .sender(currentUser)
                            .actionUrl("/dashboard/leaderboard")
                            .build());
                } else if (isPromoted) {
                    String title = "🎉 Chúc mừng! Đội " + team.getName() + " đã lọt vào " + nextRoundName;
                    String body = "Chúc mừng đội " + team.getName() + " của bạn đã xuất sắc vượt qua " + currentRoundName + " và bước vào " + nextRoundName + "! Hãy nhấn vào đây để xem đề bài và nộp bài làm.";
                    notificationRepository.save(Notification.builder()
                            .title(title)
                            .body(body)
                            .recipient(m.getUser())
                            .sender(currentUser)
                            .actionUrl("/my-team")
                            .build());
                } else {
                    String title = "Kết quả " + currentRoundName + " - Đội " + team.getName();
                    String body = "Đã có kết quả chính thức của " + currentRoundName + ". Rất tiếc đội " + team.getName() + " chưa đủ điều kiện thăng hạng. Cảm ơn các bạn đã tham gia nhiệt tình!";
                    notificationRepository.save(Notification.builder()
                            .title(title)
                            .body(body)
                            .recipient(m.getUser())
                            .sender(currentUser)
                            .actionUrl("/my-team")
                            .build());
                }
            }
        }

        String auditReason = finalRound
                ? "CHỐT KẾT QUẢ CHUNG KẾT: Coordinator " + currentUser.getFullName() + " ("
                        + currentUser.getEmail() + ") đã chốt kết quả " + currentRoundName
                : "CÔNG BỐ KẾT QUẢ & MỞ VÒNG: Coordinator " + currentUser.getFullName() + " ("
                        + currentUser.getEmail() + ") đã công bố kết quả " + currentRoundName
                        + " và mở " + nextRoundName;
        AuditLog auditLog = AuditLog.builder()
                .judge(currentUser)
                .teamName("Toàn bộ giải đấu")
                .reason(auditReason)
                .build();
        auditLogRepository.save(auditLog);

        return finalRound
                ? "Đã chốt kết quả " + currentRoundName + " thành công!"
                : "Đã công bố kết quả " + currentRoundName + " và mở " + nextRoundName + " thành công!";
    }

    private TrackRoundMatrix findNextMatrix(TrackRoundMatrix matrix) {
        if (matrix == null || matrix.getTrack() == null || matrix.getRound() == null
                || matrix.getRound().getEvent() == null) {
            return null;
        }
        int nextOrder = matrix.getRound().getOrderIndex() + 1;
        Long eventId = matrix.getRound().getEvent().getId();
        return matrixRepository.findByTrackIdAndRoundOrderIndex(matrix.getTrack().getId(), nextOrder)
                .orElseGet(() -> matrixRepository
                        .findByRoundEventIdAndTrackIsNullAndRoundOrderIndex(eventId, nextOrder)
                        .orElse(null));
    }
}
