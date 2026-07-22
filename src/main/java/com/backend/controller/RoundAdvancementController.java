package com.backend.controller;

import com.backend.dto.response.ApiResponse;
import com.backend.entity.*;
import com.backend.repository.*;
import com.backend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PreAuthorize("hasRole('COORDINATOR') or hasRole('ADMIN')")
    public ApiResponse<String> publishAndAdvanceRound(@PathVariable Long matrixId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại"));

        TrackRoundMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ma trận vòng đấu"));

        // 1. Mark current matrix as published
        matrix.setIsPublished(true);
        matrixRepository.save(matrix);

        // 2. Perform Top N promotion to next round matrix
        scoreService.promoteTopTeamsWhenRoundIsComplete(matrix);

        // 3. Find next round matrix if exists
        int nextOrder = matrix.getRound() == null ? 2 : matrix.getRound().getOrderIndex() + 1;
        Long eventId = matrix.getRound() == null ? null : matrix.getRound().getEvent().getId();
        TrackRoundMatrix nextMatrix = (matrix.getTrack() == null || eventId == null)
                ? null
                : matrixRepository.findByTrackIdAndRoundOrderIndex(matrix.getTrack().getId(), nextOrder)
                        .orElseGet(() -> matrixRepository
                                .findByRoundEventIdAndTrackIsNullAndRoundOrderIndex(eventId, nextOrder)
                                .orElse(null));

        Set<Long> promotedTeamIds = new HashSet<>();
        if (nextMatrix != null) {
            List<Submission> nextSubmissions = submissionRepository.findByMatrixId(nextMatrix.getId());
            for (Submission s : nextSubmissions) {
                if (s.getTeam() != null) {
                    promotedTeamIds.add(s.getTeam().getId());
                }
            }
        }

        // 4. Send Notifications to Promoted & Unpromoted Teams
        String currentRoundName = matrix.getRound() != null ? matrix.getRound().getName() : "Vòng đấu";
        String nextRoundName = (nextMatrix != null && nextMatrix.getRound() != null) ? nextMatrix.getRound().getName() : "Vòng đấu tiếp theo";

        List<Submission> currentSubmissions = submissionRepository.findByMatrixId(matrixId);
        for (Submission sub : currentSubmissions) {
            Team team = sub.getTeam();
            if (team == null) continue;

            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
            boolean isPromoted = promotedTeamIds.contains(team.getId());

            for (TeamMember m : members) {
                if (m.getUser() == null) continue;

                if (isPromoted) {
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

        // 5. Save AuditLog
        String auditReason = "CÔNG BỐ KẾT QUẢ & MỞ VÒNG: Coordinator " + currentUser.getFullName() + " (" + currentUser.getEmail() + ") đã công bố kết quả " + currentRoundName + " và mở " + nextRoundName;
        AuditLog auditLog = AuditLog.builder()
                .judge(currentUser)
                .teamName("Toàn bộ giải đấu")
                .reason(auditReason)
                .build();
        auditLogRepository.save(auditLog);

        return ApiResponse.<String>builder()
                .result("Đã công bố kết quả " + currentRoundName + " và mở " + nextRoundName + " thành công!")
                .build();
    }
}
