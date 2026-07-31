-- SEAL Hackathon Flow Seeding Script (15 Users, 5 Teams, 5 Active Events)
-- Password for all accounts: 123456

BEGIN;

TRUNCATE TABLE
    audit_logs,
    chat_messages,
    notification_reads,
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

-- ============================================================================
-- 1. USERS (15 Users Total)
-- ============================================================================
INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
) VALUES
-- Admin & Coordinator (2)
(1, now(), now(), 'System Admin', 'admin@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'SEAL System', null, null, null, 'ADMIN', 'APPROVED'),
(2, now(), now(), 'Coordinator Hoang Nam', 'coordinator@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'FPT University', null, null, null, 'COORDINATOR', 'APPROVED'),

-- Judges (2 Staff)
(3, now(), now(), 'Judge Nguyen Van A', 'judge1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'VNG Corp', null, null, null, 'STAFF', 'APPROVED'),
(4, now(), now(), 'Judge Le Thi B', 'judge2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'FPT Software', null, null, null, 'STAFF', 'APPROVED'),

-- Mentors (2 Staff)
(5, now(), now(), 'Mentor Tran Bao', 'mentor1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'AI Academy', null, null, null, 'STAFF', 'APPROVED'),
(6, now(), now(), 'Mentor Pham Minh', 'mentor2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'TechHub FPT', null, null, null, 'STAFF', 'APPROVED'),

-- Students / Participants (7 Approved Students)
(7, now(), now(), 'Nguyen Duc An (Leader Team Alpha)', 'student1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170010', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+An', null, 'USER', 'APPROVED'),
(8, now(), now(), 'Tran Minh Binh (Member Team Alpha)', 'student2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170011', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Binh', null, 'USER', 'APPROVED'),
(9, now(), now(), 'Le Quang Chi (Leader Team Beta)', 'student3@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170012', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Chi', null, 'USER', 'APPROVED'),
(10, now(), now(), 'Pham Hoang Duy (Member Team Beta)', 'student4@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170013', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Duy', null, 'USER', 'APPROVED'),
(11, now(), now(), 'Hoang Duc Em (Leader Team Gamma)', 'student5@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170014', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Em', null, 'USER', 'APPROVED'),
(12, now(), now(), 'Vu Hong Giang (Leader Team Delta)', 'student6@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170015', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Giang', null, 'USER', 'APPROVED'),
(13, now(), now(), 'Doan Van Hai (Leader Team Epsilon)', 'student7@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170016', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Hai', null, 'USER', 'APPROVED'),

-- Special Test Accounts (2: 1 Pending, 1 Banned)
(14, now(), now(), 'Phan Van Phat (Pending Approval)', 'pending@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170999', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Card+Pending', null, 'USER', 'PENDING'),
(15, now(), now(), 'Bui Van Vi Pham (Banned Account)', 'banned@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170888', true, 'Dai hoc FPT', null, null, 'Vi phạm quy chế thi', 'USER', 'BANNED');

-- ============================================================================
-- 2. 5 ACTIVE EVENTS (Sự kiện đang diễn ra)
-- ============================================================================
INSERT INTO events (id, created_at, updated_at, name, description, year, is_active, results_published) VALUES
(1, now(), now(), 'SEAL AI Hackathon 2026', 'Cuộc thi ứng dụng Trí tuệ Nhân tạo đột phá cho sinh viên', 2026, true, false),
(2, now(), now(), 'Web3 & Blockchain Challenge 2026', 'Thử thách giải pháp Chuỗi khối và Tài chính phi tập trung', 2026, true, false),
(3, now(), now(), 'GreenTech Innovation 2026', 'Sáng kiến Công nghệ Xanh và Phát triển bền vững', 2026, true, false),
(4, now(), now(), 'Mobile App Championship 2026', 'Giải đấu Phát triển Ứng dụng Di động Đa nền tảng', 2026, true, false),
(5, now(), now(), 'Cybersecurity Cup 2026', 'Đấu trường An ninh mạng và Bảo mật thông tin', 2026, true, false);

-- ============================================================================
-- 3. TRACKS & ROUNDS
-- ============================================================================
INSERT INTO tracks (id, created_at, updated_at, name, description, event_id) VALUES
(1, now(), now(), 'AI Applications Track', 'Chuyên đề Ứng dụng AI & Generative Model', 1),
(2, now(), now(), 'Smart Contract Track', 'Chuyên đề Lập trình Hợp đồng thông minh', 2),
(3, now(), now(), 'IoT & Sensor Track', 'Chuyên đề Thiết bị Thông minh & Cảm biến', 3),
(4, now(), now(), 'Flutter/React Native Track', 'Chuyên đề Đa nền tảng Di động', 4),
(5, now(), now(), 'Web Security Track', 'Chuyên đề Khảo sát & An toàn Web', 5);

INSERT INTO rounds (id, created_at, updated_at, name, order_index, event_id) VALUES
(1, now(), now(), 'Vòng Sơ Loại (Preliminary)', 1, 1),
(2, now(), now(), 'Vòng Chung Kết (Finals)', 2, 2),
(3, now(), now(), 'Vòng Yêu Cầu Đề Án', 1, 3),
(4, now(), now(), 'Vòng Ý Tưởng', 1, 4),
(5, now(), now(), 'Vòng CTF Qualification', 1, 5);

-- Track Round Matrix (Mapping Matrix)
INSERT INTO track_round_matrix (id, created_at, updated_at, track_id, round_id) VALUES
(1, now(), now(), 1, 1),
(2, now(), now(), 2, 2),
(3, now(), now(), 3, 3),
(4, now(), now(), 4, 4),
(5, now(), now(), 5, 5);

-- Assign Judges & Mentors to Matrix
INSERT INTO matrix_judges (matrix_id, judge_id) VALUES
(1, 3), (1, 4), -- Matrix 1 (AI Track - Vòng 1): Judge A (3), Judge B (4)
(2, 3), (2, 4); -- Matrix 2 (Blockchain Track - Vòng Chung Kết): Judge A (3), Judge B (4)

INSERT INTO matrix_mentors (matrix_id, mentor_id) VALUES
(1, 5), -- Matrix 1: Mentor Tran Bao (5)
(2, 6); -- Matrix 2: Mentor Pham Minh (6)

-- ============================================================================
-- 4. 5 TEAMS & MEMBERS (5 Đội thi)
-- ============================================================================
INSERT INTO teams (id, created_at, updated_at, name, description, type, join_password, track_id, event_id) VALUES
(1, now(), now(), 'Alpha Tech', 'Đội làm dự án AI Chatbot phân tích y tế', 'PUBLIC', '123456', 1, 1),
(2, now(), now(), 'Beta AI', 'Đội làm ứng dụng AI Nhận diện khuôn mặt điểm danh', 'PUBLIC', '123456', 1, 1),
(3, now(), now(), 'Gamma Blockchain', 'Hệ thống Ví điện tử phi tập trung Web3', 'PUBLIC', '123456', 2, 2),
(4, now(), now(), 'Delta Green', 'Giải pháp Giám sát Năng lượng xanh qua IoT', 'PUBLIC', '123456', 3, 3),
(5, now(), now(), 'Epsilon Cyber', 'Đội thi thử nghiệm An ninh mạng', 'PUBLIC', '123456', 5, 5);

INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role) VALUES
-- Team 1
(1, now(), now(), 1, 7, 'LEADER'), -- Leader An
(2, now(), now(), 1, 8, 'MEMBER'), -- Member Binh
-- Team 2
(3, now(), now(), 2, 9, 'LEADER'), -- Leader Chi
(4, now(), now(), 2, 10, 'MEMBER'), -- Member Duy
-- Team 3, 4, 5
(5, now(), now(), 3, 11, 'LEADER'),
(6, now(), now(), 4, 12, 'LEADER'),
(7, now(), now(), 5, 13, 'LEADER');

-- Join Requests (Yêu cầu xin vào đội 4 để Leader test bấm Approve / Reject)
INSERT INTO team_join_requests (id, created_at, updated_at, team_id, user_id, status, type) VALUES
(1, now(), now(), 4, 8, 'PENDING', 'APPLY'),
(2, now(), now(), 4, 10, 'PENDING', 'APPLY');

-- ============================================================================
-- 5. SUBMISSIONS (Bài nộp sẵn sàng cho Judge chấm điểm)
-- ============================================================================
INSERT INTO submissions (id, created_at, updated_at, team_id, matrix_id, file_url, is_flagged, is_graded) VALUES
-- Team 1 (Alpha Tech) - CHƯA CHẤM ĐIỂM (Để Judge vào chấm thử ngay!)
(1, now(), now(), 1, 1, 'https://github.com/alpha-tech/seal-ai-medical-chatbot', false, false),

-- Team 2 (Beta AI) - ĐÃ ĐƯỢC CHẤM 1 LẦN (Để test xem điểm đã chấm)
(2, now(), now(), 2, 1, 'https://github.com/beta-ai/face-attendance-system', false, true),

-- Team 3 (Gamma Blockchain) - CHƯA CHẤM ĐIỂM (Để Judge vào chấm thử ngay!)
(3, now(), now(), 3, 2, 'https://github.com/gamma-web3/decentralized-wallet-v1', false, false);

-- ============================================================================
-- 6. SCORES (Điểm đã chấm cho Team 2)
-- ============================================================================
INSERT INTO scores (id, created_at, updated_at, submission_id, judge_id, score_value, criteria_scores_json, comment) VALUES
(1, now(), now(), 2, 3, 88.5, '[{"name":"Innovation","score":90,"weight":0.4},{"name":"Technical","score":87,"weight":0.6}]', 'Sản phẩm hoàn thiện tốt, giao diện trực quan');

-- ============================================================================
-- 7. NOTIFICATIONS & CHAT MESSAGES
-- ============================================================================
INSERT INTO notifications (id, created_at, updated_at, title, body, sender_id) VALUES
(1, now(), now(), 'Chào mừng đến với SEAL Hackathon 2026', 'Vòng Sơ Loại đã chính thức mở nhận bài nộp!', 2),
(2, now(), now(), 'Nhắc nhở nộp bài Vòng 1', 'Các đội thuộc AI Applications Track vui lòng nộp bài đúng hạn.', 2);

INSERT INTO chat_messages (id, created_at, updated_at, sender_id, team_id, content) VALUES
(1, now() - interval '3 hours', now(), 7, 1, 'Chào cả đội, mình vừa đẩy code ban đầu lên GitHub nhé!'),
(2, now() - interval '2 hours', now(), 8, 1, 'Đã nhận link, mình sẽ test giao diện!');

-- Update sequence values
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('events_id_seq', (SELECT MAX(id) FROM events));
SELECT setval('tracks_id_seq', (SELECT MAX(id) FROM tracks));
SELECT setval('rounds_id_seq', (SELECT MAX(id) FROM rounds));
SELECT setval('track_round_matrix_id_seq', (SELECT MAX(id) FROM track_round_matrix));
SELECT setval('teams_id_seq', (SELECT MAX(id) FROM teams));
SELECT setval('team_members_id_seq', (SELECT MAX(id) FROM team_members));
SELECT setval('team_join_requests_id_seq', (SELECT MAX(id) FROM team_join_requests));
SELECT setval('submissions_id_seq', (SELECT MAX(id) FROM submissions));
SELECT setval('scores_id_seq', (SELECT MAX(id) FROM scores));
SELECT setval('notifications_id_seq', (SELECT MAX(id) FROM notifications));
SELECT setval('chat_messages_id_seq', (SELECT MAX(id) FROM chat_messages));

COMMIT;
