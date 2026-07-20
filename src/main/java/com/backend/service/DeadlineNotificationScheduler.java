package com.backend.service;

import com.backend.entity.TrackRoundMatrix;
import com.backend.entity.Notification;
import com.backend.entity.User;
import com.backend.repository.TrackRoundMatrixRepository;
import com.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineNotificationScheduler {

    private final TrackRoundMatrixRepository matrixRepository;
    private final NotificationRepository notificationRepository;

    // Chạy mỗi 30 giây để kiểm tra và gửi thông báo hết hạn nộp bài cho giám khảo
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void checkAndNotifySubmissionDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        List<TrackRoundMatrix> endedMatrices = matrixRepository.findEndedMatricesToNotify(now);

        for (TrackRoundMatrix matrix : endedMatrices) {
            if (matrix.getJudges() != null && !matrix.getJudges().isEmpty()) {
                String roundName = matrix.getRound() != null ? matrix.getRound().getName() : "Không rõ vòng";
                String trackName = matrix.getTrack() == null ? "Chung kết" : matrix.getTrack().getName();
                
                for (User judge : matrix.getJudges()) {
                    Notification notification = Notification.builder()
                            .title("Bắt đầu chấm bài: Vòng " + roundName + " (" + trackName + ")")
                            .body("Thời gian nộp bài dự thi đã kết thúc hoàn toàn. Kính mời Giám khảo tiến hành đánh giá và chấm điểm các bài nộp.")
                            .recipient(judge)
                            .actionUrl("/submissions")
                            .build();
                    notificationRepository.save(notification);
                }
            }
            matrix.setDeadlineNotified(true);
            matrixRepository.save(matrix);
        }
    }
}
