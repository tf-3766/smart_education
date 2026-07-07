-- 仅 local/test profile 加载。ID 使用固定值，便于 Postman 与自动化测试复现。
INSERT INTO sys_user
    (id, username, password_hash, display_name, user_status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (1004, 'teacher2', '$2b$10$.64FnHJo8pzAoAV1QYz/WeuHQOG47AjComkIXA6OFuc6/Gk8zQRgO', '测试教师二', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), user_status = VALUES(user_status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role
    (id, user_id, role_id, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (4004, 1004, 2002, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course
    (id, course_code, name, summary, cover_url, category_id, term, department, credit,
     owner_teacher_id, status, review_status, enrollment_open_at, enrollment_close_at, start_at, end_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (21001, 'COURSE-PUBLISHED-001', '已发布测试课程', '用于学生学习闭环测试', NULL, 1, '2026-FALL', '计算机学院', 3.00,
     1002, 'PUBLISHED', 'APPROVED', '2020-01-01 00:00:00', '2099-12-31 23:59:59', '2020-01-01 00:00:00', '2099-12-31 23:59:59',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (21002, 'COURSE-DRAFT-001', '草稿测试课程', '不可选课', NULL, 1, '2026-FALL', '计算机学院', 2.00,
     1004, 'DRAFT', 'NOT_SUBMITTED', NULL, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21003, 'COURSE-OTHER-001', '其他教师课程', '用于越权测试', NULL, 2, '2026-FALL', '数学学院', 2.00,
     1004, 'PUBLISHED', 'APPROVED', '2020-01-01 00:00:00', '2099-12-31 23:59:59', '2020-01-01 00:00:00', '2099-12-31 23:59:59',
     CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21004, 'COURSE-OFFLINE-001', '已下线测试课程', '不可新增选课', NULL, 2, '2026-FALL', '计算机学院', 1.00,
     1002, 'OFFLINE', 'APPROVED', '2020-01-01 00:00:00', '2099-12-31 23:59:59', '2020-01-01 00:00:00', '2099-12-31 23:59:59',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), summary = VALUES(summary), status = VALUES(status), review_status = VALUES(review_status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_teacher
    (id, course_id, teacher_id, role, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (21101, 21001, 1002, 'OWNER', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (21102, 21002, 1004, 'OWNER', CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21103, 21003, 1004, 'OWNER', CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21104, 21004, 1002, 'OWNER', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE role = VALUES(role), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_enrollment
    (id, course_id, student_id, status, enrolled_at, withdrawn_at, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (21201, 21001, 1001, 'ENROLLED', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0, 0)
ON DUPLICATE KEY UPDATE status = VALUES(status), withdrawn_at = NULL, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_chapter
    (id, course_id, title, description, sort_order, status, published_at, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (22001, 21001, '第一章 已发布', '公开章节', 10, 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (22002, 21001, '第二章 草稿', '学生不可见', 20, 'DRAFT', NULL, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (22003, 21003, '其他课程章节', '用于越权测试', 10, 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_lesson
    (id, course_id, chapter_id, title, content_type, content, video_url, estimated_minutes, sort_order,
     status, unlock_type, unlock_at, published_at, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (23001, 21001, 22001, '公开课时', 'RICH_TEXT', '# 公开课时', NULL, 30, 10,
     'PUBLISHED', 'IMMEDIATE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (23002, 21001, 22001, '未解锁课时', 'RICH_TEXT', '# 未解锁', NULL, 30, 20,
     'PUBLISHED', 'SCHEDULED', '2099-12-31 23:59:59', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (23003, 21001, 22002, '草稿章节中的课时', 'RICH_TEXT', '# 不可见', NULL, 20, 10,
     'PUBLISHED', 'IMMEDIATE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (23004, 21003, 22003, '其他课程公开课时', 'RICH_TEXT', '# 越权不可见', NULL, 20, 10,
     'PUBLISHED', 'IMMEDIATE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), unlock_type = VALUES(unlock_type), unlock_at = VALUES(unlock_at), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_material
    (id, course_id, chapter_id, lesson_id, name, material_type, file_key, file_url, file_size, mime_type,
     visibility, status, sort_order, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (24001, 21001, 22001, 23001, '公开课时资料', 'DOCUMENT', 'mock/course-21001/lesson-23001.pdf', NULL, 1024, 'application/pdf',
     'LESSON', 'PUBLISHED', 10, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (24002, 21003, 22003, 23004, '其他课程资料', 'DOCUMENT', 'mock/course-21003/lesson-23004.pdf', NULL, 1024, 'application/pdf',
     'LESSON', 'PUBLISHED', 10, CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (24003, 21001, 22002, NULL, '草稿章节资料', 'DOCUMENT', 'mock/course-21001/chapter-22002.pdf', NULL, 1024, 'application/pdf',
     'CHAPTER', 'PUBLISHED', 10, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_course_review
    (id, course_id, review_status, reviewer_id, reason, remark, reviewed_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (25001, 21001, 'APPROVED', 1003, NULL, '本地测试审核通过', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (25002, 21003, 'APPROVED', 1003, NULL, '本地测试审核通过', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (25003, 21004, 'APPROVED', 1003, NULL, '本地测试审核通过', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0)
ON DUPLICATE KEY UPDATE review_status = VALUES(review_status), reviewer_id = VALUES(reviewer_id), reason = VALUES(reason), remark = VALUES(remark), deleted = 0, updated_at = CURRENT_TIMESTAMP;
