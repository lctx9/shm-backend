-- SEAL Hackathon dev seed data
-- WARNING: This resets all application tables. Use only for local/dev database.
-- Login password for every seeded account: 123456

BEGIN;

TRUNCATE TABLE
    audit_logs,
    chat_messages,
    notifications,
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

INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
) VALUES
    (1, now(), now(), 'Admin SEAL', 'admin@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'SEAL Organization', null, null, null, 'ADMIN', 'APPROVED'),
    (2, now(), now(), 'Coordinator Linh Nguyen', 'coordinator@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'FPT University', null, null, null, 'COORDINATOR', 'APPROVED'),
    (3, now(), now(), 'Giám Khảo Minh Trần', 'judge1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'SEAL Partner', null, null, null, 'JUDGE', 'APPROVED'),
    (4, now(), now(), 'Giám Khảo Hạnh Phạm', 'judge2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'Tech Mentor Network', null, null, null, 'JUDGE', 'APPROVED'),
    (5, now(), now(), 'Mentor Bảo Võ', 'mentor1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'Software Guild', null, null, null, 'MENTOR', 'APPROVED'),
    (6, now(), now(), 'Mentor Nhi Lê', 'mentor2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'AI Lab', null, null, null, 'MENTOR', 'APPROVED'),

    -- Team Alpha (3 members)
    (7, now(), now(), 'Leader An Nguyễn', 'leader.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170001', true, 'Đại học FPT', null, 'https://example.com/cards/se170001.png', null, 'USER', 'APPROVED'),
    (8, now(), now(), 'Member Bình Trần', 'member.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170002', true, 'Đại học FPT', null, 'https://example.com/cards/se170002.png', null, 'USER', 'APPROVED'),
    (18, now(), now(), 'Member Cường Lê', 'member2.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170018', true, 'Đại học FPT', null, 'https://example.com/cards/se170018.png', null, 'USER', 'APPROVED'),

    -- Team Beta (3 members)
    (9, now(), now(), 'Leader Chi Phạm', 'leader.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170003', true, 'Đại học FPT', null, 'https://example.com/cards/ai170003.png', null, 'USER', 'APPROVED'),
    (10, now(), now(), 'Member Duy Lê', 'member.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170004', true, 'Đại học FPT', null, 'https://example.com/cards/ai170004.png', null, 'USER', 'APPROVED'),
    (19, now(), now(), 'Member Dũng Võ', 'member2.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170019', true, 'Đại học FPT', null, 'https://example.com/cards/ai170019.png', null, 'USER', 'APPROVED'),

    -- Team Gamma (3 members)
    (12, now(), now(), 'Leader Khoa Phan', 'leader.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170005', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Khoa', null, 'USER', 'APPROVED'),
    (13, now(), now(), 'Member Mai Ho', 'member.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170006', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Mai', null, 'USER', 'APPROVED'),
    (20, now(), now(), 'Member Giang Nguyễn', 'member2.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170020', true, 'Đại học FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Giang', null, 'USER', 'APPROVED'),

    -- Team Delta (3 members)
    (14, now(), now(), 'Leader Nam Do', 'leader.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170007', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Nam', null, 'USER', 'APPROVED'),
    (15, now(), now(), 'Member Oanh Bui', 'member.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170008', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Oanh', null, 'USER', 'APPROVED'),
    (21, now(), now(), 'Member Hà Trần', 'member2.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170021', true, 'Đại học FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Ha', null, 'USER', 'APPROVED'),

    -- Pending & Join request students
    (11, now(), now(), 'Pending Student', 'pending@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170099', true, 'Đại học FPT', null, 'https://example.com/cards/se170099.png', null, 'USER', 'PENDING'),
    (16, now(), now(), 'Student Join Request', 'join.request@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170088', true, 'Đại học FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Join', null, 'USER', 'APPROVED'),
    (17, now(), now(), 'Pending Upload Student', 'pending.upload@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170100', true, 'Đại học FPT', null, 'https://placehold.co/900x560/fef3c7/92400e?text=Pending+Student+Card', null, 'USER', 'PENDING');

-- EVENTS
INSERT INTO events (
    id, created_at, updated_at, name, season, year, reg_start_date, reg_end_date,
    event_start_date, event_end_date, default_submission_deadline, round_count,
    structure_initialized, submission_form_schema, competition_rules,
    rule_document_url, is_active, results_published
) VALUES
(
    1, now(), now(), 'SEAL Hackathon Spring 2026', 'SPRING', 2026,
    '2026-07-01 08:00:00', '2026-08-30 23:59:00',
    '2026-07-21 08:00:00', '2026-09-30 18:00:00',
    '2026-07-21 23:59:00', 2, true,
    $$[{"id":"projectName","label":"Ten du an","type":"text","required":true}]$$,
    $$1. Moi doi co 2-5 thanh vien.$$,
    'https://example.com/seal/rules-spring-2026.pdf',
    true, false
),
(
    2, now(), now(), 'SEAL Hackathon Fall 2025', 'FALL', 2025,
    '2025-09-01 08:00:00', '2025-09-15 23:59:00',
    '2025-09-20 08:00:00', '2025-09-28 18:00:00',
    '2025-09-26 23:59:00', 2, true,
    $$[{"id":"projectName","label":"Ten du an","type":"text","required":true}]$$,
    $$1. Moi doi co 2-5 thanh vien.$$,
    'https://example.com/seal/rules-fall-2025.pdf',
    false, true
),
(
    3, now(), now(), 'Green Tech Hackathon 2024', 'SPRING', 2024,
    '2024-03-01 08:00:00', '2024-03-15 23:59:00',
    '2024-03-20 08:00:00', '2024-03-28 18:00:00',
    '2024-03-26 23:59:00', 2, true,
    $$[{"id":"projectName","label":"Ten du an","type":"text","required":true}]$$,
    $$1. Moi doi co 2-5 thanh vien.$$,
    'https://example.com/seal/rules-green-2024.pdf',
    false, true
);

-- TRACKS
INSERT INTO tracks (id, created_at, updated_at, name, description, event_id, max_teams) VALUES
    (1, now(), now(), 'Software Engineering', 'Web, mobile, backend, platform and productivity tools', 1, NULL),
    (2, now(), now(), 'AI Application', 'AI-powered products, data apps and automation workflows', 1, NULL),
    (3, now(), now(), 'AI & ML Track 2025', 'Bảng Trí Tuệ Nhân Tạo 2025', 2, NULL),
    (4, now(), now(), 'Green Tech Track 2024', 'Bảng Năng Lượng Xanh 2024', 3, NULL);

-- ROUNDS
INSERT INTO rounds (id, created_at, updated_at, name, order_index, event_id) VALUES
    (1, now(), now(), 'Round 1 - Prototype', 1, 1),
    (2, now(), now(), 'Round 2 - Final Pitch', 2, 1),
    (3, now(), now(), 'Vòng Sơ Loại 2025', 1, 2),
    (4, now(), now(), 'Vòng Chung Kết Fall 2025', 2, 2),
    (5, now(), now(), 'Vòng Sơ Loại 2024', 1, 3),
    (6, now(), now(), 'Vòng Chung Kết GreenTech 2024', 2, 3);

-- TRACK ROUND MATRIX
INSERT INTO track_round_matrix (
    id, created_at, updated_at, track_id, round_id, guideline_url,
    submission_start_date, submission_deadline, topn, is_published, scoring_criteria_json
) VALUES
    (1, now(), now(), 1, 1, 'https://example.com/guidelines/se-r1.pdf', '2026-07-01 08:00:00', '2026-07-21 23:59:00', 1, true, $$[]$$),
    (2, now(), now(), 1, 2, 'https://example.com/guidelines/se-r2.pdf', '2026-07-22 08:00:00', '2026-07-28 23:59:00', 1, false, $$[]$$),
    (3, now(), now(), 2, 1, 'https://example.com/guidelines/ai-r1.pdf', '2026-07-01 08:00:00', '2026-07-21 23:59:00', 1, true, $$[]$$),
    (4, now(), now(), 2, 2, 'https://example.com/guidelines/ai-r2.pdf', '2026-07-22 08:00:00', '2026-07-28 23:59:00', 1, false, $$[]$$),
    (5, now(), now(), 3, 4, 'https://example.com/guidelines/fall-final.pdf', '2025-09-20 08:00:00', '2025-09-26 23:59:00', 1, true, $$[]$$),
    (6, now(), now(), 4, 6, 'https://example.com/guidelines/green-final.pdf', '2024-03-20 08:00:00', '2024-03-26 23:59:00', 1, true, $$[]$$);

INSERT INTO matrix_mentors (matrix_id, mentor_id) VALUES (1, 5), (2, 5), (3, 6), (4, 6);
INSERT INTO matrix_judges (matrix_id, judge_id) VALUES (1, 3), (1, 4), (2, 3), (2, 4), (3, 3), (3, 4), (4, 3), (4, 4);

-- TEAMS
INSERT INTO teams (
    id, created_at, updated_at, name, description, type, join_password,
    disqualification_status, disqualification_reason, disqualifier_email,
    rejection_reason, event_id, track_id
) VALUES
    (1, now(), now(), 'Alpha Builders', 'Nen tang quan ly du an hackathon', 'PUBLIC', '123456', 'NOT_DISQUALIFIED', null, null, null, 1, 1),
    (2, now(), now(), 'Beta Vision', 'Tro ly AI ho tro giam khảo', 'PUBLIC', '123456', 'NOT_DISQUALIFIED', null, null, null, 1, 2),
    (3, now(), now(), 'Gamma Flow', 'San gia dich vu mentor', 'PUBLIC', '123456', 'NOT_DISQUALIFIED', null, null, null, 1, 1),
    (4, now(), now(), 'Delta Mind', 'He thong tu dong phan tich code', 'PUBLIC', '123456', 'NOT_DISQUALIFIED', null, null, null, 1, 2);

-- TEAM MEMBERS
INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role) VALUES
    (1, now(), now(), 1, 7, 'LEADER'), (2, now(), now(), 1, 8, 'MEMBER'), (11, now(), now(), 1, 18, 'MEMBER'),
    (3, now(), now(), 2, 9, 'LEADER'), (4, now(), now(), 2, 10, 'MEMBER'), (12, now(), now(), 2, 19, 'MEMBER'),
    (5, now(), now(), 3, 12, 'LEADER'), (6, now(), now(), 3, 13, 'MEMBER'), (13, now(), now(), 3, 20, 'MEMBER'),
    (7, now(), now(), 4, 14, 'LEADER'), (8, now(), now(), 4, 15, 'MEMBER'), (14, now(), now(), 4, 21, 'MEMBER');

-- PRIZES
INSERT INTO prizes (id, created_at, updated_at, name, description, event_id, team_id) VALUES
    -- Event 1 Prizes (Spring 2026)
    (1, now(), now(), '🏆 Giải Nhất - Quán Quân Spring 2026', '50.000.000 VNĐ + Cúp Vàng', 1, 2),
    (2, now(), now(), '🥈 Giải Nhì - Á Quân Spring 2026', '30.000.000 VNĐ + Huy Chương Bạc', 1, 1),
    (3, now(), now(), '🥉 Giải Ba - Hạng Ba Spring 2026', '15.000.000 VNĐ + Huy Chương Đồng', 1, 3),

    -- Event 2 Prizes (Fall 2025)
    (4, now(), now(), '🏆 Giải Nhất - Quán Quân AI Fall 2025', '40.000.000 VNĐ + Cúp Vàng', 2, 1),
    (5, now(), now(), '🥈 Giải Nhì - Á Quân AI Fall 2025', '20.000.000 VNĐ + Huy Chương Bạc', 2, 2),
    (6, now(), now(), '🥉 Giải Ba - Hạng Ba AI Fall 2025', '10.000.000 VNĐ + Huy Chương Đồng', 2, 3),

    -- Event 3 Prizes (Green Tech 2024 - 3 configured prizes, but only 2 teams competed!)
    (7, now(), now(), '🏆 Giải Nhất - Quán Quân GreenTech 2024', '35.000.000 VNĐ + Cúp Vàng', 3, 4),
    (8, now(), now(), '🥈 Giải Nhì - Á Quân GreenTech 2024', '18.000.000 VNĐ + Huy Chương Bạc', 3, 1),
    (9, now(), now(), '🥉 Giải Ba - Khuyến Khích GreenTech 2024', '8.000.000 VNĐ + Huy Chương Đồng', 3, null);

-- SUBMISSIONS
INSERT INTO submissions (
    id, created_at, updated_at, team_id, matrix_id, file_url, is_flagged,
    flag_reason, score, feedback, criteria_scores_json, is_graded
) VALUES
    -- Event 1 Final Round (Matrix 2 & 4) - Ready for Coordinator to Publish!
    (101, now(), now(), 2, 4, 'https://github.com/seal-demo/beta-vision/final-demo', false, null, 95.5, 'Giải pháp AI vượt trội, ứng dụng cao', $$[]$$, true),
    (102, now(), now(), 1, 2, 'https://github.com/seal-demo/alpha-builders/final-demo', false, null, 89.0, 'Prototype hoàn thiện, giao diện mượt', $$[]$$, true),
    (103, now(), now(), 3, 2, 'https://github.com/seal-demo/gamma-flow/final-demo', false, null, 83.5, 'Ý tưởng sáng tạo, pitching ấn tượng', $$[]$$, true),

    -- Event 2 Final Round (Matrix 5) - Already Published!
    (201, now(), now(), 1, 5, 'https://github.com/seal-demo/alpha-ai-2025', false, null, 97.0, 'Mô hình AI xuất sắc nhất mùa giải 2025', $$[]$$, true),
    (202, now(), now(), 2, 5, 'https://github.com/seal-demo/beta-ai-2025', false, null, 91.5, 'Ý tưởng thực tiễn cao', $$[]$$, true),
    (203, now(), now(), 3, 5, 'https://github.com/seal-demo/gamma-ai-2025', false, null, 86.0, 'Tiềm năng phát triển lớn', $$[]$$, true),

    -- Event 3 Final Round (Matrix 6) - 2 Teams, 3 Configured Prizes!
    (301, now(), now(), 4, 6, 'https://github.com/seal-demo/delta-green-2024', false, null, 94.0, 'Giải pháp năng lượng thông minh tiêu biểu', $$[]$$, true),
    (302, now(), now(), 1, 6, 'https://github.com/seal-demo/alpha-green-2024', false, null, 88.5, 'Ứng dụng thực tế cao', $$[]$$, true);

-- SCORES (Giám khảo đã chấm xong 100%)
INSERT INTO scores (
    id, created_at, updated_at, submission_id, judge_id, score_value,
    criteria_scores_json, comment
) VALUES
    (1011, now(), now(), 101, 3, 95.5, $$[]$$, 'Giải pháp AI vượt trội, ứng dụng cao'),
    (1012, now(), now(), 101, 4, 95.5, $$[]$$, 'Chấm điểm cao cho mô hình AI'),
    (1021, now(), now(), 102, 3, 89.0, $$[]$$, 'Prototype hoàn thiện, giao diện mượt'),
    (1022, now(), now(), 102, 4, 89.0, $$[]$$, 'Thiết kế hệ thống bài bản'),
    (1031, now(), now(), 103, 3, 83.5, $$[]$$, 'Ý tưởng sáng tạo, pitching ấn tượng'),
    (1032, now(), now(), 103, 4, 83.5, $$[]$$, 'Trình bày rõ ràng'),

    (2011, now(), now(), 201, 3, 97.0, $$[]$$, 'Mô hình AI xuất sắc nhất mùa giải 2025'),
    (2021, now(), now(), 202, 3, 91.5, $$[]$$, 'Ý tưởng thực tiễn cao'),
    (2031, now(), now(), 203, 3, 86.0, $$[]$$, 'Tiềm năng phát triển lớn'),

    (3011, now(), now(), 301, 3, 94.0, $$[]$$, 'Giải pháp năng lượng thông minh tiêu biểu'),
    (3021, now(), now(), 302, 3, 88.5, $$[]$$, 'Ứng dụng thực tế cao');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT max(id) FROM events));
SELECT setval(pg_get_serial_sequence('tracks', 'id'), (SELECT max(id) FROM tracks));
SELECT setval(pg_get_serial_sequence('rounds', 'id'), (SELECT max(id) FROM rounds));
SELECT setval(pg_get_serial_sequence('track_round_matrix', 'id'), (SELECT max(id) FROM track_round_matrix));
SELECT setval(pg_get_serial_sequence('teams', 'id'), (SELECT max(id) FROM teams));
SELECT setval(pg_get_serial_sequence('team_members', 'id'), (SELECT max(id) FROM team_members));
SELECT setval(pg_get_serial_sequence('submissions', 'id'), (SELECT max(id) FROM submissions));
SELECT setval(pg_get_serial_sequence('scores', 'id'), (SELECT max(id) FROM scores));
SELECT setval(pg_get_serial_sequence('prizes', 'id'), (SELECT max(id) FROM prizes));

COMMIT;
