-- SEAL Hackathon comprehensive development seed
-- WARNING: This script removes ALL existing application data.
-- Target: PostgreSQL database seal_hackathon
-- Login password for every seeded account: 1
--
-- Accounts:
--   admin@seal.dev
--   coordinator1@seal.dev .. coordinator2@seal.dev
--   staff1@seal.dev       .. staff10@seal.dev
--   user1@seal.dev        .. user20@seal.dev
--
-- Run from shm-backend:
--   $env:PGPASSWORD='12345'
--   & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' `
--     -h localhost -U postgres -d seal_hackathon -v ON_ERROR_STOP=1 -f .\dev-seed.sql

\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';

BEGIN;

TRUNCATE TABLE
    notification_reads,
    audit_logs,
    chat_messages,
    notifications,
    scores,
    submissions,
    prizes,
    matrix_judges,
    matrix_mentors,
    track_mentors,
    track_round_matrix,
    team_members,
    team_join_requests,
    teams,
    rounds,
    tracks,
    rule_templates,
    system_activities,
    system_settings,
    users,
    events
RESTART IDENTITY CASCADE;

-- BCrypt hash for raw password "1".
-- All accounts deliberately share this password for local development only.
\set password_hash '$2a$10$f8Rlcv8vfWjbVTtTA3KQUOxQigCRXGWu9sQyKZ1ppG7ZTikyOsInm'

-- ---------------------------------------------------------------------------
-- 1. USERS: 1 admin, 2 coordinators, 10 staff, 20 regular users
-- ---------------------------------------------------------------------------

INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
) VALUES (
    1, now(), now(), 'SEAL Administrator', 'admin@seal.dev', :'password_hash',
    NULL, false, 'SEAL Organization', NULL, NULL, NULL, 'ADMIN', 'APPROVED'
);

INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
)
SELECT
    1 + n,
    now(),
    now(),
    'Coordinator ' || n,
    'coordinator' || n || '@seal.dev',
    :'password_hash',
    NULL,
    false,
    'SEAL Operations',
    NULL,
    NULL,
    NULL,
    'COORDINATOR',
    'APPROVED'
FROM generate_series(1, 2) AS n;

INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
)
SELECT
    3 + n,
    now(),
    now(),
    'SEAL Staff ' || lpad(n::text, 2, '0'),
    'staff' || n || '@seal.dev',
    :'password_hash',
    NULL,
    false,
    CASE
        WHEN n <= 4 THEN 'FPT University'
        WHEN n <= 7 THEN 'SEAL Technology Partner'
        ELSE 'SEAL Mentor Network'
    END,
    NULL,
    NULL,
    NULL,
    'STAFF',
    'APPROVED'
FROM generate_series(1, 10) AS n;

INSERT INTO users (
    id, created_at, updated_at, full_name, email, password, student_id,
    is_fpt_student, university_name, avatar_url, student_card_url,
    rejection_reason, role, status
)
SELECT
    13 + n,
    now(),
    now(),
    'Student User ' || lpad(n::text, 2, '0'),
    'user' || n || '@seal.dev',
    :'password_hash',
    'SE26' || lpad(n::text, 4, '0'),
    true,
    'FPT University',
    NULL,
    'https://placehold.co/900x560/eaf3ff/0f63c9?text=Student+Card+' || n,
    CASE
        WHEN n = 19 THEN 'Student card information is unreadable.'
        WHEN n = 20 THEN 'Account banned for testing the access-control flow.'
        ELSE NULL
    END,
    'USER',
    CASE
        WHEN n <= 17 THEN 'APPROVED'
        WHEN n = 18 THEN 'PENDING'
        WHEN n = 19 THEN 'REJECTED'
        ELSE 'BANNED'
    END
FROM generate_series(1, 20) AS n;

-- ---------------------------------------------------------------------------
-- 2. EVENTS
--
-- Event 1: registration is open
-- Event 2: round 1 fully graded and ready to publish/advance
-- Event 3: round 1 published, round 2 is being graded
-- Event 4: final fully graded and ready to publish
-- Event 5: completed historical event with published leaderboard and prizes
-- Event 6: upcoming event, structure not initialized
-- ---------------------------------------------------------------------------

INSERT INTO events (
    id, created_at, updated_at, name, description, season, year,
    reg_start_date, reg_end_date, event_start_date, event_end_date,
    default_submission_deadline, round_count, structure_initialized,
    submission_form_schema, competition_rules, rule_document_url,
    is_active, results_published, ended_early
) VALUES
(
    1, now(), now(),
    'SEAL Summer Launch 2026',
    'Registration-open event used to test team creation, joining, mentor chat and registration limits.',
    'SUMMER', 2026,
    now() - interval '7 days', now() + interval '14 days',
    now() + interval '21 days', now() + interval '30 days',
    now() + interval '22 days', 3, false,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"problemStatement","label":"Problem statement","type":"textarea","required":true},
      {"id":"repoUrl","label":"Source repository","type":"url","required":true},
      {"id":"demoUrl","label":"Demo URL","type":"url","required":false},
      {"id":"pitchDeck","label":"Pitch deck","type":"url","required":true}
    ]$$,
    'Teams need 3-5 approved members. Only the team leader can submit. Staff assigned as mentors may support their own group.',
    'https://example.com/rules/summer-launch-2026.pdf',
    true, false, false
),
(
    2, now(), now(),
    'SEAL Build Sprint 2026',
    'Round 1 is completely graded. Sign in as a coordinator to test Publish and Advance Top N.',
    'SUMMER', 2026,
    now() - interval '20 days', now() - interval '12 days',
    now() - interval '2 days', now() + interval '10 days',
    now() - interval '2 hours', 3, true,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"repoUrl","label":"Repository URL","type":"url","required":true},
      {"id":"demoUrl","label":"Demo URL","type":"url","required":true}
    ]$$,
    'This event is seeded at the exact state before round 1 results are published.',
    'https://example.com/rules/build-sprint-2026.pdf',
    true, false, false
),
(
    3, now(), now(),
    'SEAL AI Challenge 2026',
    'Round 1 results are published. Round 2 contains one partially graded submission, one ungraded submission and a disqualified team.',
    'SUMMER', 2026,
    now() - interval '35 days', now() - interval '25 days',
    now() - interval '10 days', now() + interval '7 days',
    now() + interval '2 hours', 3, true,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"modelCard","label":"Model card","type":"url","required":true},
      {"id":"repoUrl","label":"Repository URL","type":"url","required":true}
    ]$$,
    'AI projects must disclose their datasets, evaluation metrics and safety limitations.',
    'https://example.com/rules/ai-challenge-2026.pdf',
    true, false, false
),
(
    4, now(), now(),
    'SEAL Grand Final 2026',
    'The final round is fully graded and waiting for the coordinator to publish final results.',
    'SUMMER', 2026,
    now() - interval '45 days', now() - interval '35 days',
    now() - interval '20 days', now() + interval '2 days',
    now() - interval '1 hour', 3, true,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"repoUrl","label":"Repository URL","type":"url","required":true},
      {"id":"pitchDeck","label":"Final pitch deck","type":"url","required":true}
    ]$$,
    'The final ranking is visible after the coordinator publishes the final round.',
    'https://example.com/rules/grand-final-2026.pdf',
    true, false, false
),
(
    5, now(), now(),
    'SEAL Spring Archive 2026',
    'Completed event used to test historical results, published leaderboard, prizes and locked scoring.',
    'SPRING', 2026,
    now() - interval '75 days', now() - interval '65 days',
    now() - interval '50 days', now() - interval '20 days',
    now() - interval '22 days', 3, true,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"repoUrl","label":"Repository URL","type":"url","required":true}
    ]$$,
    'Historical event. All scoring is locked because final results are published.',
    'https://example.com/rules/spring-archive-2026.pdf',
    false, true, false
),
(
    6, now(), now(),
    'SEAL Fall Innovation 2026',
    'Upcoming event used to test event editing before teams and round matrices exist.',
    'FALL', 2026,
    now() + interval '7 days', now() + interval '21 days',
    now() + interval '30 days', now() + interval '45 days',
    now() + interval '31 days', 3, false,
    $$[
      {"id":"projectName","label":"Project name","type":"text","required":true},
      {"id":"repoUrl","label":"Repository URL","type":"url","required":true}
    ]$$,
    'Upcoming innovation event. The coordinator may still edit groups and initialize its structure.',
    NULL,
    true, false, false
);

-- ---------------------------------------------------------------------------
-- 3. GROUPS/TRACKS AND MENTORS
-- ---------------------------------------------------------------------------

INSERT INTO tracks (id, created_at, updated_at, name, description, max_teams, event_id) VALUES
    (1, now(), now(), 'Group A - Product', 'Product and workflow solutions.', 10, 1),
    (2, now(), now(), 'Group B - AI', 'Applied AI and data products.', 10, 1),
    (3, now(), now(), 'Group C - Green Tech', 'Sustainability and climate technology.', 10, 1),

    (4, now(), now(), 'Group A - Web Platform', 'Web applications and developer platforms.', 10, 2),
    (5, now(), now(), 'Group B - Intelligent Apps', 'AI-enabled applications.', 10, 2),
    (6, now(), now(), 'Group C - Cloud Systems', 'Cloud and distributed systems.', 10, 2),

    (7, now(), now(), 'Group A - Generative AI', 'Language, multimodal and agentic systems.', 10, 3),
    (8, now(), now(), 'Group B - Responsible AI', 'Evaluation, safety and explainability.', 10, 3),

    (9, now(), now(), 'Group A - Digital Product', 'Customer-facing digital products.', 10, 4),
    (10, now(), now(), 'Group B - Enterprise Tech', 'Enterprise automation and infrastructure.', 10, 4),

    (11, now(), now(), 'Group A - Software', 'Historical software group.', 10, 5),
    (12, now(), now(), 'Group B - Data', 'Historical data group.', 10, 5),

    (13, now(), now(), 'Group A - Future Web', 'Future-facing web experiences.', 10, 6),
    (14, now(), now(), 'Group B - Robotics', 'Robotics and embedded systems.', 10, 6),
    (15, now(), now(), 'Group C - Cybersecurity', 'Security and privacy solutions.', 10, 6);

INSERT INTO track_mentors (track_id, mentor_id) VALUES
    (1, 4), (1, 5), (2, 6), (3, 7),
    (4, 4), (4, 5), (5, 6), (5, 7), (6, 8),
    (7, 5), (7, 6), (8, 7), (8, 8),
    (9, 4), (9, 6), (10, 5), (10, 7),
    (11, 4), (11, 5), (12, 6), (12, 7),
    (13, 8), (14, 9), (15, 10);

-- ---------------------------------------------------------------------------
-- 4. ROUNDS AND ROUND INSTANCES (track_round_matrix)
-- Events 2-5 each use grouped qualifiers plus one shared final.
-- ---------------------------------------------------------------------------

INSERT INTO rounds (id, created_at, updated_at, name, order_index, event_id) VALUES
    (1, now(), now(), 'Round 1 - Qualifier', 1, 2),
    (2, now(), now(), 'Round 2 - Semifinal', 2, 2),
    (3, now(), now(), 'Round 3 - Grand Final', 3, 2),

    (4, now(), now(), 'Round 1 - Qualifier', 1, 3),
    (5, now(), now(), 'Round 2 - Semifinal', 2, 3),
    (6, now(), now(), 'Round 3 - Grand Final', 3, 3),

    (7, now(), now(), 'Round 1 - Qualifier', 1, 4),
    (8, now(), now(), 'Round 2 - Semifinal', 2, 4),
    (9, now(), now(), 'Round 3 - Grand Final', 3, 4),

    (10, now(), now(), 'Round 1 - Qualifier', 1, 5),
    (11, now(), now(), 'Round 2 - Semifinal', 2, 5),
    (12, now(), now(), 'Round 3 - Grand Final', 3, 5);

-- Shared rubric used by the seeded matrices.
\set rubric_json '[{"id":"problem","label":"Problem and impact","description":"Problem clarity and real-world impact","maxScore":100,"weight":25},{"id":"innovation","label":"Innovation","description":"Originality and differentiation","maxScore":100,"weight":25},{"id":"technical","label":"Technical quality","description":"Architecture, implementation and reliability","maxScore":100,"weight":30},{"id":"presentation","label":"Presentation","description":"Demo, storytelling and Q&A","maxScore":100,"weight":20}]'

INSERT INTO track_round_matrix (
    id, created_at, updated_at, track_id, round_id, guideline_url,
    submission_start_date, submission_deadline, topn, duration_minutes,
    deadline_notified, is_published, grading_completion_notified,
    grading_duration_minutes, grading_deadline, grading_extension_notified,
    break_duration_minutes, break_end_time, scoring_criteria_json
) VALUES
    -- Event 2: round 1 closed and fully graded; later rounds not opened.
    (1, now(), now(), 4, 1, 'https://example.com/guides/build-r1-a.pdf',
        now() - interval '4 hours', now() - interval '2 hours', 1, 120,
        true, false, true, 240, now() + interval '2 hours', false, 10, NULL, :'rubric_json'),
    (2, now(), now(), 5, 1, 'https://example.com/guides/build-r1-b.pdf',
        now() - interval '4 hours', now() - interval '2 hours', 1, 120,
        true, false, true, 240, now() + interval '2 hours', false, 10, NULL, :'rubric_json'),
    (3, now(), now(), 6, 1, 'https://example.com/guides/build-r1-c.pdf',
        now() - interval '4 hours', now() - interval '2 hours', 1, 120,
        true, false, true, 240, now() + interval '2 hours', false, 10, NULL, :'rubric_json'),
    (4, now(), now(), 4, 2, 'https://example.com/guides/build-r2-a.pdf',
        NULL, NULL, 1, 120, false, false, false, 180, NULL, false, 10, NULL, :'rubric_json'),
    (5, now(), now(), 5, 2, 'https://example.com/guides/build-r2-b.pdf',
        NULL, NULL, 1, 120, false, false, false, 180, NULL, false, 10, NULL, :'rubric_json'),
    (6, now(), now(), 6, 2, 'https://example.com/guides/build-r2-c.pdf',
        NULL, NULL, 1, 120, false, false, false, 180, NULL, false, 10, NULL, :'rubric_json'),
    (7, now(), now(), NULL, 3, 'https://example.com/guides/build-final.pdf',
        NULL, NULL, NULL, 150, false, false, false, 240, NULL, false, 15, NULL, :'rubric_json'),

    -- Event 3: round 1 published; round 2 currently open and partially graded.
    (8, now(), now(), 7, 4, 'https://example.com/guides/ai-r1-a.pdf',
        now() - interval '9 days', now() - interval '8 days', 1, 120,
        true, true, true, 180, now() - interval '8 days' + interval '3 hours', false, 10,
        now() - interval '8 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (9, now(), now(), 8, 4, 'https://example.com/guides/ai-r1-b.pdf',
        now() - interval '9 days', now() - interval '8 days', 1, 120,
        true, true, true, 180, now() - interval '8 days' + interval '3 hours', false, 10,
        now() - interval '8 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (10, now(), now(), 7, 5, 'https://example.com/guides/ai-r2-a.pdf',
        now() - interval '2 hours', now() + interval '2 hours', 1, 240,
        false, false, false, 180, NULL, false, 10, NULL, :'rubric_json'),
    (11, now(), now(), 8, 5, 'https://example.com/guides/ai-r2-b.pdf',
        now() - interval '2 hours', now() + interval '2 hours', 1, 240,
        false, false, false, 180, NULL, false, 10, NULL, :'rubric_json'),
    (12, now(), now(), NULL, 6, 'https://example.com/guides/ai-final.pdf',
        NULL, NULL, NULL, 150, false, false, false, 240, NULL, false, 15, NULL, :'rubric_json'),

    -- Event 4: rounds 1 and 2 published; final closed and fully graded.
    (13, now(), now(), 9, 7, 'https://example.com/guides/grand-r1-a.pdf',
        now() - interval '18 days', now() - interval '17 days', 1, 120,
        true, true, true, 180, now() - interval '17 days' + interval '3 hours', false, 10,
        now() - interval '17 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (14, now(), now(), 10, 7, 'https://example.com/guides/grand-r1-b.pdf',
        now() - interval '18 days', now() - interval '17 days', 1, 120,
        true, true, true, 180, now() - interval '17 days' + interval '3 hours', false, 10,
        now() - interval '17 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (15, now(), now(), 9, 8, 'https://example.com/guides/grand-r2-a.pdf',
        now() - interval '10 days', now() - interval '9 days', 1, 120,
        true, true, true, 180, now() - interval '9 days' + interval '3 hours', false, 10,
        now() - interval '9 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (16, now(), now(), 10, 8, 'https://example.com/guides/grand-r2-b.pdf',
        now() - interval '10 days', now() - interval '9 days', 1, 120,
        true, true, true, 180, now() - interval '9 days' + interval '3 hours', false, 10,
        now() - interval '9 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (17, now(), now(), NULL, 9, 'https://example.com/guides/grand-final.pdf',
        now() - interval '4 hours', now() - interval '1 hour', NULL, 180,
        true, false, true, 180, now() + interval '2 hours', false, 15, NULL, :'rubric_json'),

    -- Event 5: every round and the final leaderboard are already published.
    (18, now(), now(), 11, 10, 'https://example.com/guides/archive-r1-a.pdf',
        now() - interval '48 days', now() - interval '47 days', 1, 120,
        true, true, true, 180, now() - interval '47 days' + interval '3 hours', false, 10,
        now() - interval '47 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (19, now(), now(), 12, 10, 'https://example.com/guides/archive-r1-b.pdf',
        now() - interval '48 days', now() - interval '47 days', 1, 120,
        true, true, true, 180, now() - interval '47 days' + interval '3 hours', false, 10,
        now() - interval '47 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (20, now(), now(), 11, 11, 'https://example.com/guides/archive-r2-a.pdf',
        now() - interval '38 days', now() - interval '37 days', 1, 120,
        true, true, true, 180, now() - interval '37 days' + interval '3 hours', false, 10,
        now() - interval '37 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (21, now(), now(), 12, 11, 'https://example.com/guides/archive-r2-b.pdf',
        now() - interval '38 days', now() - interval '37 days', 1, 120,
        true, true, true, 180, now() - interval '37 days' + interval '3 hours', false, 10,
        now() - interval '37 days' + interval '3 hours 10 minutes', :'rubric_json'),
    (22, now(), now(), NULL, 12, 'https://example.com/guides/archive-final.pdf',
        now() - interval '25 days', now() - interval '23 days', NULL, 180,
        true, true, true, 180, now() - interval '23 days' + interval '3 hours', false, 15,
        now() - interval '23 days' + interval '3 hours 15 minutes', :'rubric_json');

-- Mentors are group-level assignments. matrix_mentors is also populated because
-- the current backend still reads this legacy relationship in a few screens.
INSERT INTO matrix_mentors (matrix_id, mentor_id)
SELECT m.id, tm.mentor_id
FROM track_round_matrix m
JOIN track_mentors tm ON tm.track_id = m.track_id;

INSERT INTO matrix_mentors (matrix_id, mentor_id) VALUES
    (7, 4), (7, 6), (7, 8),
    (12, 5), (12, 7),
    (17, 4), (17, 5), (17, 6),
    (22, 4), (22, 6);

-- All judges are STAFF accounts. No judge is a mentor of the same group.
INSERT INTO matrix_judges (matrix_id, judge_id) VALUES
    (1, 9), (1, 10), (2, 9), (2, 11), (3, 10), (3, 11),
    (4, 9), (4, 10), (5, 9), (5, 11), (6, 10), (6, 11),
    (7, 11), (7, 12), (7, 13),

    (8, 9), (8, 10), (9, 9), (9, 11),
    (10, 9), (10, 10), (11, 9), (11, 11),
    (12, 10), (12, 12), (12, 13),

    (13, 9), (13, 10), (14, 9), (14, 11),
    (15, 9), (15, 10), (16, 9), (16, 11),
    (17, 10), (17, 11), (17, 12),

    (18, 9), (18, 10), (19, 9), (19, 11),
    (20, 9), (20, 10), (21, 9), (21, 11),
    (22, 10), (22, 11), (22, 12);

-- ---------------------------------------------------------------------------
-- 5. TEAMS AND MEMBERS
-- Each team has exactly 3 approved users. Users may join different teams in
-- different events, which makes it possible to test all events with 20 users.
-- ---------------------------------------------------------------------------

INSERT INTO teams (
    id, created_at, updated_at, name, description, type, join_password,
    track_id, event_id, disqualification_status, disqualification_reason,
    disqualifier_email, rejection_reason, skills_needed
) VALUES
    (1, now(), now(), 'Registration Rockets', 'Prototype planning workspace for student teams.', 'PUBLIC', NULL, 1, 1, NULL, NULL, NULL, NULL, 'React, UI/UX'),
    (2, now(), now(), 'Spring Pioneers', 'AI learning assistant for university courses.', 'PRIVATE', :'password_hash', 2, 1, NULL, NULL, NULL, NULL, 'Python, AI'),
    (3, now(), now(), 'Green Future Lab', 'Energy monitoring for campus buildings.', 'PUBLIC', NULL, 3, 1, 'PENDING', 'Possible use of copied source code.', 'staff8@seal.dev', NULL, 'IoT, Data'),

    (4, now(), now(), 'Code Forge', 'Collaborative developer environment.', 'PUBLIC', NULL, 4, 2, NULL, NULL, NULL, NULL, NULL),
    (5, now(), now(), 'Pixel Pilots', 'Accessible visual website builder.', 'PRIVATE', :'password_hash', 4, 2, NULL, NULL, NULL, NULL, NULL),
    (6, now(), now(), 'AI Orbit', 'Research assistant with verifiable citations.', 'PUBLIC', NULL, 5, 2, NULL, NULL, NULL, NULL, NULL),
    (7, now(), now(), 'Data Sparks', 'Real-time operational analytics.', 'PUBLIC', NULL, 5, 2, 'REJECTED', 'Initial disqualification proposal was rejected after review.', 'staff7@seal.dev', NULL, NULL),
    (8, now(), now(), 'Cloud Nine', 'Resilient deployment platform.', 'PUBLIC', NULL, 6, 2, NULL, NULL, NULL, NULL, NULL),

    (9, now(), now(), 'Vision Crew', 'Multimodal document understanding.', 'PUBLIC', NULL, 7, 3, NULL, NULL, NULL, NULL, NULL),
    (10, now(), now(), 'Neural Nest', 'Private on-device study assistant.', 'PUBLIC', NULL, 7, 3, NULL, NULL, NULL, NULL, NULL),
    (11, now(), now(), 'Secure Stack', 'AI safety evaluation dashboard.', 'PUBLIC', NULL, 8, 3, NULL, NULL, NULL, NULL, NULL),
    (12, now(), now(), 'Risky Bytes', 'Synthetic media generation toolkit.', 'PUBLIC', NULL, 8, 3, 'APPROVED', 'Use of an undeclared external project.', 'coordinator1@seal.dev', NULL, NULL),

    (13, now(), now(), 'Final Alpha', 'Student success and retention platform.', 'PUBLIC', NULL, 9, 4, NULL, NULL, NULL, NULL, NULL),
    (14, now(), now(), 'Final Beta', 'Inclusive campus navigation.', 'PUBLIC', NULL, 9, 4, NULL, NULL, NULL, NULL, NULL),
    (15, now(), now(), 'Final Gamma', 'Enterprise security automation.', 'PUBLIC', NULL, 10, 4, NULL, NULL, NULL, NULL, NULL),
    (16, now(), now(), 'Final Delta', 'Cloud cost optimization assistant.', 'PUBLIC', NULL, 10, 4, NULL, NULL, NULL, NULL, NULL),

    (17, now(), now(), 'Legacy One', 'Archived winning software project.', 'PUBLIC', NULL, 11, 5, NULL, NULL, NULL, NULL, NULL),
    (18, now(), now(), 'Legacy Two', 'Archived runner-up data platform.', 'PUBLIC', NULL, 11, 5, NULL, NULL, NULL, NULL, NULL),
    (19, now(), now(), 'Legacy Three', 'Archived responsible-data project.', 'PUBLIC', NULL, 12, 5, NULL, NULL, NULL, NULL, NULL);

INSERT INTO team_members (id, created_at, updated_at, team_id, user_id, role)
SELECT
    row_number() OVER (ORDER BY t.id, member_offset),
    now(),
    now(),
    t.id,
    14 + (((t.id - 1) * 3 + member_offset) % 17),
    CASE WHEN member_offset = 0 THEN 'LEADER' ELSE 'MEMBER' END
FROM teams t
CROSS JOIN generate_series(0, 2) AS gs(member_offset)
ORDER BY t.id, member_offset;

INSERT INTO team_join_requests (
    id, created_at, updated_at, team_id, user_id, status, type
) VALUES
    (1, now() - interval '2 hours', now() - interval '2 hours', 1, 31, 'PENDING', 'JOIN'),
    (2, now() - interval '2 days', now() - interval '1 day', 2, 32, 'REJECTED', 'JOIN'),
    (3, now() - interval '1 day', now() - interval '1 hour', 3, 30, 'APPROVED', 'INVITE');

-- ---------------------------------------------------------------------------
-- 6. SUBMISSIONS
--
-- IDs  1-5: Event 2 round 1, fully graded and ready to publish.
-- IDs  6-9: Event 3 round 1, published historical scores.
-- IDs 10-11: Event 3 round 2, partial and ungraded.
-- IDs 12-19: Event 4 qualifier/semifinal/final, all fully graded.
-- IDs 20-26: Event 5 completed history, all fully graded and locked.
-- ---------------------------------------------------------------------------

INSERT INTO submissions (
    id, created_at, updated_at, team_id, matrix_id, file_url,
    submission_data_json, is_flagged, flag_reason, score, feedback,
    criteria_scores_json, is_graded
) VALUES
    (1, now() - interval '3 hours', now() - interval '3 hours', 4, 1, 'https://github.com/seal-demo/code-forge-r1',
        '{"projectName":"Code Forge","repoUrl":"https://github.com/seal-demo/code-forge-r1","demoUrl":"https://demo.example.com/code-forge"}',
        false, NULL, NULL, NULL, NULL, false),
    (2, now() - interval '3 hours', now() - interval '3 hours', 5, 1, 'https://github.com/seal-demo/pixel-pilots-r1',
        '{"projectName":"Pixel Pilots","repoUrl":"https://github.com/seal-demo/pixel-pilots-r1","demoUrl":"https://demo.example.com/pixel-pilots"}',
        false, NULL, NULL, NULL, NULL, false),
    (3, now() - interval '3 hours', now() - interval '3 hours', 6, 2, 'https://github.com/seal-demo/ai-orbit-r1',
        '{"projectName":"AI Orbit","repoUrl":"https://github.com/seal-demo/ai-orbit-r1","demoUrl":"https://demo.example.com/ai-orbit"}',
        false, NULL, NULL, NULL, NULL, false),
    (4, now() - interval '3 hours', now() - interval '3 hours', 7, 2, 'https://github.com/seal-demo/data-sparks-r1',
        '{"projectName":"Data Sparks","repoUrl":"https://github.com/seal-demo/data-sparks-r1","demoUrl":"https://demo.example.com/data-sparks"}',
        false, NULL, NULL, NULL, NULL, false),
    (5, now() - interval '3 hours', now() - interval '3 hours', 8, 3, 'https://github.com/seal-demo/cloud-nine-r1',
        '{"projectName":"Cloud Nine","repoUrl":"https://github.com/seal-demo/cloud-nine-r1","demoUrl":"https://demo.example.com/cloud-nine"}',
        false, NULL, NULL, NULL, NULL, false),

    (6, now() - interval '9 days', now() - interval '9 days', 9, 8, 'https://github.com/seal-demo/vision-crew-r1',
        '{"projectName":"Vision Crew","modelCard":"https://example.com/model-cards/vision-crew","repoUrl":"https://github.com/seal-demo/vision-crew-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (7, now() - interval '9 days', now() - interval '9 days', 10, 8, 'https://github.com/seal-demo/neural-nest-r1',
        '{"projectName":"Neural Nest","modelCard":"https://example.com/model-cards/neural-nest","repoUrl":"https://github.com/seal-demo/neural-nest-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (8, now() - interval '9 days', now() - interval '9 days', 11, 9, 'https://github.com/seal-demo/secure-stack-r1',
        '{"projectName":"Secure Stack","modelCard":"https://example.com/model-cards/secure-stack","repoUrl":"https://github.com/seal-demo/secure-stack-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (9, now() - interval '9 days', now() - interval '9 days', 12, 9, 'https://github.com/seal-demo/risky-bytes-r1',
        '{"projectName":"Risky Bytes","modelCard":"https://example.com/model-cards/risky-bytes","repoUrl":"https://github.com/seal-demo/risky-bytes-r1"}',
        true, 'Submission later disqualified for undeclared external work.', NULL, NULL, NULL, false),

    (10, now() - interval '90 minutes', now() - interval '90 minutes', 9, 10, 'https://github.com/seal-demo/vision-crew-r2',
        '{"projectName":"Vision Crew","modelCard":"https://example.com/model-cards/vision-crew-r2","repoUrl":"https://github.com/seal-demo/vision-crew-r2"}',
        false, NULL, NULL, 'One of two assigned judges has submitted a score.', NULL, false),
    (11, now() - interval '70 minutes', now() - interval '70 minutes', 11, 11, 'https://github.com/seal-demo/secure-stack-r2',
        '{"projectName":"Secure Stack","modelCard":"https://example.com/model-cards/secure-stack-r2","repoUrl":"https://github.com/seal-demo/secure-stack-r2"}',
        true, 'Flagged for coordinator review; no judge has scored it yet.', NULL, NULL, NULL, false),

    (12, now() - interval '18 days', now() - interval '18 days', 13, 13, 'https://github.com/seal-demo/final-alpha-r1',
        '{"projectName":"Final Alpha","repoUrl":"https://github.com/seal-demo/final-alpha-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (13, now() - interval '18 days', now() - interval '18 days', 14, 13, 'https://github.com/seal-demo/final-beta-r1',
        '{"projectName":"Final Beta","repoUrl":"https://github.com/seal-demo/final-beta-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (14, now() - interval '18 days', now() - interval '18 days', 15, 14, 'https://github.com/seal-demo/final-gamma-r1',
        '{"projectName":"Final Gamma","repoUrl":"https://github.com/seal-demo/final-gamma-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (15, now() - interval '18 days', now() - interval '18 days', 16, 14, 'https://github.com/seal-demo/final-delta-r1',
        '{"projectName":"Final Delta","repoUrl":"https://github.com/seal-demo/final-delta-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (16, now() - interval '10 days', now() - interval '10 days', 13, 15, 'https://github.com/seal-demo/final-alpha-r2',
        '{"projectName":"Final Alpha","repoUrl":"https://github.com/seal-demo/final-alpha-r2"}',
        false, NULL, NULL, NULL, NULL, false),
    (17, now() - interval '10 days', now() - interval '10 days', 15, 16, 'https://github.com/seal-demo/final-gamma-r2',
        '{"projectName":"Final Gamma","repoUrl":"https://github.com/seal-demo/final-gamma-r2"}',
        false, NULL, NULL, NULL, NULL, false),
    (18, now() - interval '3 hours', now() - interval '3 hours', 13, 17, 'https://github.com/seal-demo/final-alpha-grand-final',
        '{"projectName":"Final Alpha","repoUrl":"https://github.com/seal-demo/final-alpha-grand-final","pitchDeck":"https://example.com/decks/final-alpha"}',
        false, NULL, NULL, NULL, NULL, false),
    (19, now() - interval '3 hours', now() - interval '3 hours', 15, 17, 'https://github.com/seal-demo/final-gamma-grand-final',
        '{"projectName":"Final Gamma","repoUrl":"https://github.com/seal-demo/final-gamma-grand-final","pitchDeck":"https://example.com/decks/final-gamma"}',
        false, NULL, NULL, NULL, NULL, false),

    (20, now() - interval '48 days', now() - interval '48 days', 17, 18, 'https://github.com/seal-demo/legacy-one-r1',
        '{"projectName":"Legacy One","repoUrl":"https://github.com/seal-demo/legacy-one-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (21, now() - interval '48 days', now() - interval '48 days', 18, 18, 'https://github.com/seal-demo/legacy-two-r1',
        '{"projectName":"Legacy Two","repoUrl":"https://github.com/seal-demo/legacy-two-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (22, now() - interval '48 days', now() - interval '48 days', 19, 19, 'https://github.com/seal-demo/legacy-three-r1',
        '{"projectName":"Legacy Three","repoUrl":"https://github.com/seal-demo/legacy-three-r1"}',
        false, NULL, NULL, NULL, NULL, false),
    (23, now() - interval '38 days', now() - interval '38 days', 17, 20, 'https://github.com/seal-demo/legacy-one-r2',
        '{"projectName":"Legacy One","repoUrl":"https://github.com/seal-demo/legacy-one-r2"}',
        false, NULL, NULL, NULL, NULL, false),
    (24, now() - interval '38 days', now() - interval '38 days', 19, 21, 'https://github.com/seal-demo/legacy-three-r2',
        '{"projectName":"Legacy Three","repoUrl":"https://github.com/seal-demo/legacy-three-r2"}',
        false, NULL, NULL, NULL, NULL, false),
    (25, now() - interval '25 days', now() - interval '25 days', 17, 22, 'https://github.com/seal-demo/legacy-one-final',
        '{"projectName":"Legacy One","repoUrl":"https://github.com/seal-demo/legacy-one-final"}',
        false, NULL, NULL, NULL, NULL, false),
    (26, now() - interval '25 days', now() - interval '25 days', 19, 22, 'https://github.com/seal-demo/legacy-three-final',
        '{"projectName":"Legacy Three","repoUrl":"https://github.com/seal-demo/legacy-three-final"}',
        false, NULL, NULL, NULL, NULL, false);

-- Full scores for all submissions except the two current round-2 test cases.
-- Each fully graded submission receives one score from every assigned judge.
INSERT INTO scores (
    id, created_at, updated_at, submission_id, judge_id,
    score_value, criteria_scores_json, comment
)
SELECT
    row_number() OVER (ORDER BY s.id, mj.judge_id),
    s.created_at + interval '30 minutes',
    s.created_at + interval '30 minutes',
    s.id,
    mj.judge_id,
    round((
        96.0
        - (s.id % 5) * 2.4
        - (mj.judge_id % 3) * 1.1
    )::numeric, 1)::double precision,
    jsonb_build_array(
        jsonb_build_object('id','problem','label','Problem and impact','maxScore',100,'weight',25,'score',88 + (s.id % 8),'note','Clear problem framing.'),
        jsonb_build_object('id','innovation','label','Innovation','maxScore',100,'weight',25,'score',84 + (s.id % 10),'note','Good differentiation.'),
        jsonb_build_object('id','technical','label','Technical quality','maxScore',100,'weight',30,'score',86 + (s.id % 9),'note','Implementation is stable.'),
        jsonb_build_object('id','presentation','label','Presentation','maxScore',100,'weight',20,'score',85 + (s.id % 10),'note','Demo and Q&A are clear.')
    )::text,
    'Seeded judge review from staff' || (mj.judge_id - 3) || '@seal.dev.'
FROM submissions s
JOIN matrix_judges mj ON mj.matrix_id = s.matrix_id
WHERE s.id NOT IN (10, 11);

-- One partial score: the second assigned judge still needs to grade submission 10.
INSERT INTO scores (
    id, created_at, updated_at, submission_id, judge_id,
    score_value, criteria_scores_json, comment
)
SELECT
    (SELECT max(id) + 1 FROM scores),
    now() - interval '30 minutes',
    now() - interval '30 minutes',
    10,
    min(mj.judge_id),
    91.0,
    $$[
      {"id":"problem","label":"Problem and impact","maxScore":100,"weight":25,"score":92,"note":"Strong problem framing."},
      {"id":"innovation","label":"Innovation","maxScore":100,"weight":25,"score":90,"note":"Meaningful differentiation."},
      {"id":"technical","label":"Technical quality","maxScore":100,"weight":30,"score":91,"note":"Reliable implementation."},
      {"id":"presentation","label":"Presentation","maxScore":100,"weight":20,"score":91,"note":"Clear demo."}
    ]$$,
    'First judge completed; waiting for the second judge.'
FROM matrix_judges mj
WHERE mj.matrix_id = 10;

-- Calculate and snapshot aggregate scores for fully graded submissions.
WITH aggregates AS (
    SELECT
        s.id AS submission_id,
        round(avg(sc.score_value)::numeric, 1)::double precision AS average_score
    FROM submissions s
    JOIN scores sc ON sc.submission_id = s.id
    WHERE s.id NOT IN (10, 11)
    GROUP BY s.id
)
UPDATE submissions s
SET
    score = a.average_score,
    feedback = 'All assigned judges completed scoring. Aggregate score is ready.',
    criteria_scores_json = :'rubric_json',
    is_graded = true,
    updated_at = now()
FROM aggregates a
WHERE s.id = a.submission_id;

UPDATE submissions
SET score = 91.0,
    feedback = 'Partially graded: 1 of 2 judges completed.',
    criteria_scores_json = :'rubric_json',
    is_graded = false
WHERE id = 10;

-- ---------------------------------------------------------------------------
-- 7. PRIZES
-- Active-event prizes have no winner yet. Completed-event prizes do.
-- ---------------------------------------------------------------------------

INSERT INTO prizes (id, created_at, updated_at, name, description, event_id, team_id) VALUES
    (1, now(), now(), 'Champion', '20,000,000 VND and three months of mentoring.', 1, NULL),
    (2, now(), now(), 'Runner-up', '10,000,000 VND.', 1, NULL),
    (3, now(), now(), 'Best Innovation', 'Special award for the most original solution.', 1, NULL),

    (4, now(), now(), 'Champion', '30,000,000 VND and incubation support.', 2, NULL),
    (5, now(), now(), 'Runner-up', '15,000,000 VND.', 2, NULL),
    (6, now(), now(), 'Best Engineering', 'Award for technical excellence.', 2, NULL),

    (7, now(), now(), 'Champion', '30,000,000 VND and AI cloud credits.', 3, NULL),
    (8, now(), now(), 'Responsible AI Award', 'Award for safety and transparency.', 3, NULL),

    (9, now(), now(), 'Grand Champion', '50,000,000 VND and a SEAL trophy.', 4, NULL),
    (10, now(), now(), 'Grand Runner-up', '25,000,000 VND.', 4, NULL),

    (11, now(), now(), 'Champion', 'Historical first prize.', 5, 17),
    (12, now(), now(), 'Runner-up', 'Historical second prize.', 5, 19),
    (13, now(), now(), 'Community Choice', 'Historical community award.', 5, 18),

    (14, now(), now(), 'Champion', '40,000,000 VND and product incubation.', 6, NULL),
    (15, now(), now(), 'Best Future Technology', 'Special innovation award.', 6, NULL);

-- ---------------------------------------------------------------------------
-- 8. NOTIFICATIONS, READ STATE, CHAT, AUDIT, SETTINGS AND RULE TEMPLATES
-- ---------------------------------------------------------------------------

INSERT INTO notifications (
    id, created_at, updated_at, title, body, target_role,
    recipient_id, sender_id, action_url
) VALUES
    (1, now() - interval '3 days', now() - interval '3 days',
        'Registration is open', 'SEAL Summer Launch 2026 is accepting teams.', 'USER',
        NULL, 2, '/events/1'),
    (2, now() - interval '2 hours', now() - interval '2 hours',
        'Round 1 grading completed', 'All Build Sprint round 1 submissions have been graded and are ready for publication.', 'COORDINATOR',
        NULL, 1, '/dashboard/events'),
    (3, now() - interval '30 minutes', now() - interval '30 minutes',
        'A submission still needs your score', 'Vision Crew round 2 is waiting for the second assigned judge.', NULL,
        10, 2, '/dashboard/grading'),
    (4, now() - interval '25 minutes', now() - interval '25 minutes',
        'Submission flagged for review', 'Secure Stack round 2 is flagged and has not been graded.', NULL,
        3, 9, '/dashboard/grading'),
    (5, now() - interval '1 day', now() - interval '1 day',
        'Team disqualified', 'Risky Bytes was disqualified after coordinator review.', NULL,
        24, 2, '/my-team'),
    (6, now() - interval '20 days', now() - interval '20 days',
        'Final results published', 'SEAL Spring Archive 2026 results and prizes are now public.', 'USER',
        NULL, 2, '/events/5/results');

INSERT INTO notification_reads (
    id, created_at, updated_at, notification_id, user_id
) VALUES
    (1, now() - interval '2 days', now() - interval '2 days', 1, 14),
    (2, now() - interval '1 hour', now() - interval '1 hour', 2, 2),
    (3, now() - interval '19 days', now() - interval '19 days', 6, 14);

INSERT INTO chat_messages (
    id, created_at, updated_at, team_id, sender_id, content
) VALUES
    (1, now() - interval '6 hours', now() - interval '6 hours', 1, 4,
        'Please clarify the primary user journey before the mentor review.'),
    (2, now() - interval '5 hours', now() - interval '5 hours', 1, 14,
        'We updated the prototype and added the user journey to the project brief.'),
    (3, now() - interval '4 hours', now() - interval '4 hours', 2, 6,
        'Remember to include dataset limitations in the model card.'),
    (4, now() - interval '90 minutes', now() - interval '90 minutes', 9, 5,
        'Round 2 is open. Focus the demo on evaluation quality and failure cases.');

INSERT INTO audit_logs (
    id, created_at, updated_at, score_id, judge_id,
    old_score, new_score, team_name, reason
) VALUES
    (1, now() - interval '8 days', now() - interval '8 days',
        (SELECT min(id) FROM scores WHERE submission_id = 6), 9,
        89.0, 91.0, 'Vision Crew', 'Judge corrected a criterion total before round publication.'),
    (2, now() - interval '7 days', now() - interval '7 days',
        NULL, 2, NULL, NULL, 'SEAL AI Challenge 2026', 'RESULTS ANNOUNCEMENT: Round 1 published and Top N advanced.'),
    (3, now() - interval '1 day', now() - interval '1 day',
        NULL, 2, NULL, NULL, 'Risky Bytes', 'TEAM DISQUALIFIED: undeclared external project confirmed by coordinator.');

INSERT INTO system_settings (
    id, created_at, updated_at, setting_key, setting_value
) VALUES
    (1, now(), now(), 'systemName', 'SEAL Hackathon Management System'),
    (2, now(), now(), 'supportEmail', 'sealfpt@gmail.com'),
    (3, now(), now(), 'maintenanceMode', 'false'),
    (4, now(), now(), 'registrationEnabled', 'true'),
    (5, now(), now(), 'sessionTimeoutMinutes', '120');

INSERT INTO rule_templates (
    id, created_at, updated_at, name, content
) VALUES
    (1, now(), now(), 'Standard Hackathon Rules',
        'Teams need 3-5 approved members. Submissions must be original and uploaded before the deadline.'),
    (2, now(), now(), 'AI Safety Rules',
        'Teams must disclose datasets, models, external services, evaluation methods and known limitations.'),
    (3, now(), now(), 'Final Pitch Rules',
        'Finalists receive 10 minutes to pitch, 5 minutes for demo and 5 minutes for questions.');

INSERT INTO system_activities (
    id, created_at, updated_at, actor_email, action, detail
) VALUES
    (1, now() - interval '3 days', now() - interval '3 days',
        'coordinator1@seal.dev', 'EVENT_UPDATED', 'Updated registration dates for SEAL Summer Launch 2026.'),
    (2, now() - interval '2 hours', now() - interval '2 hours',
        'staff6@seal.dev', 'SCORE_SUBMITTED', 'Completed the final missing score for Build Sprint round 1.'),
    (3, now() - interval '1 day', now() - interval '1 day',
        'coordinator1@seal.dev', 'TEAM_DISQUALIFIED', 'Approved the disqualification of Risky Bytes.');

-- ---------------------------------------------------------------------------
-- 9. RESET ALL IDENTITY SEQUENCES
-- ---------------------------------------------------------------------------

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users), true);
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT max(id) FROM events), true);
SELECT setval(pg_get_serial_sequence('tracks', 'id'), (SELECT max(id) FROM tracks), true);
SELECT setval(pg_get_serial_sequence('rounds', 'id'), (SELECT max(id) FROM rounds), true);
SELECT setval(pg_get_serial_sequence('track_round_matrix', 'id'), (SELECT max(id) FROM track_round_matrix), true);
SELECT setval(pg_get_serial_sequence('teams', 'id'), (SELECT max(id) FROM teams), true);
SELECT setval(pg_get_serial_sequence('team_members', 'id'), (SELECT max(id) FROM team_members), true);
SELECT setval(pg_get_serial_sequence('team_join_requests', 'id'), (SELECT max(id) FROM team_join_requests), true);
SELECT setval(pg_get_serial_sequence('submissions', 'id'), (SELECT max(id) FROM submissions), true);
SELECT setval(pg_get_serial_sequence('scores', 'id'), (SELECT max(id) FROM scores), true);
SELECT setval(pg_get_serial_sequence('prizes', 'id'), (SELECT max(id) FROM prizes), true);
SELECT setval(pg_get_serial_sequence('notifications', 'id'), (SELECT max(id) FROM notifications), true);
SELECT setval(pg_get_serial_sequence('notification_reads', 'id'), (SELECT max(id) FROM notification_reads), true);
SELECT setval(pg_get_serial_sequence('chat_messages', 'id'), (SELECT max(id) FROM chat_messages), true);
SELECT setval(pg_get_serial_sequence('audit_logs', 'id'), (SELECT max(id) FROM audit_logs), true);
SELECT setval(pg_get_serial_sequence('system_settings', 'id'), (SELECT max(id) FROM system_settings), true);
SELECT setval(pg_get_serial_sequence('rule_templates', 'id'), (SELECT max(id) FROM rule_templates), true);
SELECT setval(pg_get_serial_sequence('system_activities', 'id'), (SELECT max(id) FROM system_activities), true);

COMMIT;

-- Verification summary shown after a successful run.
SELECT role, status, count(*) AS account_count
FROM users
GROUP BY role, status
ORDER BY role, status;

SELECT
    e.id,
    e.name,
    e.is_active,
    e.results_published,
    count(DISTINCT t.id) AS teams,
    count(DISTINCT m.id) AS round_instances,
    count(DISTINCT s.id) AS submissions
FROM events e
LEFT JOIN teams t ON t.event_id = e.id
LEFT JOIN rounds r ON r.event_id = e.id
LEFT JOIN track_round_matrix m ON m.round_id = r.id
LEFT JOIN submissions s ON s.matrix_id = m.id
GROUP BY e.id, e.name, e.is_active, e.results_published
ORDER BY e.id;

SELECT
    count(*) FILTER (WHERE is_graded = true) AS fully_graded_submissions,
    count(*) FILTER (WHERE is_graded = false) AS pending_submissions,
    count(*) FILTER (WHERE is_flagged = true) AS flagged_submissions,
    count(*) AS total_submissions
FROM submissions;
