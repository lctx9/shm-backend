-- =========================================================================
-- 1. CHÈN DỮ LIỆU MẪU CHO BẢNG USERS (Mật khẩu mặc định đều để thô là '123456')
-- =========================================================================

-- Tài khoản Admin (Platform Owner)
INSERT INTO users (id, email, password, full_name, status, is_guest_judge, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'admin@fpt.edu.vn', '1', 'Admin', 'ACTIVE', false, NOW())
ON CONFLICT (email) DO NOTHING;

-- Tài khoản Coordinator (Ban tổ chức - SE Dept / PDP Staff)
INSERT INTO users (id, email, password, full_name, status, is_guest_judge, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'coordinator@fpt.edu.vn', '1', 'BTC', 'ACTIVE', false, NOW())
ON CONFLICT (email) DO NOTHING;

-- Tài khoản Staff Đa nhiệm (Vừa làm Giám khảo Vòng 1, vừa làm Mentor cho Track B)
INSERT INTO users (id, email, password, full_name, status, is_guest_judge, created_at)
VALUES ('33333333-3333-3333-3333-333333333333', 'staff01@fpt.edu.vn', '1', 'Dr. Nguyễn Văn Chuyên Gia', 'ACTIVE', false, NOW())
ON CONFLICT (email) DO NOTHING;


-- Tài khoản Thí sinh nội bộ (Sinh viên FPT - Đã được duyệt ACTIVE)
INSERT INTO users (id, email, password, full_name, status, student_type, student_id, university_name, is_guest_judge, created_at)
VALUES ('55555555-5555-5555-5555-555555555555', 'letrangiahuy@fpt.edu.vn', '1', 'Lê Trần Gia Huy', 'ACTIVE', 'FPT', 'SE193344', 'FPT University', false, NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, email, password, full_name, status, student_type, student_id, university_name, is_guest_judge, created_at)
VALUES ('55555555-5555-5555-5555-555555555551', 'nguyentienphu@fpt.edu.vn', '1', 'Nguyễn Tiến Phú', 'ACTIVE', 'FPT', 'SE190131', 'FPT University', false, NOW())
ON CONFLICT (email) DO NOTHING;


INSERT INTO users (id, email, password, full_name, status, student_type, student_id, university_name, is_guest_judge, created_at)
VALUES ('55555555-5555-5555-5555-555555555552', 'lechitam@fpt.edu.vn', '1', 'Lê Chí Tâm', 'ACTIVE', 'FPT', 'SE190322', 'FPT University', false, NOW())
ON CONFLICT (email) DO NOTHING;

-- Tài khoản Thí sinh ngoài trường (Sinh viên ĐH Bách Khoa - Đang chờ duyệt PENDING)
INSERT INTO users (id, email, password, full_name, status, student_type, student_id, university_name, is_guest_judge, created_at)
VALUES ('66666666-6666-6666-6666-666666666666', 'student01@gmail.com', '123456', 'Nguyễn Hoàng Nam', 'PENDING', 'EXTERNAL', 'BK2026', 'ĐH Bách Khoa TP.HCM', false, NOW())
ON CONFLICT (email) DO NOTHING;


-- =========================================================================
-- 2. ÁNH XẠ QUYỀN VÀO BẢNG TRUNG GIAN USER_ROLES (@ManyToMany)
-- Định nghĩa ID của các Role (đã nạp ở bước trước):
-- 1: ADMIN, 2: COORDINATOR, 3: JUDGE, 4: MENTOR, 5: STUDENT
-- =========================================================================

-- Gắn quyền ADMIN cho tài khoản admin@fpt.edu.vn
INSERT INTO user_roles (user_id, role_id)
VALUES ('11111111-1111-1111-1111-111111111111', 1) ON CONFLICT DO NOTHING;

-- Gắn quyền COORDINATOR cho tài khoản coordinator@fpt.edu.vn
INSERT INTO user_roles (user_id, role_id)
VALUES ('22222222-2222-2222-2222-222222222222', 2) ON CONFLICT DO NOTHING;

-- CASE ĐA NHIỆM: Gắn song song quyền JUDGE (3) và MENTOR (4) cho tài khoản staff.expert@fpt.edu.vn
INSERT INTO user_roles (user_id, role_id)
VALUES ('33333333-3333-3333-3333-333333333333', 3) ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
VALUES ('33333333-3333-3333-3333-333333333333', 4) ON CONFLICT DO NOTHING;

-- Gắn quyền JUDGE cho Giám khảo doanh nghiệp (john.doe@fpt.software)
INSERT INTO user_roles (user_id, role_id)
VALUES ('44444444-4444-4444-4444-444444444444', 3) ON CONFLICT DO NOTHING;

-- Gắn quyền STUDENT cho thí sinh FPT
INSERT INTO user_roles (user_id, role_id)
VALUES ('55555555-5555-5555-5555-555555555555', 5) ON CONFLICT DO NOTHING;

-- Gắn quyền STUDENT cho thí sinh ngoài trường Bách Khoa
INSERT INTO user_roles (user_id, role_id)
VALUES ('66666666-6666-6666-6666-666666666666', 5) ON CONFLICT DO NOTHING;