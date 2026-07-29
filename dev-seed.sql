-- SEAL Hackathon comprehensive seed script
-- Login password for ALL users: 123456

BEGIN;

TRUNCATE TABLE
    audit_logs,
    chat_messages,
    notifications,
    notification_reads,
    scores,
    submissions,
    prizes,
    matrix_judges,
    matrix_mentors,
    track_round_matrix,
    team_members,
    team_join_requests,
    teams,
    rounds,
    tracks,
    users,
    events
RESTART IDENTITY CASCADE;

-- 1. USERS
-- Admin & Coordinator & Judges & Mentors
INSERT INTO users (id, created_at, updated_at, full_name, email, password, role, status, is_fpt_student, university_name) VALUES
    (1, now(), now(), 'Admin SEAL', 'admin@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'ADMIN', 'APPROVED', false, 'SEAL Admin HQ'),
    (2, now(), now(), 'Coordinator Linh Nguyen', 'coordinator@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'COORDINATOR', 'APPROVED', false, 'FPT University'),
    (3, now(), now(), 'Giám Khảo Minh Trần', 'judge1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'JUDGE', 'APPROVED', false, 'SEAL Partner'),
    (4, now(), now(), 'Giám Khảo Hạnh Phạm', 'judge2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'JUDGE', 'APPROVED', false, 'Tech Mentor Network'),
    (5, now(), now(), 'Mentor Bảo Võ', 'mentor1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'MENTOR', 'APPROVED', false, 'Software Guild'),
    (6, now(), now(), 'Mentor Nhi Lê', 'mentor2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'MENTOR', 'APPROVED', false, 'AI Lab');

-- Team Leaders & Members
INSERT INTO users (id, created_at, updated_at, full_name, email, password, role, status, student_id, is_fpt_student, university_name) VALUES
    -- Event 1 Teams
    (7, now(), now(), 'Leader Alpha', 'leader.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170001', true, 'Đại học FPT'),
    (8, now(), now(), 'Member Alpha 1', 'member1.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170002', true, 'Đại học FPT'),
    (9, now(), now(), 'Leader Beta', 'leader.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170003', true, 'Đại học FPT'),
    (10, now(), now(), 'Member Beta 1', 'member1.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170004', true, 'Đại học FPT'),
    (11, now(), now(), 'Leader Gamma', 'leader.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170005', true, 'Đại học FPT'),
    (12, now(), now(), 'Member Gamma 1', 'member1.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170006', true, 'Đại học FPT'),

    -- Event 2 Teams
    (13, now(), now(), 'Leader Delta', 'leader.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170007', true, 'Đại học FPT'),
    (14, now(), now(), 'Member Delta 1', 'member1.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170008', true, 'Đại học FPT'),
    (15, now(), now(), 'Leader Epsilon', 'leader.epsilon@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170009', true, 'Đại học FPT'),
    (16, now(), now(), 'Member Epsilon 1', 'member1.epsilon@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170010', true, 'Đại học FPT'),
    (17, now(), now(), 'Leader Zeta', 'leader.zeta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170011', true, 'Đại học FPT'),
    (18, now(), now(), 'Member Zeta 1', 'member1.zeta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170012', true, 'Đại học FPT'),

    -- Event 3 Teams
    (19, now(), now(), 'Leader Titan', 'leader.titan@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170013', true, 'Đại học FPT'),
    (20, now(), now(), 'Member Titan 1', 'member1.titan@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170014', true, 'Đại học FPT'),
    (21, now(), now(), 'Leader Phoenix', 'leader.phoenix@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170015', true, 'Đại học FPT'),
    (22, now(), now(), 'Member Phoenix 1', 'member1.phoenix@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'AI170016', true, 'Đại học FPT'),
    (23, now(), now(), 'Leader Apex', 'leader.apex@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170017', true, 'Đại học FPT'),
    (24, now(), now(), 'Member Apex 1', 'member1.apex@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'USER', 'APPROVED', 'SE170018', true, 'Đại học FPT');


-- 2. EVENTS
INSERT INTO events (
    id, created_at, updated_at, name, season, year, reg_start_date, reg_end_date,
    event_start_date, event_end_date, default_submission_deadline, round_count,
    structure_initialized, submission_form_schema, competition_rules, is_active, results_published, ended_early
) VALUES
-- Event 1: Test R1 -> R2 (Round 1 fully graded)
(
    1, now(), now(), 'SEAL Test 1: Sơ loại sang Bán kết (R1 -> R2)', 'SPRING', 2026,
    '2026-07-01 08:00:00', '2026-08-30 23:59:00', '2026-07-20 08:00:00', '2026-09-30 18:00:00', '2026-07-28 23:59:00',
    2, true,
    $$[{"id":"projectName","label":"Tên dự án","type":"text","required":true},{"id":"repoUrl","label":"Link Repo","type":"url","required":true}]$$,
    'Vui lòng nộp bài đúng hạn.', true, false, false
),
-- Event 2: Test R2 -> Final (Round 2 fully graded)
(
    2, now(), now(), 'SEAL Test 2: Bán kết sang Chung kết (R2 -> Final)', 'SUMMER', 2026,
    '2026-07-01 08:00:00', '2026-08-30 23:59:00', '2026-07-15 08:00:00', '2026-09-30 18:00:00', '2026-07-28 23:59:00',
    3, true,
    $$[{"id":"projectName","label":"Tên dự án","type":"text","required":true},{"id":"repoUrl","label":"Link Repo","type":"url","required":true}]$$,
    'Vui lòng nộp bài đúng hạn.', true, false, false
),
-- Event 3: Test Final Round (Chung kết fully graded)
(
    3, now(), now(), 'SEAL Test 3: Vòng Chung Kết & Kết thúc sớm', 'FALL', 2026,
    '2026-07-01 08:00:00', '2026-08-30 23:59:00', '2026-07-10 08:00:00', '2026-09-30 18:00:00', '2026-07-28 23:59:00',
    3, true,
    $$[{"id":"projectName","label":"Tên dự án","type":"text","required":true},{"id":"repoUrl","label":"Link Repo","type":"url","required":true}]$$,
    'Vui lòng nộp bài đúng hạn.', true, false, false
);

-- 3. TRACKS
INSERT INTO tracks (id, created_at, updated_at, name, description, event_id) VALUES
    (1, now(), now(), 'Software Engineering', 'Phần mềm & Hệ thống Web', 1),
    (2, now(), now(), 'AI Application', 'Ứng dụng Trí tuệ Nhân tạo', 2),
    (3, now(), now(), 'Global Innovation', 'Giải pháp Đổi mới Toàn cầu', 3);

-- 4. ROUNDS
INSERT INTO rounds (id, created_at, updated_at, name, order_index, event_id) VALUES
    -- Event 1 Rounds
    (1, now(), now(), 'Vòng 1 - Sơ Loại', 1, 1),
    (2, now(), now(), 'Vòng 2 - Bán Kết', 2, 1),

    -- Event 2 Rounds
    (3, now(), now(), 'Vòng 1 - Sơ Loại', 1, 2),
    (4, now(), now(), 'Vòng 2 - Bán Kết', 2, 2),
    (5, now(), now(), 'Vòng 3 - Chung Kết', 3, 2),

    -- Event 3 Rounds
    (6, now(), now(), 'Vòng 1 - Sơ Loại', 1, 3),
    (7, now(), now(), 'Vòng 2 - Bán Kết', 2, 3),
    (8, now(), now(), 'Vòng 3 - Chung Kết', 3, 3);

-- 5. TRACK ROUND MATRICES
INSERT INTO track_round_matrix (
    id, created_at, updated_at, track_id, round_id, guideline_url,
    submission_start_date, submission_deadline, is_published, topn,
    grading_duration_minutes, break_duration_minutes, scoring_criteria_json
) VALUES
    -- Event 1: Matrix 1 (R1 - ready to advance), Matrix 2 (R2 - waiting)
    (1, now(), now(), 1, 1, 'https://example.com/guide/r1.pdf', '2026-07-20 08:00:00', '2026-07-28 23:59:00', false, 2, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),
    (2, now(), now(), 1, 2, 'https://example.com/guide/r2.pdf', '2026-07-29 08:00:00', '2026-08-05 23:59:00', false, 1, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),

    -- Event 2: Matrix 3 (R1 - published), Matrix 4 (R2 - ready to advance to Final), Matrix 5 (Final - waiting)
    (3, now(), now(), 2, 3, 'https://example.com/guide/r1.pdf', '2026-07-15 08:00:00', '2026-07-20 23:59:00', true, 3, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),
    (4, now(), now(), 2, 4, 'https://example.com/guide/r2.pdf', '2026-07-21 08:00:00', '2026-07-28 23:59:00', false, 2, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),
    (5, now(), now(), 2, 5, 'https://example.com/guide/r3.pdf', '2026-07-29 08:00:00', '2026-08-10 23:59:00', false, 1, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),

    -- Event 3: Matrix 6 (R1 - published), Matrix 7 (R2 - published), Matrix 8 (Final Round - ready for publish & end early)
    (6, now(), now(), 3, 6, 'https://example.com/guide/r1.pdf', '2026-07-10 08:00:00', '2026-07-15 23:59:00', true, 3, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),
    (7, now(), now(), 3, 7, 'https://example.com/guide/r2.pdf', '2026-07-16 08:00:00', '2026-07-22 23:59:00', true, 3, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$),
    (8, now(), now(), 3, 8, 'https://example.com/guide/r3.pdf', '2026-07-23 08:00:00', '2026-07-28 23:59:00', false, 1, 10, 5,
    $$[{"id":"idea","label":"Ý tưởng","maxScore":100,"weight":50},{"id":"code","label":"Code","maxScore":100,"weight":50}]$$);

-- Assign Judges to Matrices
INSERT INTO matrix_judges (matrix_id, judge_id) VALUES
    (1, 3), (1, 4),
    (2, 3), (2, 4),
    (3, 3), (3, 4),
    (4, 3), (4, 4),
    (5, 3), (5, 4),
    (6, 3), (6, 4),
    (7, 3), (7, 4),
    (8, 3), (8, 4);

-- 6. TEAMS
INSERT INTO teams (id, created_at, updated_at, name, description, type, track_id, event_id) VALUES
    -- Event 1 Teams (3 teams)
    (1, now(), now(), 'Alpha Builders', 'Dự án Nền tảng Học tập thông minh', 'PUBLIC', 1, 1),
    (2, now(), now(), 'Beta Code', 'Công cụ phân tích source code tự động', 'PUBLIC', 1, 1),
    (3, now(), now(), 'Gamma System', 'Hệ thống tự động hóa vận hành', 'PUBLIC', 1, 1),

    -- Event 2 Teams (3 teams)
    (4, now(), now(), 'Delta AI', 'Trợ lý AI hỗ trợ nghiên cứu', 'PUBLIC', 2, 2),
    (5, now(), now(), 'Epsilon Vision', 'Nhận diện hình ảnh y tế bằng AI', 'PUBLIC', 2, 2),
    (6, now(), now(), 'Zeta Mind', 'Phân tích cảm xúc dữ liệu lớn', 'PUBLIC', 2, 2),

    -- Event 3 Teams (3 teams)
    (7, now(), now(), 'Titan Group', 'Nền tảng Quản trị chuỗi cung ứng', 'PUBLIC', 3, 3),
    (8, now(), now(), 'Phoenix Lab', 'Giải pháp Năng lượng xanh thông minh', 'PUBLIC', 3, 3),
    (9, now(), now(), 'Apex Global', 'Hệ thống thanh toán toàn cầu', 'PUBLIC', 3, 3);

-- Team Members
INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role) VALUES
    (1, now(), now(), 1, 7, 'LEADER'), (2, now(), now(), 1, 8, 'MEMBER'),
    (3, now(), now(), 2, 9, 'LEADER'), (4, now(), now(), 2, 10, 'MEMBER'),
    (5, now(), now(), 3, 11, 'LEADER'), (6, now(), now(), 3, 12, 'MEMBER'),
    (7, now(), now(), 4, 13, 'LEADER'), (8, now(), now(), 4, 14, 'MEMBER'),
    (9, now(), now(), 5, 15, 'LEADER'), (10, now(), now(), 5, 16, 'MEMBER'),
    (11, now(), now(), 6, 17, 'LEADER'), (12, now(), now(), 6, 18, 'MEMBER'),
    (13, now(), now(), 7, 19, 'LEADER'), (14, now(), now(), 7, 20, 'MEMBER'),
    (15, now(), now(), 8, 21, 'LEADER'), (16, now(), now(), 8, 22, 'MEMBER'),
    (17, now(), now(), 9, 23, 'LEADER'), (18, now(), now(), 9, 24, 'MEMBER');

-- 7. SUBMISSIONS & FULL SCORES FOR READY MATRICES

-- Event 1: Submissions & Scores for Matrix 1 (Round 1) -> 3 teams fully graded!
INSERT INTO submissions (id, created_at, updated_at, team_id, matrix_id, file_url, score, is_graded, is_flagged, feedback) VALUES
    (1, now(), now(), 1, 1, 'https://github.com/seal/alpha-builders-r1', 92.5, true, false, 'Sản phẩm xuất sắc, giao diện đẹp.'),
    (2, now(), now(), 2, 1, 'https://github.com/seal/beta-code-r1', 86.0, true, false, 'Mã nguồn sạch, tính ứng dụng cao.'),
    (3, now(), now(), 3, 1, 'https://github.com/seal/gamma-system-r1', 78.5, true, false, 'Hoàn thành tốt phần cốt lõi.');

INSERT INTO scores (id, created_at, updated_at, submission_id, judge_id, score_value, comment) VALUES
    (1, now(), now(), 1, 3, 95.0, 'Rất ấn tượng với phần demo.'),
    (2, now(), now(), 1, 4, 90.0, 'Kiến trúc hệ thống chuẩn.'),
    (3, now(), now(), 2, 3, 88.0, 'Code tổ chức tốt.'),
    (4, now(), now(), 2, 4, 84.0, 'Cần bổ sung thêm unit test.'),
    (5, now(), now(), 3, 3, 80.0, 'Ý tưởng sáng tạo.'),
    (6, now(), now(), 3, 4, 77.0, 'Chức năng cơ bản hoàn thành.');


-- Event 2: Submissions & Scores for Matrix 4 (Round 2) -> 3 teams fully graded!
INSERT INTO submissions (id, created_at, updated_at, team_id, matrix_id, file_url, score, is_graded, is_flagged, feedback) VALUES
    (4, now(), now(), 4, 4, 'https://github.com/seal/delta-ai-r2', 94.0, true, false, 'Mô hình AI chính xác cao.'),
    (5, now(), now(), 5, 4, 'https://github.com/seal/epsilon-vision-r2', 89.5, true, false, 'Báo cáo thuyết phục.'),
    (6, now(), now(), 6, 4, 'https://github.com/seal/zeta-mind-r2', 82.0, true, false, 'Tiềm năng ứng dụng thực tế tốt.');

INSERT INTO scores (id, created_at, updated_at, submission_id, judge_id, score_value, comment) VALUES
    (7, now(), now(), 4, 3, 96.0, 'Thuật toán rất tối ưu.'),
    (8, now(), now(), 4, 4, 92.0, 'Demo mượt mà.'),
    (9, now(), now(), 5, 3, 90.0, 'Xử lý dữ liệu chuẩn.'),
    (10, now(), now(), 5, 4, 89.0, 'Pitching xuất sắc.'),
    (11, now(), now(), 6, 3, 83.0, 'Khả năng mở rộng tốt.'),
    (12, now(), now(), 6, 4, 81.0, 'Cần thêm bảo mật dữ liệu.');


-- Event 3: Submissions & Scores for Matrix 8 (Final Round) -> 3 teams fully graded!
INSERT INTO submissions (id, created_at, updated_at, team_id, matrix_id, file_url, score, is_graded, is_flagged, feedback) VALUES
    (7, now(), now(), 7, 8, 'https://github.com/seal/titan-group-final', 96.5, true, false, 'Quán quân xứng đáng, giải pháp xuất sắc toàn diện!'),
    (8, now(), now(), 8, 8, 'https://github.com/seal/phoenix-lab-final', 91.0, true, false, 'Á quân xuất sắc, tính thực tiễn cao.'),
    (9, now(), now(), 9, 8, 'https://github.com/seal/apex-global-final', 87.5, true, false, 'Quý quân, ý tưởng có tính đột phá.');

INSERT INTO scores (id, created_at, updated_at, submission_id, judge_id, score_value, comment) VALUES
    (13, now(), now(), 7, 3, 98.0, 'Giải pháp hoàn hảo ở trận chung kết.'),
    (14, now(), now(), 7, 4, 95.0, 'Kỹ thuật và thuyết trình xuất sắc.'),
    (15, now(), now(), 8, 3, 92.0, 'Thuyết phục hoàn toàn ban giám khảo.'),
    (16, now(), now(), 8, 4, 90.0, 'Mô hình kinh doanh khả thi.'),
    (17, now(), now(), 9, 3, 88.0, 'Sáng tạo và khác biệt.'),
    (18, now(), now(), 9, 4, 87.0, 'Trình bày chuyên nghiệp.');


-- 8. RESET SEQUENCES
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT max(id) FROM events));
SELECT setval(pg_get_serial_sequence('tracks', 'id'), (SELECT max(id) FROM tracks));
SELECT setval(pg_get_serial_sequence('rounds', 'id'), (SELECT max(id) FROM rounds));
SELECT setval(pg_get_serial_sequence('track_round_matrix', 'id'), (SELECT max(id) FROM track_round_matrix));
SELECT setval(pg_get_serial_sequence('teams', 'id'), (SELECT max(id) FROM teams));
SELECT setval(pg_get_serial_sequence('team_members', 'id'), (SELECT max(id) FROM team_members));
SELECT setval(pg_get_serial_sequence('submissions', 'id'), (SELECT max(id) FROM submissions));
SELECT setval(pg_get_serial_sequence('scores', 'id'), (SELECT max(id) FROM scores));

COMMIT;
