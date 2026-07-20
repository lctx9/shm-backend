-- =============================================================
-- SCRIPT TEST: Thêm 5 tài khoản thí sinh chờ duyệt (PENDING)
-- Mục đích: Kiểm tra badge số lượng trên thanh nav cập nhật ngay
--           sau khi Coordinator duyệt hoặc từ chối thí sinh
-- =============================================================
-- Chạy xong → vào dashboard/student-approval → Duyệt 1 thí sinh
-- → Badge số đếm phải giảm ngay lập tức, không cần tải lại trang
-- =============================================================

-- Mật khẩu được mã hoá bằng BCrypt của chuỗi: "Test@1234"
-- Bạn có thể đăng nhập bằng email + mật khẩu này nếu muốn test thêm

INSERT INTO users (
    full_name,
    email,
    password,
    student_id,
    is_fpt_student,
    university_name,
    avatar_url,
    student_card_url,
    rejection_reason,
    role,
    status,
    created_at,
    updated_at
) VALUES
(
    N'Nguyễn Test Một',
    'test.student.1@gmail.com',
    '$2a$10$9X7v7QDjGy8V3KzPqkXJJuYlBxpEhYkNzWZBTY8T1E3n.m9C5MRMK',
    'SE180001',
    true,
    'FPT University',
    NULL,
    NULL,
    NULL,
    'USER',
    'PENDING',
    NOW(),
    NOW()
),
(
    N'Trần Test Hai',
    'test.student.2@gmail.com',
    '$2a$10$9X7v7QDjGy8V3KzPqkXJJuYlBxpEhYkNzWZBTY8T1E3n.m9C5MRMK',
    'SE180002',
    true,
    'FPT University',
    NULL,
    NULL,
    NULL,
    'USER',
    'PENDING',
    NOW(),
    NOW()
),
(
    N'Lê Test Ba',
    'test.student.3@gmail.com',
    '$2a$10$9X7v7QDjGy8V3KzPqkXJJuYlBxpEhYkNzWZBTY8T1E3n.m9C5MRMK',
    'HE180003',
    false,
    'Hanoi University of Science and Technology',
    NULL,
    NULL,
    NULL,
    'USER',
    'PENDING',
    NOW(),
    NOW()
),
(
    N'Phạm Test Bốn',
    'test.student.4@gmail.com',
    '$2a$10$9X7v7QDjGy8V3KzPqkXJJuYlBxpEhYkNzWZBTY8T1E3n.m9C5MRMK',
    'BA180004',
    false,
    'Bach Khoa University',
    NULL,
    NULL,
    NULL,
    'USER',
    'PENDING',
    NOW(),
    NOW()
),
(
    N'Hoàng Test Năm',
    'test.student.5@gmail.com',
    '$2a$10$9X7v7QDjGy8V3KzPqkXJJuYlBxpEhYkNzWZBTY8T1E3n.m9C5MRMK',
    'SE180005',
    true,
    'FPT University',
    NULL,
    NULL,
    NULL,
    'USER',
    'PENDING',
    NOW(),
    NOW()
);

-- =============================================================
-- Kiểm tra sau khi insert:
SELECT id, full_name, email, role, status
FROM users
WHERE email LIKE 'test.student.%'
ORDER BY created_at DESC;

-- =============================================================
-- DỌN DẸP SAU KHI TEST XONG (chạy lệnh này để xoá dữ liệu test):
-- DELETE FROM users WHERE email LIKE 'test.student.%';
-- =============================================================
