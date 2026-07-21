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
    (3, now(), now(), 'Staff Minh Tran', 'judge1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'SEAL Partner', null, null, null, 'STAFF', 'APPROVED'),
    (4, now(), now(), 'Staff Hanh Pham', 'judge2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'Tech Mentor Network', null, null, null, 'STAFF', 'APPROVED'),
    (5, now(), now(), 'Staff Bao Vo', 'mentor1@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'Software Guild', null, null, null, 'STAFF', 'APPROVED'),
    (6, now(), now(), 'Staff Nhi Le', 'mentor2@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', null, false, 'AI Lab', null, null, null, 'STAFF', 'APPROVED'),
    (7, now(), now(), 'Leader An Nguyen', 'leader.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170001', true, 'Dai hoc FPT', null, 'https://example.com/cards/se170001.png', null, 'LEADER', 'APPROVED'),
    (8, now(), now(), 'Member Binh Tran', 'member.alpha@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170002', true, 'Dai hoc FPT', null, 'https://example.com/cards/se170002.png', null, 'MEMBER', 'APPROVED'),
    (9, now(), now(), 'Leader Chi Pham', 'leader.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170003', true, 'Dai hoc FPT', null, 'https://example.com/cards/ai170003.png', null, 'LEADER', 'APPROVED'),
    (10, now(), now(), 'Member Duy Le', 'member.beta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170004', true, 'Dai hoc FPT', null, 'https://example.com/cards/ai170004.png', null, 'MEMBER', 'APPROVED'),
    (11, now(), now(), 'Pending Student', 'pending@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170099', true, 'Dai hoc FPT', null, 'https://example.com/cards/se170099.png', null, 'MEMBER', 'PENDING');

INSERT INTO events (
    id, created_at, updated_at, name, season, year, reg_start_date, reg_end_date,
    event_start_date, event_end_date, default_submission_deadline, round_count,
    structure_initialized, submission_form_schema, competition_rules,
    rule_document_url, is_active
) VALUES (
    1, now(), now(), 'SEAL Hackathon Spring 2026', 'SPRING', 2026,
    '2026-07-01 08:00:00', '2026-07-20 23:59:00',
    '2026-07-22 08:00:00', '2026-07-30 18:00:00',
    '2026-07-28 23:59:00', 2, true,
    $$[
      {"id":"projectName","label":"Ten du an","type":"text","required":true},
      {"id":"problemStatement","label":"Van de can giai quyet","type":"textarea","required":true},
      {"id":"repoUrl","label":"Link source code","type":"url","required":true},
      {"id":"demoUrl","label":"Link demo","type":"url","required":false},
      {"id":"pitchDeck","label":"Pitch deck","type":"url","required":true}
    ]$$,
    $$1. Moi doi co 2-5 thanh vien.
2. San pham phai duoc phat trien trong khuon kho su kien.
3. Bai nop tre deadline se bi danh dau va can coordinator xem xet.
4. Judge cham diem theo rubric cong khai cua tung round.
5. Moi lan sua diem phai co ly do de luu audit log.$$,
    'https://example.com/seal/rules-spring-2026.pdf',
    true
),
(
    2, now(), now(), 'SEAL Hackathon Fall 2025', 'FALL', 2025,
    '2025-09-01 08:00:00', '2025-09-15 23:59:00',
    '2025-09-20 08:00:00', '2025-09-28 18:00:00',
    '2025-09-26 23:59:00', 2, true,
    $$[
      {"id":"projectName","label":"Ten du an","type":"text","required":true},
      {"id":"repoUrl","label":"Link source code","type":"url","required":true},
      {"id":"demoUrl","label":"Link demo","type":"url","required":true}
    ]$$,
    $$1. Moi doi co 2-5 thanh vien.
2. San pham can co demo hoat dong.
3. Ket qua duoc cong bo sau vong final pitch.$$,
    'https://example.com/seal/rules-fall-2025.pdf',
    false
),
(
    3, now(), now(), 'SEAL Hackathon Spring 2025', 'SPRING', 2025,
    '2025-03-01 08:00:00', '2025-03-12 23:59:00',
    '2025-03-18 08:00:00', '2025-03-25 18:00:00',
    '2025-03-23 23:59:00', 2, true,
    $$[
      {"id":"projectName","label":"Ten du an","type":"text","required":true},
      {"id":"pitchDeck","label":"Pitch deck","type":"url","required":true}
    ]$$,
    $$1. Giai phap can bam sat chu de hoc duong so.
2. Diem final gom san pham, tac dong va kha nang trinh bay.$$,
    'https://example.com/seal/rules-spring-2025.pdf',
    false
);

INSERT INTO tracks (id, created_at, updated_at, name, description, event_id, max_teams) VALUES
    (1, now(), now(), 'Software Engineering', 'Web, mobile, backend, platform and productivity tools', 1, NULL),
    (2, now(), now(), 'AI Application', 'AI-powered products, data apps and automation workflows', 1, NULL);

INSERT INTO rounds (id, created_at, updated_at, name, order_index, event_id) VALUES
    (1, now(), now(), 'Round 1 - Prototype', 1, 1),
    (2, now(), now(), 'Round 2 - Final Pitch', 2, 1);

INSERT INTO track_round_matrix (
    id, created_at, updated_at, track_id, round_id, guideline_url,
    submission_deadline, scoring_criteria_json
) VALUES
    (1, now(), now(), 1, 1, 'https://example.com/guidelines/se-r1.pdf', '2026-07-24 23:59:00',
    $$[
      {"id":"problem_fit","label":"Problem fit","description":"Muc do hieu bai toan va dung nhu cau nguoi dung","maxScore":100,"weight":25},
      {"id":"technical","label":"Ky thuat","description":"Kien truc, code quality, kha nang van hanh","maxScore":100,"weight":35},
      {"id":"prototype","label":"Prototype","description":"Do hoan thien cua ban demo","maxScore":100,"weight":25},
      {"id":"presentation","label":"Presentation","description":"Trinh bay ro rang va tra loi cau hoi","maxScore":100,"weight":15}
    ]$$),
    (2, now(), now(), 1, 2, 'https://example.com/guidelines/se-r2.pdf', '2026-07-28 23:59:00',
    $$[
      {"id":"presentation","label":"Presentation","description":"Storytelling, slide, Q&A","maxScore":100,"weight":30},
      {"id":"innovation","label":"Tinh sang tao","description":"Cach tiep can moi va khac biet","maxScore":100,"weight":20},
      {"id":"technical","label":"Ky thuat","description":"Tinh on dinh, bao mat, kha nang mo rong","maxScore":100,"weight":30},
      {"id":"impact","label":"Tinh ung dung","description":"Gia tri thuc te va kha nang trien khai","maxScore":100,"weight":20}
    ]$$),
    (3, now(), now(), 2, 1, 'https://example.com/guidelines/ai-r1.pdf', '2026-07-24 23:59:00',
    $$[
      {"id":"data","label":"Du lieu","description":"Chat luong du lieu va cach xu ly","maxScore":100,"weight":25},
      {"id":"model","label":"Model","description":"Lua chon mo hinh, prompt, evaluation","maxScore":100,"weight":35},
      {"id":"product","label":"San pham","description":"Trai nghiem nguoi dung va tinh hoan thien","maxScore":100,"weight":25},
      {"id":"presentation","label":"Presentation","description":"Trinh bay va Q&A","maxScore":100,"weight":15}
    ]$$),
    (4, now(), now(), 2, 2, 'https://example.com/guidelines/ai-r2.pdf', '2026-07-28 23:59:00',
    $$[
      {"id":"presentation","label":"Presentation","description":"Storytelling, demo flow, Q&A","maxScore":100,"weight":25},
      {"id":"innovation","label":"Tinh sang tao","description":"Muc do moi cua ung dung AI","maxScore":100,"weight":25},
      {"id":"model_quality","label":"Chat luong AI","description":"Do chinh xac, an toan, kha nang giai thich","maxScore":100,"weight":30},
      {"id":"business","label":"Tinh ung dung","description":"Gia tri, thi truong, kha nang mo rong","maxScore":100,"weight":20}
    ]$$);

INSERT INTO matrix_mentors (matrix_id, mentor_id) VALUES
    (1, 5), (2, 5), (3, 6), (4, 6);

INSERT INTO matrix_judges (matrix_id, judge_id) VALUES
    (1, 3), (1, 4), (2, 3), (2, 4), (3, 3), (3, 4), (4, 3), (4, 4);

INSERT INTO teams (
    id, created_at, updated_at, name, description, type, join_password,
    track_id, event_id
) VALUES
    (1, now(), now(), 'Alpha Builders', 'Builds a collaborative project planning tool for student teams.', 'PUBLIC', null, 1, 1),
    (2, now(), now(), 'Beta Vision', 'Creates an AI assistant for summarizing lectures and extracting action items.', 'PRIVATE', 'beta2026', 2, 1);

INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role) VALUES
    (1, now(), now(), 1, 7, 'LEADER'),
    (2, now(), now(), 1, 8, 'MEMBER'),
    (3, now(), now(), 2, 9, 'LEADER'),
    (4, now(), now(), 2, 10, 'MEMBER');

INSERT INTO submissions (
    id, created_at, updated_at, team_id, matrix_id, file_url, is_flagged,
    flag_reason, score, feedback, criteria_scores_json, is_graded
) VALUES
    (1, now(), now(), 1, 1, 'https://github.com/seal-demo/alpha-builders/releases/tag/r1', false, null, 84.5,
     'Prototype tot, can lam ro hon phan scale va monitoring.',
     $$[
       {"id":"problem_fit","label":"Problem fit","maxScore":100,"weight":25,"score":86,"note":"Dung pain point cua doi thi."},
       {"id":"technical","label":"Ky thuat","maxScore":100,"weight":35,"score":82,"note":"Kien truc on, can test them."},
       {"id":"prototype","label":"Prototype","maxScore":100,"weight":25,"score":88,"note":"Demo chay tot."},
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":15,"score":82,"note":"Trinh bay gon."}
     ]$$, true),
    (2, now(), now(), 2, 3, 'https://github.com/seal-demo/beta-vision/releases/tag/r1', false, null, null,
     null, null, false);

INSERT INTO scores (
    id, created_at, updated_at, submission_id, judge_id, score_value,
    criteria_scores_json, comment
) VALUES
    (1, now(), now(), 1, 3, 84.5,
     $$[
       {"id":"problem_fit","label":"Problem fit","maxScore":100,"weight":25,"score":86,"note":"Dung pain point cua doi thi."},
       {"id":"technical","label":"Ky thuat","maxScore":100,"weight":35,"score":82,"note":"Kien truc on, can test them."},
       {"id":"prototype","label":"Prototype","maxScore":100,"weight":25,"score":88,"note":"Demo chay tot."},
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":15,"score":82,"note":"Trinh bay gon."}
     ]$$,
     'Prototype tot, can lam ro hon phan scale va monitoring.');

INSERT INTO prizes (id, created_at, updated_at, name, description, event_id, team_id) VALUES
    (1, now(), now(), 'First Prize', '10,000,000 VND va goi mentoring 3 thang', 1, 2),
    (2, now(), now(), 'Innovation Prize', 'Danh cho y tuong sang tao nhat', 1, 1),
    (3, now(), now(), 'Best Presentation', 'Danh cho phan pitch thuyet phuc nhat', 1, 1),
    (4, now(), now(), 'Second Prize', 'Giai nhi SEAL Fall 2025 voi san pham lap lich hoc nhom thong minh', 2, 1),
    (5, now(), now(), 'Community Impact Award', 'Du an duoc danh gia cao ve tac dong voi sinh vien nam nhat', 3, 1),
    (6, now(), now(), 'AI Excellence Award', 'Giai ung dung AI co chat luong model va evaluation tot nhat', 2, 2),
    (7, now(), now(), 'Best Product Demo', 'Demo san pham on dinh, flow ro va co kha nang trien khai', 3, 2);

INSERT INTO notifications (
    id, created_at, updated_at, title, body, target_role, recipient_id, sender_id
) VALUES
    (1, now(), now(), 'Mo cong dang ky SEAL Spring 2026', 'Coordinator da mo cong dang ky cho SEAL Hackathon Spring 2026.', 'MEMBER', null, 2),
    (2, now(), now(), 'Can cham bai Round 1', 'Staff duoc phan cong Judge vui long cham cac bai nop Round 1 truoc deadline.', 'STAFF', null, 2);

INSERT INTO chat_messages (id, created_at, updated_at, team_id, sender_id, content) VALUES
    (1, now(), now(), 1, 5, 'Team Alpha, hay them risk log va deployment note vao submission.'),
    (2, now(), now(), 1, 7, 'Da ro mentor, team se cap nhat trong ban nop tiep theo.'),
    (3, now(), now(), 2, 6, 'Beta Vision nen them metric danh gia chat luong summary.');

-- Extra demo data for full role testing.
INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
) VALUES
    (12, now(), now(), 'Leader Khoa Phan', 'leader.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170005', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Khoa', null, 'LEADER', 'APPROVED'),
    (13, now(), now(), 'Member Mai Ho', 'member.gamma@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170006', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Mai', null, 'MEMBER', 'APPROVED'),
    (14, now(), now(), 'Leader Nam Do', 'leader.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170007', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Nam', null, 'LEADER', 'APPROVED'),
    (15, now(), now(), 'Member Oanh Bui', 'member.delta@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'AI170008', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Oanh', null, 'MEMBER', 'APPROVED'),
    (16, now(), now(), 'Student Join Request', 'join.request@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170088', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+Join', null, 'MEMBER', 'APPROVED'),
    (17, now(), now(), 'Pending Upload Student', 'pending.upload@seal.dev', '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa', 'SE170100', true, 'Dai hoc FPT', null, 'https://placehold.co/900x560/fef3c7/92400e?text=Pending+Student+Card', null, 'MEMBER', 'PENDING');

INSERT INTO teams (
    id, created_at, updated_at, name, description, type, join_password,
    track_id, event_id
) VALUES
    (3, now(), now(), 'Gamma Flow', 'Designs an internal event operations dashboard with checklist automation.', 'PUBLIC', null, 1, 1),
    (4, now(), now(), 'Delta Mind', 'Builds an AI mentor assistant that reviews pitch decks and suggests improvements.', 'PRIVATE', 'delta2026', 2, 1);

INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role) VALUES
    (5, now(), now(), 3, 12, 'LEADER'),
    (6, now(), now(), 3, 13, 'MEMBER'),
    (7, now(), now(), 4, 14, 'LEADER'),
    (8, now(), now(), 4, 15, 'MEMBER');

INSERT INTO team_join_requests (id, created_at, updated_at, team_id, user_id, status) VALUES
    (1, now(), now(), 1, 16, 'PENDING');

INSERT INTO submissions (
    id, created_at, updated_at, team_id, matrix_id, file_url, is_flagged,
    flag_reason, score, feedback, criteria_scores_json, is_graded
) VALUES
    (3, now(), now(), 1, 2, 'https://github.com/seal-demo/alpha-builders/releases/tag/final', false, null, null, null, null, false),
    (4, now(), now(), 3, 1, 'https://github.com/seal-demo/gamma-flow/releases/tag/r1', false, null, 78.2,
     'Dashboard co workflow ro, can giam bot complexity cua permission model.',
     $$[
       {"id":"problem_fit","label":"Problem fit","maxScore":100,"weight":25,"score":80,"note":"Bai toan van hanh ro."},
       {"id":"technical","label":"Ky thuat","maxScore":100,"weight":35,"score":76,"note":"Can cai thien phan audit."},
       {"id":"prototype","label":"Prototype","maxScore":100,"weight":25,"score":82,"note":"Demo day du flow chinh."},
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":15,"score":74,"note":"Can trinh bay ngan hon."}
     ]$$, true),
    (5, now(), now(), 4, 3, 'https://github.com/seal-demo/delta-mind/releases/tag/r1', false, null, null, null, null, false),
    (6, now(), now(), 2, 4, 'https://github.com/seal-demo/beta-vision/releases/tag/final', false, null, 91.0,
     'Ung dung AI ro gia tri, demo tot va co metric danh gia kha thuyet phuc.',
     $$[
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":25,"score":92,"note":"Pitch mach lac."},
       {"id":"innovation","label":"Tinh sang tao","maxScore":100,"weight":25,"score":90,"note":"Y tuong AI co diem khac biet."},
       {"id":"model_quality","label":"Chat luong AI","maxScore":100,"weight":30,"score":91,"note":"Co evaluation ro."},
       {"id":"business","label":"Tinh ung dung","maxScore":100,"weight":20,"score":91,"note":"Co kha nang trien khai."}
     ]$$, true);

INSERT INTO scores (
    id, created_at, updated_at, submission_id, judge_id, score_value,
    criteria_scores_json, comment
) VALUES
    (2, now(), now(), 4, 4, 78.2,
     $$[
       {"id":"problem_fit","label":"Problem fit","maxScore":100,"weight":25,"score":80,"note":"Bai toan van hanh ro."},
       {"id":"technical","label":"Ky thuat","maxScore":100,"weight":35,"score":76,"note":"Can cai thien phan audit."},
       {"id":"prototype","label":"Prototype","maxScore":100,"weight":25,"score":82,"note":"Demo day du flow chinh."},
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":15,"score":74,"note":"Can trinh bay ngan hon."}
     ]$$,
     'Dashboard co workflow ro, can giam bot complexity cua permission model.'),
    (3, now(), now(), 6, 3, 91.0,
     $$[
       {"id":"presentation","label":"Presentation","maxScore":100,"weight":25,"score":92,"note":"Pitch mach lac."},
       {"id":"innovation","label":"Tinh sang tao","maxScore":100,"weight":25,"score":90,"note":"Y tuong AI co diem khac biet."},
       {"id":"model_quality","label":"Chat luong AI","maxScore":100,"weight":30,"score":91,"note":"Co evaluation ro."},
       {"id":"business","label":"Tinh ung dung","maxScore":100,"weight":20,"score":91,"note":"Co kha nang trien khai."}
     ]$$,
     'Ung dung AI ro gia tri, demo tot va co metric danh gia kha thuyet phuc.');

INSERT INTO prizes (id, created_at, updated_at, name, description, event_id, team_id) VALUES
    (8, now(), now(), 'Rising Team Award', 'Doi moi co workflow van hanh tot va kha nang cai tien nhanh qua tung vong', 1, 3),
    (9, now(), now(), 'Operational Excellence', 'Du an dashboard noi bo co thiet ke checklist va audit flow ro rang', 2, 3),
    (10, now(), now(), 'Third Prize', 'Giai ba cho giai phap AI mentor ho tro review pitch deck', 1, 4),
    (11, now(), now(), 'Best AI Prototype', 'Prototype AI co trai nghiem san pham tot va output de giai thich', 3, 4);

INSERT INTO chat_messages (id, created_at, updated_at, team_id, sender_id, content) VALUES
    (4, now(), now(), 3, 5, 'Gamma Flow, mentor da xem prototype. Hay them status filter cho submission monitor.'),
    (5, now(), now(), 3, 12, 'Team se bo sung filter va cap nhat demo trong ban nop cuoi.'),
    (6, now(), now(), 4, 6, 'Delta Mind nen them phan so sanh output giua cac prompt strategy.'),
    (7, now(), now(), 4, 14, 'Da ro mentor, team se them evaluation table vao pitch deck.'),
    (8, now(), now(), 2, 6, 'Beta Vision dang co loi upload deck? Neu can mentor se review link demo.'),
    (9, now(), now(), 2, 9, 'Link demo da cap nhat, nho mentor xem lai giup team.');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT max(id) FROM events));
SELECT setval(pg_get_serial_sequence('tracks', 'id'), (SELECT max(id) FROM tracks));
SELECT setval(pg_get_serial_sequence('rounds', 'id'), (SELECT max(id) FROM rounds));
SELECT setval(pg_get_serial_sequence('track_round_matrix', 'id'), (SELECT max(id) FROM track_round_matrix));
SELECT setval(pg_get_serial_sequence('teams', 'id'), (SELECT max(id) FROM teams));
SELECT setval(pg_get_serial_sequence('team_members', 'id'), (SELECT max(id) FROM team_members));
SELECT setval(pg_get_serial_sequence('team_join_requests', 'id'), (SELECT max(id) FROM team_join_requests));
SELECT setval(pg_get_serial_sequence('submissions', 'id'), (SELECT max(id) FROM submissions));
SELECT setval(pg_get_serial_sequence('scores', 'id'), (SELECT max(id) FROM scores));
SELECT setval(pg_get_serial_sequence('prizes', 'id'), (SELECT max(id) FROM prizes));
SELECT setval(pg_get_serial_sequence('notifications', 'id'), (SELECT max(id) FROM notifications));
SELECT setval(pg_get_serial_sequence('chat_messages', 'id'), (SELECT max(id) FROM chat_messages));

-- Account role is flexible; team position remains in team_members.role.
UPDATE users SET role = 'USER' WHERE role IN ('MEMBER', 'LEADER');
UPDATE notifications SET target_role = 'USER' WHERE target_role IN ('MEMBER', 'LEADER');

COMMIT;
