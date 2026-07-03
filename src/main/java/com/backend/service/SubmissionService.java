package com.backend.service;

import com.backend.dto.DisqualifyRequest;
import com.backend.dto.SubmissionRequest;
import com.backend.entity.*;
import com.backend.entity.enums.SubmissionStatus;
import com.backend.entity.enums.TeamMemberRole;
import com.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Transactional
    public Submission submitOrUpdate(UUID userId, SubmissionRequest request) {
        // 1. Kiểm tra Round có tồn tại không
        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi yêu cầu!"));

        // 2. Kiểm tra thời hạn nộp bài (Deadline)
        if (round.getSubmissionDeadline() != null && LocalDateTime.now().isAfter(round.getSubmissionDeadline())) {
            throw new RuntimeException("Đã quá hạn chót nộp bài cho vòng thi này!");
        }

        // 3. Tìm xem User đang thuộc đội nào
        // Lưu ý: Dựa theo TeamMemberRepository.existsByUser để kiểm tra sự tồn tại
        // Ta cần tìm thực thể TeamMember của user để xác định Team và Vai trò
        // Hệ thống của bạn đang lấy Team qua TeamMember hoặc JoinRequest công khai
        // Ở đây giả định bạn có phương thức tìm kiếm Team từ User, ta sẽ lấy thông tin thông qua TeamMember

        // Để khớp với cơ chế bảo mật Header X-User-Id của bạn, ta xử lý lấy Team của User:
        // Cần giả định bạn có hàm hoặc ta tự viết logic lấy Team của User hiện tại:
        TeamMember memberInfo = teamMemberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bạn chưa tham gia bất kỳ đội thi nào!"));

        Team team = memberInfo.getTeam();

        // 4. RÀNG BUỘC QUY CHẾ: Chỉ có Team Leader mới có quyền nộp bài
        if (!memberInfo.getRole().equals(TeamMemberRole.LEADER)) {
            throw new RuntimeException("Quyền nộp bài hoặc chỉnh sửa bài nộp chỉ dành cho Trưởng nhóm (Team Leader)!");
        }

        // 5. Kiểm tra xem đội đã nộp bài cho vòng này chưa (Unique Constraint trong DB: team_id, round_id)
        // Ta tìm kiếm bài nộp cũ để cập nhật hoặc tạo mới
        Submission submission = submissionRepository.findByRoundId(round.getId()).stream()
                .filter(s -> s.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElse(new Submission());

        // Nếu là cập nhật, chặn không cho sửa nếu bài đã bị BTC hủy (DISQUALIFIED)
        if (submission.getStatus() == SubmissionStatus.DISQUALIFIED) {
            throw new RuntimeException("Bài nộp đã bị Ban tổ chức hủy bỏ vi phạm. Không thể chỉnh sửa!");
        }

        // 6. Cập nhật thông tin bài nộp
        submission.setTeam(team);
        submission.setRound(round);
        submission.setRepoUrl(request.getRepoUrl());
        submission.setDemoUrl(request.getDemoUrl());
        submission.setReportUrl(request.getReportUrl());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED); // Chuyển từ DRAFT sang SUBMITTED sau khi nhấn nộp

        // (Optional): Bạn có thể tích hợp lấy metadata JSON từ GitHub/GitLab tại đây
        // submission.setRepoMetadata("{ \"status\": \"verified\" }");

        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Submission getTeamSubmission(UUID userId, UUID roundId) {
        TeamMember memberInfo = teamMemberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc bất kỳ đội thi nào để xem bài nộp!"));

        UUID teamId = memberInfo.getTeam().getId();

        return submissionRepository.findByRoundId(roundId).stream()
                .filter(s -> s.getTeam().getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Đội của bạn chưa có bài nộp nào cho vòng đấu này!"));
    }

    @Transactional
    public Submission disqualifySubmission(DisqualifyRequest request) {
        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp yêu cầu!"));

        submission.setStatus(SubmissionStatus.DISQUALIFIED);
        submission.setDisqualificationReason(request.getReason());

        return submissionRepository.save(submission);
    }
}