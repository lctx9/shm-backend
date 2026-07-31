-- ============================================================================
-- SCRIPT TẠO DỮ LIỆU TEST CHO 2 KỊCH BẢN:
-- Kịch bản 1: Thăng vòng từ Round 1 sang Round 2 / Chung kết (Event 2 - Matrix 1, 2, 3)
-- Kịch bản 2: Vòng Chung kết Công bố kết quả & Xếp hạng Leaderboard (Event 4 - Matrix 17)
-- ============================================================================

-- 1. Đảm bảo trạng thái cho KỊCH BẢN 1: Thăng vòng từ Round 1 (Event 2 - Matrix 1, 2, 3)
-- Mở lại trạng thái chưa công bố cho Matrix 1, 2, 3 để test nhiều lần
UPDATE track_round_matrix
SET is_published = false,
    submission_start_date = now() - interval '4 hours',
    submission_deadline = now() - interval '1 hour',
    grading_deadline = now() + interval '2 hours',
    topn = 1
WHERE id IN (1, 2, 3);

-- Đảm bảo bài nộp Vòng 1 của Event 2 đã đầy đủ và được chấm điểm 100%
UPDATE submissions
SET is_graded = true,
    score = CASE id
        WHEN 1 THEN 92.5 -- Đội 4 (Code Forge) - HẠNG 1 Track A -> THẮNG VÒNG
        WHEN 2 THEN 78.0 -- Đội 5 (Pixel Pilots) - HẠNG 2 Track A
        WHEN 3 THEN 89.0 -- Đội 6 (AI Orbit) - HẠNG 1 Track B -> THẮNG VÒNG
        WHEN 4 THEN 81.5 -- Đội 7 (Data Sparks) - HẠNG 2 Track B
        WHEN 5 THEN 94.0 -- Đội 8 (Cloud Nine) - HẠNG 1 Track C -> THẮNG VÒNG
        ELSE score
    END,
    feedback = 'Tất cả giám khảo đã hoàn tất chấm điểm Vòng 1.'
WHERE id IN (1, 2, 3, 4, 5);


-- 2. Đảm bảo trạng thái cho KỊCH BẢN 2: Vòng Chung Kết Công bố & Xếp hạng (Event 4 - Matrix 17)
-- Mở lại trạng thái chưa công bố cho Matrix 17 (Chung kết) và Event 4
UPDATE track_round_matrix
SET is_published = false,
    submission_start_date = now() - interval '5 hours',
    submission_deadline = now() - interval '1 hour',
    grading_deadline = now() + interval '2 hours'
WHERE id = 17;

UPDATE events
SET results_published = false
WHERE id = 4;

-- Bài nộp Vòng Chung kết (Matrix 17) đã chấm điểm xong 100% với điểm số xếp hạng rõ ràng
UPDATE submissions
SET is_graded = true,
    score = CASE id
        WHEN 18 THEN 96.5 -- Đội 13 (Final Alpha) - GIẢI NHẤT / HẠNG 1
        WHEN 19 THEN 91.0 -- Đội 15 (Final Gamma) - GIẢI NHÌ / HẠNG 2
        ELSE score
    END,
    feedback = 'Bài làm Chung kết xuất sắc, đáp ứng đầy đủ tiêu chí của Ban Giám Khảo.'
WHERE id IN (18, 19);

-- Đảm bảo bảng scores lưu điểm từ các giám khảo tương ứng
INSERT INTO scores (submission_id, judge_id, score_value, criteria_scores_json, comment, created_at, updated_at)
SELECT 18, 10, 97.0, '[{"id":"problem","score":98},{"id":"technical","score":96}]', 'Xuất sắc! Ý tưởng và demo thương mại hóa cao.', now(), now()
ON CONFLICT DO NOTHING;

INSERT INTO scores (submission_id, judge_id, score_value, criteria_scores_json, comment, created_at, updated_at)
SELECT 18, 11, 96.0, '[{"id":"problem","score":96},{"id":"technical","score":96}]', 'Demo mượt mà, tính bảo mật cao.', now(), now()
ON CONFLICT DO NOTHING;

INSERT INTO scores (submission_id, judge_id, score_value, criteria_scores_json, comment, created_at, updated_at)
SELECT 19, 10, 92.0, '[{"id":"problem","score":92},{"id":"technical","score":92}]', 'Giải pháp ổn định, giao diện đẹp.', now(), now()
ON CONFLICT DO NOTHING;

INSERT INTO scores (submission_id, judge_id, score_value, criteria_scores_json, comment, created_at, updated_at)
SELECT 19, 11, 90.0, '[{"id":"problem","score":90},{"id":"technical","score":90}]', 'Tốt, hoàn thành đúng hạn.', now(), now()
ON CONFLICT DO NOTHING;
