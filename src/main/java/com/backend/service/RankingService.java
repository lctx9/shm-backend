package com.backend.service;

import com.backend.entity.*;
import com.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TeamRoundRankingRepository rankingRepository;

    @Transactional
    public List<TeamRoundRanking> calculateAndExecuteRanking(UUID roundId) {
        // 1. Kiểm tra vòng thi tồn tại
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vòng thi yêu cầu!"));

        // 2. Lấy toàn bộ bài nộp của vòng thi này
        List<Submission> submissions = submissionRepository.findByRoundId(roundId);
        List<TeamRoundRanking> rankings = new ArrayList<>();

        // 3. Tự động tính tổng điểm có trọng số cho từng đội thi từ bài nộp
        for (Submission submission : submissions) {
            Team team = submission.getTeam();
            List<JudgeScore> scores = submission.getScores();

            double totalWeightedScore = 0.0;

            if (scores != null && !scores.isEmpty()) {
                // ĐÃ ĐỒNG BỘ: Nhóm điểm số theo thực thể EventScoringCriteria chuẩn xác từ JudgeScore
                Map<EventScoringCriteria, List<JudgeScore>> scoresByCriteria = scores.stream()
                        .collect(Collectors.groupingBy(JudgeScore::getCriteria));

                for (Map.Entry<EventScoringCriteria, List<JudgeScore>> entry : scoresByCriteria.entrySet()) {
                    EventScoringCriteria criteria = entry.getKey();
                    List<JudgeScore> judgeScoresList = entry.getValue();

                    // Tính điểm trung bình cộng của các giám khảo cho tiêu chí này
                    double avgScoreForCriteria = judgeScoresList.stream()
                            .mapToDouble(JudgeScore::getScore)
                            .average()
                            .orElse(0.0);

                    // Sử dụng trường weight đã có sẵn trong EventScoringCriteria của bạn
                    double weight = (criteria.getWeight() != null) ? criteria.getWeight() : 1.0;
                    totalWeightedScore += (avgScoreForCriteria * weight);
                }
            }

            // Tìm kiếm bản ghi xếp hạng cũ hoặc tạo mới nếu chưa tồn tại trong DB
            TeamRoundRanking ranking = rankingRepository.findByTeamIdAndRoundId(team.getId(), roundId)
                    .orElse(new TeamRoundRanking());

            ranking.setTeam(team);
            ranking.setRound(round);
            ranking.setTotalScore(totalWeightedScore);
            ranking.setAdvanced(false); // Reset trước khi xét duyệt Top N mới

            rankings.add(ranking);
        }

        if (rankings.isEmpty()) {
            return rankings;
        }

        // 4. Xếp hạng tổng toàn Event (Rank Overall)
        rankings.sort((r1, r2) -> Double.compare(r2.getTotalScore(), r1.getTotalScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRankOverall(i + 1);
        }

        // 5. Xếp hạng đội theo từng Track riêng biệt trong Round (Rank in Track)
        Map<Track, List<TeamRoundRanking>> rankingsByTrack = rankings.stream()
                .collect(Collectors.groupingBy(r -> r.getTeam().getTrack()));

        for (Map.Entry<Track, List<TeamRoundRanking>> entry : rankingsByTrack.entrySet()) {
            Track track = entry.getKey();
            List<TeamRoundRanking> trackRankings = entry.getValue();

            // Sắp xếp giảm dần điểm số trong nội bộ Track
            trackRankings.sort((r1, r2) -> Double.compare(r2.getTotalScore(), r1.getTotalScore()));
            for (int j = 0; j < trackRankings.size(); j++) {
                trackRankings.get(j).setRankInTrack(j + 1);
            }

            // 6. Tự động xác định đội thăng vòng (Top N) dựa trên cấu hình Vòng thi hoặc cấu hình riêng của Track
            int slots = round.getTopNAdvancement() != null ? round.getTopNAdvancement() :
                    (track.getAdvancementSlots() != null ? track.getAdvancementSlots() : 0);

            for (int k = 0; k < Math.min(slots, trackRankings.size()); k++) {
                trackRankings.get(k).setAdvanced(true);
            }
        }

        // 7. Lưu tất cả kết quả tính toán và xếp hạng mới nhất vào Database
        return rankingRepository.saveAll(rankings);
    }

    // GIỮ NGUYÊN: Tên hàm cũ không đổi để tránh conflict khi push Git
    public List<TeamRoundRanking> getRankingsByRound(UUID roundId) {
        return rankingRepository.findByRoundId(roundId);
    }
}