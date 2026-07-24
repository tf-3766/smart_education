-- Online education auxiliary teaching system bootstrap script
-- Target: an empty MySQL 8.4 database using utf8mb4.
-- Includes the complete schema and local demo data used by this project.
-- This is the only database initialization source for local development.

-- Force the client session to utf8mb4 so Chinese seed data survives loads
-- from docker-entrypoint-initdb.d or clients whose default charset differs.
SET NAMES utf8mb4;

-- ============================================================================
-- Authentication, authorization, and managed file schema
-- ============================================================================

CREATE TABLE sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    avatar_file_id BIGINT NULL,
    user_status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE INDEX idx_user_status_deleted ON sys_user (user_status, deleted, id);
CREATE INDEX idx_user_avatar ON sys_user (avatar_file_id, deleted, id);

CREATE TABLE sys_file (
    id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    storage_provider VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    file_status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_file_object_key UNIQUE (object_key)
);

CREATE INDEX idx_file_owner_status ON sys_file (owner_user_id, file_status, deleted, created_at, id);
CREATE INDEX idx_file_purpose_status ON sys_file (purpose, file_status, deleted, id);

CREATE TABLE sys_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (role_code)
);

CREATE TABLE sys_permission (
    id BIGINT NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_role_user ON sys_user_role (user_id, deleted, role_id);
CREATE INDEX idx_user_role_role ON sys_user_role (role_id, deleted, user_id);

CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permission_role ON sys_role_permission (role_id, deleted, permission_id);
CREATE INDEX idx_role_permission_permission ON sys_role_permission (permission_id, deleted, role_id);

INSERT INTO sys_role
    (id, role_code, role_name, enabled, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (2001, 'STUDENT', '学生', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (2002, 'TEACHER', '教师', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (2003, 'ADMIN', '管理员', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (2004, 'SUPER_ADMIN', '超级管理员', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_permission
    (id, permission_code, permission_name, enabled, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (3001, 'auth:profile:read', '查看本人资料', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3002, 'student:access', '访问学生入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3003, 'teacher:access', '访问教师入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3004, 'admin:access', '访问管理员入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3005, 'admin:manage', '授予或撤销管理员角色', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_role_permission
    (id, role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (5001, 2001, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5002, 2001, 3002, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5003, 2002, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5004, 2002, 3003, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5005, 2003, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5006, 2003, 3004, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5007, 2004, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5008, 2004, 3004, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5009, 2004, 3005, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);


-- ============================================================================
-- Course, category, enrollment, content, and learning schema
-- ============================================================================

CREATE TABLE edu_course_category (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_category_name UNIQUE (name)
);

CREATE INDEX idx_course_category_enabled ON edu_course_category (enabled, deleted, sort_order, id);

INSERT INTO edu_course_category
    (id, name, sort_order, enabled, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (1, '计算机与软件', 10, 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);

CREATE TABLE edu_term_enrollment_window (
    id BIGINT NOT NULL,
    term VARCHAR(32) NOT NULL,
    enrollment_open_at DATETIME(3) NULL,
    enrollment_close_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_term_enrollment_window UNIQUE (term)
);

CREATE INDEX idx_term_enrollment_window_term ON edu_term_enrollment_window (deleted, term);

INSERT INTO edu_term_enrollment_window
    (id, term, enrollment_open_at, enrollment_close_at, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (20001, '2026 秋季', '2026-08-20 01:00:00', '2026-09-05 09:00:00', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);
CREATE TABLE edu_course (
    id BIGINT NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    summary TEXT NULL,
    cover_url VARCHAR(1024) NULL,
    category_id BIGINT NULL,
    term VARCHAR(32) NULL,
    department VARCHAR(128) NULL,
    credit DECIMAL(5,2) NULL,
    owner_teacher_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    enrollment_open_at DATETIME(3) NULL,
    enrollment_close_at DATETIME(3) NULL,
    start_at DATETIME(3) NULL,
    end_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_course_code ON edu_course (course_code, deleted, id);
CREATE INDEX idx_course_owner_status ON edu_course (owner_teacher_id, deleted, status, updated_at);
CREATE INDEX idx_course_review_queue ON edu_course (review_status, deleted, updated_at, id);
CREATE INDEX idx_course_catalog ON edu_course (status, review_status, enrollment_open_at, enrollment_close_at, deleted);
CREATE INDEX idx_course_term_category ON edu_course (term, category_id, deleted, id);

CREATE TABLE edu_course_teacher (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_teacher UNIQUE (course_id, teacher_id)
);

CREATE INDEX idx_course_teacher_teacher ON edu_course_teacher (teacher_id, deleted, status, role, course_id);
CREATE INDEX idx_course_teacher_course_role ON edu_course_teacher (course_id, role, status, deleted, teacher_id);

CREATE TABLE edu_course_enrollment (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    enrolled_at DATETIME(3) NOT NULL,
    withdrawn_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_enrollment UNIQUE (course_id, student_id)
);

CREATE INDEX idx_enrollment_student_status ON edu_course_enrollment (student_id, status, deleted, course_id);
CREATE INDEX idx_enrollment_course_status ON edu_course_enrollment (course_id, status, deleted, student_id);

CREATE TABLE edu_course_chapter (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT NULL,
    sort_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_chapter_course_order ON edu_course_chapter (course_id, deleted, sort_order, id);
CREATE INDEX idx_chapter_course_status ON edu_course_chapter (course_id, status, deleted, sort_order);

CREATE TABLE edu_course_lesson (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    content TEXT NULL,
    video_url VARCHAR(1024) NULL,
    estimated_minutes INT NULL,
    sort_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    unlock_type VARCHAR(32) NOT NULL,
    unlock_at DATETIME(3) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_lesson_chapter_order ON edu_course_lesson (chapter_id, deleted, sort_order, id);
CREATE INDEX idx_lesson_course_status ON edu_course_lesson (course_id, status, deleted, chapter_id, sort_order);
CREATE INDEX idx_lesson_unlock ON edu_course_lesson (course_id, status, unlock_type, unlock_at, deleted);

CREATE TABLE edu_course_material (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NULL,
    lesson_id BIGINT NULL,
    name VARCHAR(160) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    file_id BIGINT NULL,
    file_key VARCHAR(512) NULL,
    file_url VARCHAR(1024) NULL,
    file_size BIGINT NULL,
    mime_type VARCHAR(128) NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_material_course ON edu_course_material (course_id, status, deleted, sort_order, id);
CREATE INDEX idx_material_chapter ON edu_course_material (chapter_id, status, deleted, sort_order, id);
CREATE INDEX idx_material_lesson ON edu_course_material (lesson_id, status, deleted, sort_order, id);
CREATE INDEX idx_material_file ON edu_course_material (file_id, deleted, id);

CREATE TABLE edu_lesson_learning_record (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    last_studied_at DATETIME(3) NULL,
    study_seconds BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_lesson_student UNIQUE (lesson_id, student_id)
);

CREATE INDEX idx_learning_student_course ON edu_lesson_learning_record (student_id, course_id, status, deleted, last_studied_at);
CREATE INDEX idx_learning_course_lesson ON edu_lesson_learning_record (course_id, lesson_id, status, deleted, student_id);


-- ============================================================================
-- Course review schema
-- ============================================================================

CREATE TABLE edu_course_review (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    remark VARCHAR(500) NULL,
    reviewed_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_course_review_history ON edu_course_review (course_id, deleted, reviewed_at, id);
CREATE INDEX idx_course_review_status ON edu_course_review (review_status, deleted, reviewed_at, course_id);


-- ============================================================================
-- Assignment, grade, forum, announcement, warning, exam, and AI audit schema
-- ============================================================================

CREATE TABLE edu_assignment (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    response_mode VARCHAR(32) NOT NULL DEFAULT 'MIXED',
    questions_json TEXT NULL,
    max_score DECIMAL(7,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    open_at DATETIME(3) NULL,
    due_at DATETIME(3) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_assignment_course_status ON edu_assignment (course_id, status, deleted, due_at, id);
CREATE INDEX idx_assignment_lesson ON edu_assignment (lesson_id, deleted, id);

CREATE TABLE edu_assignment_attachment (
    id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    file_id BIGINT NULL,
    file_key VARCHAR(512) NULL,
    file_url VARCHAR(1024) NULL,
    file_size BIGINT NULL,
    mime_type VARCHAR(128) NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_assignment_attachment_assignment ON edu_assignment_attachment (assignment_id, deleted, sort_order, id);
CREATE INDEX idx_assignment_attachment_file ON edu_assignment_attachment (file_id, deleted, id);

CREATE TABLE edu_assignment_submission (
    id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    content TEXT NULL,
    answers_json TEXT NULL,
    file_id BIGINT NULL,
    file_key VARCHAR(512) NULL,
    file_url VARCHAR(1024) NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at DATETIME(3) NULL,
    score DECIMAL(7,2) NULL,
    teacher_comment VARCHAR(1000) NULL,
    ai_comment_draft_id BIGINT NULL,
    graded_by BIGINT NULL,
    graded_at DATETIME(3) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_assignment_submission_attempt UNIQUE (assignment_id, student_id, attempt_no)
);

CREATE INDEX idx_submission_assignment_status ON edu_assignment_submission (assignment_id, status, deleted, submitted_at, id);
CREATE INDEX idx_submission_student_course ON edu_assignment_submission (student_id, course_id, status, deleted, updated_at);
CREATE INDEX idx_submission_file ON edu_assignment_submission (file_id, deleted, id);

CREATE TABLE edu_grade_record (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    score DECIMAL(7,2) NOT NULL,
    max_score DECIMAL(7,2) NOT NULL,
    weight DECIMAL(5,2) NULL,
    grade_status VARCHAR(32) NOT NULL,
    comment VARCHAR(1000) NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_grade_source_student UNIQUE (source_type, source_id, student_id)
);

CREATE INDEX idx_grade_student_course ON edu_grade_record (student_id, course_id, grade_status, deleted, updated_at);
CREATE INDEX idx_grade_course_source ON edu_grade_record (course_id, source_type, source_id, deleted, id);

CREATE TABLE edu_forum_topic (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    moderation_reason VARCHAR(300) NULL,
    moderated_by BIGINT NULL,
    moderated_at DATETIME(3) NULL,
    pinned TINYINT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    last_replied_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_forum_topic_course ON edu_forum_topic (course_id, status, pinned, deleted, last_replied_at, id);
CREATE INDEX idx_forum_topic_author ON edu_forum_topic (author_id, deleted, created_at, id);

CREATE TABLE edu_forum_reply (
    id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_reply_id BIGINT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    moderation_reason VARCHAR(300) NULL,
    moderated_by BIGINT NULL,
    moderated_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_forum_reply_topic ON edu_forum_reply (topic_id, status, deleted, created_at, id);
CREATE INDEX idx_forum_reply_author ON edu_forum_reply (author_id, deleted, created_at, id);

CREATE TABLE edu_announcement (
    id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    course_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    audience VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    published_at DATETIME(3) NOT NULL,
    withdrawn_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_announcement_scope ON edu_announcement (scope_type, course_id, status, deleted, published_at, id);
CREATE INDEX idx_announcement_audience ON edu_announcement (audience, status, deleted, published_at, id);

CREATE TABLE edu_notification (
    id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    announcement_id BIGINT NULL,
    course_id BIGINT NULL,
    assignment_id BIGINT NULL,
    exam_id BIGINT NULL,
    warning_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_notification_recipient ON edu_notification (recipient_user_id, status, category, deleted, created_at, id);
CREATE INDEX idx_notification_announcement ON edu_notification (announcement_id, status, deleted, id);

CREATE TABLE edu_notification_read (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    read_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_read_user UNIQUE (user_id, notification_id)
);

CREATE INDEX idx_notification_read_user ON edu_notification_read (user_id, deleted, notification_id);

CREATE TABLE edu_notification_preference (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_preference_user UNIQUE (user_id, category)
);

CREATE INDEX idx_notification_preference_user ON edu_notification_preference (user_id, deleted, category);

CREATE TABLE edu_learning_warning (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    warning_type VARCHAR(32) NOT NULL,
    warning_level VARCHAR(32) NOT NULL,
    warning_status VARCHAR(32) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    suggestion VARCHAR(1000) NULL,
    ai_explanation_draft_id BIGINT NULL,
    generated_at DATETIME(3) NOT NULL,
    handled_by BIGINT NULL,
    handle_remark VARCHAR(4000) NULL,
    handled_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_warning_student_course ON edu_learning_warning (student_id, course_id, warning_status, deleted, generated_at);
CREATE INDEX idx_warning_course_level ON edu_learning_warning (course_id, warning_level, warning_status, deleted, generated_at);

CREATE TABLE edu_warning_evidence (
    id BIGINT NOT NULL,
    warning_id BIGINT NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    source_id BIGINT NULL,
    metric_code VARCHAR(64) NULL,
    metric_value VARCHAR(128) NULL,
    description VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_warning_evidence_warning ON edu_warning_evidence (warning_id, deleted, id);

CREATE TABLE edu_exam (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    start_at DATETIME(3) NULL,
    end_at DATETIME(3) NULL,
    duration_minutes INT NULL,
    total_score DECIMAL(7,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_exam_course_status ON edu_exam (course_id, status, deleted, start_at, id);

CREATE TABLE edu_question_bank (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_question_bank_course ON edu_question_bank (course_id, status, deleted, id);

CREATE TABLE edu_question (
    id BIGINT NOT NULL,
    bank_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    analysis TEXT NULL,
    difficulty VARCHAR(32) NULL,
    score DECIMAL(7,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_question_bank_type ON edu_question (bank_id, question_type, status, deleted, id);
CREATE INDEX idx_question_course ON edu_question (course_id, status, deleted, id);

CREATE TABLE edu_question_option (
    id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    option_label VARCHAR(16) NOT NULL,
    option_content TEXT NOT NULL,
    is_correct TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_question_option_label UNIQUE (question_id, option_label)
);

CREATE INDEX idx_question_option_order ON edu_question_option (question_id, deleted, sort_order, id);

CREATE TABLE edu_exam_paper (
    id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    total_score DECIMAL(7,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    ai_generation_record_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_exam_paper_exam ON edu_exam_paper (exam_id, status, deleted, id);
CREATE INDEX idx_exam_paper_course ON edu_exam_paper (course_id, status, deleted, id);

CREATE TABLE edu_exam_paper_question (
    id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    score DECIMAL(7,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_paper_question UNIQUE (paper_id, question_id),
    CONSTRAINT uk_paper_question_order UNIQUE (paper_id, question_order)
);

CREATE INDEX idx_paper_question_order ON edu_exam_paper_question (paper_id, deleted, question_order, id);

CREATE TABLE edu_exam_attempt (
    id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME(3) NULL,
    submitted_at DATETIME(3) NULL,
    score DECIMAL(7,2) NULL,
    graded_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_exam_attempt_student UNIQUE (exam_id, student_id)
);

CREATE INDEX idx_exam_attempt_student ON edu_exam_attempt (student_id, status, deleted, started_at, id);
CREATE INDEX idx_exam_attempt_exam ON edu_exam_attempt (exam_id, status, deleted, submitted_at, id);

CREATE TABLE edu_exam_answer (
    id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_content TEXT NULL,
    score DECIMAL(7,2) NULL,
    teacher_comment VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_exam_answer_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_exam_answer_attempt ON edu_exam_answer (attempt_id, deleted, id);

CREATE TABLE edu_ai_generation_record (
    id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    requester_id BIGINT NOT NULL,
    provider VARCHAR(64) NULL,
    model_name VARCHAR(128) NULL,
    prompt_version VARCHAR(64) NULL,
    request_hash VARCHAR(128) NULL,
    output_summary TEXT NULL,
    status VARCHAR(32) NOT NULL,
    accepted TINYINT NOT NULL DEFAULT 0,
    accepted_by BIGINT NULL,
    accepted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_ai_generation_business ON edu_ai_generation_record (business_type, business_id, status, deleted, id);
CREATE INDEX idx_ai_generation_requester ON edu_ai_generation_record (requester_id, status, deleted, created_at);

CREATE TABLE edu_ai_action (
    id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    capability_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    confirmation_policy VARCHAR(32) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id BIGINT NULL,
    target_version INT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    parameters_json TEXT NOT NULL,
    preview_json TEXT NOT NULL,
    result_json TEXT NULL,
    resource_type VARCHAR(64) NULL,
    resource_id BIGINT NULL,
    resource_href VARCHAR(500) NULL,
    trace_id VARCHAR(128) NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(500) NULL,
    expires_at DATETIME(3) NOT NULL,
    confirmed_by BIGINT NULL,
    confirmed_at DATETIME(3) NULL,
    executed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_action_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_ai_action_requester ON edu_ai_action (requester_id, role_code, status, deleted, created_at, id);
CREATE INDEX idx_ai_action_capability ON edu_ai_action (capability_id, status, deleted, created_at, id);
CREATE INDEX idx_ai_action_target ON edu_ai_action (target_type, target_id, status, deleted, id);


-- ============================================================================
-- Local demonstration accounts and role assignments
-- ============================================================================

-- 仅 local/test profile 加载。密码为 BCrypt 哈希，明文只记录在开发文档中。
INSERT INTO sys_user
    (id, username, password_hash, display_name, user_status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (1001, 'student', '$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u', '测试学生', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (1002, 'teacher', '$2b$10$4/jxzR1iDdnQVYlELBd2zuN3wCdDNlcAfjX4bX.4e08ggPOiLcieS', '测试教师', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (1003, 'admin', '$2b$10$qklC5Vnw0Ov6Q3AVg1onj.DUeTJwQ0Zxh.o0fD0qkexIAF6y05yRG', '测试超级管理员', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (1014, 'admin_ops', '$2b$10$qklC5Vnw0Ov6Q3AVg1onj.DUeTJwQ0Zxh.o0fD0qkexIAF6y05yRG', '测试普通管理员', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    user_status = VALUES(user_status),
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role
    (id, user_id, role_id, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (4001, 1001, 2001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (4002, 1002, 2002, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (4003, 1003, 2003, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (4005, 1003, 2004, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (4014, 1014, 2003, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE deleted = 0, updated_at = CURRENT_TIMESTAMP;


-- ============================================================================
-- Local course and learning demonstration data
-- ============================================================================

-- 仅 local/test profile 加载。ID 使用固定值，便于 Postman 与自动化测试复现。
INSERT INTO sys_user
    (id, username, password_hash, display_name, user_status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (1004, 'teacher2', '$2b$10$4/jxzR1iDdnQVYlELBd2zuN3wCdDNlcAfjX4bX.4e08ggPOiLcieS', '测试教师二', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
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
    (id, course_id, teacher_id, role, status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (21101, 21001, 1002, 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (21102, 21002, 1004, 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21103, 21003, 1004, 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, 1004, CURRENT_TIMESTAMP, 1004, 0, 0),
    (21104, 21004, 1002, 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE role = VALUES(role), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

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


-- ============================================================================
-- Local collaboration, assessment, and platform demonstration data
-- ============================================================================

INSERT INTO edu_ai_generation_record
    (id, business_type, business_id, requester_id, provider, model_name, prompt_version, request_hash,
     output_summary, status, accepted, accepted_by, accepted_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (39001, 'GRADING_COMMENT_DRAFT', 32001, 1002, 'fake', 'fake-grading-v1', 'grading-comment-v1', 'demo-hash-39001',
     '学习态度认真，建议补充关键概念的证明过程。', 'SUCCEEDED', 1, 1002, CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (39002, 'RISK_EXPLANATION', 36001, 1002, 'fake', 'fake-risk-v1', 'risk-explanation-v1', 'demo-hash-39002',
     '学生近期学习进度低于课程节奏，建议安排一次跟进。', 'SUCCEEDED', 0, NULL, NULL,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE status = VALUES(status), output_summary = VALUES(output_summary), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_assignment
    (id, course_id, lesson_id, title, description, max_score, status, open_at, due_at, published_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (31001, 21001, 23001, '第一章课后练习', '完成基础概念说明与案例分析。', 100.00, 'PUBLISHED',
     '2026-09-01 08:00:00', '2026-09-15 23:59:59', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_assignment_attachment
    (id, assignment_id, name, file_key, file_url, file_size, mime_type, sort_order,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (31501, 31001, '作业说明.pdf', 'mock/assignment-31001/guide.pdf', NULL, 2048, 'application/pdf', 10,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_assignment_submission
    (id, assignment_id, course_id, student_id, attempt_no, content, file_key, file_url, status,
     submitted_at, score, teacher_comment, ai_comment_draft_id, graded_by, graded_at, published_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (32001, 31001, 21001, 1001, 1, '学生提交的第一章练习内容。', 'mock/submission-32001/report.docx', NULL, 'GRADED',
     CURRENT_TIMESTAMP, 88.50, '结构清楚，注意补充推导细节。', 39001, 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE status = VALUES(status), score = VALUES(score), teacher_comment = VALUES(teacher_comment), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_grade_record
    (id, course_id, student_id, source_type, source_id, score, max_score, weight, grade_status, comment, published_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (33001, 21001, 1001, 'ASSIGNMENT', 31001, 88.50, 100.00, 20.00, 'PUBLISHED', '第一章课后练习成绩', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE score = VALUES(score), grade_status = VALUES(grade_status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_forum_topic
    (id, course_id, author_id, title, content, status, pinned, reply_count, last_replied_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (34001, 21001, 1001, '第一章概念讨论', '我对第一章中的案例还有疑问。', 'VISIBLE', 0, 1, CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), reply_count = VALUES(reply_count), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_forum_reply
    (id, topic_id, course_id, author_id, parent_reply_id, content, status,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (35001, 34001, 21001, 1002, NULL, '可以先回看公开课时中的案例，再补充你的理解。', 'VISIBLE',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE content = VALUES(content), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_announcement
    (id, scope_type, course_id, title, content, audience, status, published_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (35001, 'COURSE', 21001, '第一章学习提醒', '请在本周内完成第一章学习与课后作业。', 'STUDENT', 'PUBLISHED', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (35002, 'SYSTEM', NULL, '系统联调公告', '当前环境用于项目联调和演示。', 'ALL', 'PUBLISHED', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_notification
    (id, recipient_user_id, title, content, category, status, source_type, idempotency_key,
     announcement_id, course_id, assignment_id, exam_id, warning_id,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (50001, 1001, '第一章学习提醒', '请在本周内完成第一章学习与课后作业。', 'COURSE', 'PUBLISHED', 'ANNOUNCEMENT',
     'announcement:35001:user:1001', 35001, 21001, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (50002, 1001, '系统联调公告', '当前环境用于项目联调和演示。', 'SYSTEM', 'PUBLISHED', 'ANNOUNCEMENT',
     'announcement:35002:user:1001', 35002, NULL, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (50003, 1002, '系统联调公告', '当前环境用于项目联调和演示。', 'SYSTEM', 'PUBLISHED', 'ANNOUNCEMENT',
     'announcement:35002:user:1002', 35002, NULL, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (50004, 1003, '系统联调公告', '当前环境用于项目联调和演示。', 'SYSTEM', 'PUBLISHED', 'ANNOUNCEMENT',
     'announcement:35002:user:1003', 35002, NULL, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (50005, 1004, '系统联调公告', '当前环境用于项目联调和演示。', 'SYSTEM', 'PUBLISHED', 'ANNOUNCEMENT',
     'announcement:35002:user:1004', 35002, NULL, NULL, NULL, NULL,
     CURRENT_TIMESTAMP, 1003, CURRENT_TIMESTAMP, 1003, 0, 0),
    (50006, 1001, '作业已发布：第一章课后练习', '请按时完成作业，截止时间：2026-09-15T23:59:59Z',
     'ASSIGNMENT', 'PUBLISHED', 'ASSIGNMENT_PUBLISHED', 'assignment:31001:published:user:1001',
     NULL, 21001, 31001, NULL, NULL, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (50007, 1002, '收到作业提交：第一章课后练习', '有新的学生作业等待批改。',
     'ASSIGNMENT', 'PUBLISHED', 'ASSIGNMENT_SUBMITTED', 'submission:32001:submitted:user:1002',
     NULL, 21001, 31001, NULL, NULL, CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0, 0),
    (50008, 1001, '学习预警：学习进度低于课程节奏', '建议完成第一章补学并提交学习反馈。',
     'WARNING', 'PUBLISHED', 'WARNING_CREATED', 'warning:36001:created:user:1001',
     NULL, 21001, NULL, NULL, 36001, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (50009, 1002, '学生学习预警：学习进度低于课程节奏', '请进入预警中心查看并跟进。',
     'WARNING', 'PUBLISHED', 'WARNING_CREATED', 'warning:36001:created:user:1002',
     NULL, 21001, NULL, NULL, 36001, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (50010, 1001, '作业批改已完成：第一章课后练习', '成绩已发布，请进入成绩页面查看。',
     'ASSIGNMENT', 'PUBLISHED', 'GRADE_PUBLISHED', 'grade:33001:published:user:1001',
     NULL, 21001, 31001, NULL, NULL, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (50011, 1001, '考试安排已发布：第一章随堂测验', '考试时间：2026-09-16T09:00Z 至 2026-09-16T10:00Z',
     'EXAM', 'PUBLISHED', 'EXAM_PUBLISHED', 'exam:38001:published:user:1001',
     NULL, 21001, NULL, 38001, NULL, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    category = VALUES(category),
    status = VALUES(status),
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_learning_warning
    (id, course_id, student_id, warning_type, warning_level, warning_status, summary, suggestion,
     ai_explanation_draft_id, generated_at, handled_by, handled_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (36001, 21001, 1001, 'PROGRESS_LAG', 'MEDIUM', 'OPEN', '学习进度低于课程节奏', '建议完成第一章补学并提交学习反馈。',
     39002, CURRENT_TIMESTAMP, NULL, NULL,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE warning_status = VALUES(warning_status), summary = VALUES(summary), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_warning_evidence
    (id, warning_id, evidence_type, source_id, metric_code, metric_value, description,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (36101, 36001, 'LESSON_PROGRESS', 23001, 'completedLessonRate', '0.25', '课程学习完成率低于 50%。',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE metric_value = VALUES(metric_value), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_question_bank
    (id, course_id, name, description, status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (37001, 21001, '第一章基础题库', '用于第一章测验和智能组卷演示。', 'ACTIVE',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_question
    (id, bank_id, course_id, question_type, stem, analysis, difficulty, score, status,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (37101, 37001, 21001, 'SINGLE_CHOICE', '下列哪一项最符合课程中的核心概念？', '选择 B，因为它体现了案例中的主要约束。', 'EASY', 5.00, 'ACTIVE',
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE stem = VALUES(stem), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_question_option
    (id, question_id, option_label, option_content, is_correct, sort_order,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (37201, 37101, 'A', '仅关注实现速度', 0, 10, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (37202, 37101, 'B', '先明确边界和约束', 1, 20, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0),
    (37203, 37101, 'C', '直接复制历史代码', 0, 30, CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE option_content = VALUES(option_content), is_correct = VALUES(is_correct), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_exam
    (id, course_id, title, description, status, start_at, end_at, duration_minutes, total_score,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (38001, 21001, '第一章随堂测验', '用于考试安排与答题演示。', 'PUBLISHED',
     '2026-09-16 09:00:00', '2026-09-16 10:00:00', 60, 100.00,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_exam_paper
    (id, exam_id, course_id, title, total_score, status, ai_generation_record_id,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (38101, 38001, 21001, '第一章随堂测验 A 卷', 100.00, 'PUBLISHED', NULL,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_exam_paper_question
    (id, paper_id, question_id, question_order, score,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (38201, 38101, 37101, 1, 5.00,
     CURRENT_TIMESTAMP, 1002, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE score = VALUES(score), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_exam_attempt
    (id, exam_id, paper_id, student_id, status, started_at, submitted_at, score, graded_at,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (38301, 38001, 38101, 1001, 'SUBMITTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 5.00, CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE status = VALUES(status), score = VALUES(score), deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO edu_exam_answer
    (id, attempt_id, question_id, answer_content, score, teacher_comment,
     created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (38401, 38301, 37101, 'B', 5.00, '回答正确。',
     CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1002, 0, 0)
ON DUPLICATE KEY UPDATE answer_content = VALUES(answer_content), score = VALUES(score), deleted = 0, updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- Acceptance showcase expansion: this is part of the canonical bootstrap.
-- ============================================================================
INSERT INTO sys_user (id,username,password_hash,display_name,user_status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(1101,'student_d','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','王晨','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1102,'student_e','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','李欣怡','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1103,'student_f','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','赵明远','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1104,'student_g','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','孙雨桐','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1105,'student_h','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','陈思涵','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1020,'teacher_cs','$2b$10$4/jxzR1iDdnQVYlELBd2zuN3wCdDNlcAfjX4bX.4e08ggPOiLcieS','刘老师','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(1021,'teacher_math','$2b$10$4/jxzR1iDdnQVYlELBd2zuN3wCdDNlcAfjX4bX.4e08ggPOiLcieS','张老师','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO sys_user_role (id,user_id,role_id,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(4111,1101,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4112,1102,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4113,1103,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4114,1104,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4115,1105,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4120,1020,2002,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4121,1021,2002,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0)
ON DUPLICATE KEY UPDATE deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_course (id,course_code,name,summary,cover_url,category_id,term,department,credit,owner_teacher_id,status,review_status,enrollment_open_at,enrollment_close_at,start_at,end_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22001,'DEMO-JAVA-001','Java 程序设计','面向对象、集合与异常处理的完整课程演示',NULL,1,'2026-FALL','计算机学院',3,1020,'PUBLISHED','APPROVED','2026-07-01','2026-10-31','2026-09-01','2027-01-20',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22002,'DEMO-DATA-001','数据结构与算法','线性表、树、图和经典算法验收课程',NULL,1,'2026-FALL','计算机学院',4,1020,'PUBLISHED','APPROVED','2026-07-01','2026-10-31','2026-09-01','2027-01-20',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22003,'DEMO-MATH-001','高等数学基础','极限、导数与积分的混合教学课程',NULL,1,'2026-FALL','理学院',4,1021,'PUBLISHED','APPROVED','2026-07-01','2026-10-31','2026-09-01','2027-01-20',CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0)
ON DUPLICATE KEY UPDATE name=VALUES(name),status='PUBLISHED',review_status='APPROVED',deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_course_teacher (id,course_id,teacher_id,role,status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES (22101,22001,1020,'OWNER','ACTIVE',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22102,22002,1020,'OWNER','ACTIVE',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22103,22003,1021,'OWNER','ACTIVE',CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0) ON DUPLICATE KEY UPDATE status='ACTIVE',deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_course_enrollment (id,course_id,student_id,status,enrolled_at,withdrawn_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES (22201,22001,1101,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1101,CURRENT_TIMESTAMP,1101,0,0),(22202,22001,1102,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1102,CURRENT_TIMESTAMP,1102,0,0),(22203,22001,1103,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1103,CURRENT_TIMESTAMP,1103,0,0),(22204,22002,1101,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1101,CURRENT_TIMESTAMP,1101,0,0),(22205,22002,1104,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1104,CURRENT_TIMESTAMP,1104,0,0),(22206,22002,1105,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1105,CURRENT_TIMESTAMP,1105,0,0),(22207,22003,1102,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1102,CURRENT_TIMESTAMP,1102,0,0),(22208,22003,1103,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1103,CURRENT_TIMESTAMP,1103,0,0) ON DUPLICATE KEY UPDATE status='ENROLLED',withdrawn_at=NULL,deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_chapter (id,course_id,title,description,sort_order,status,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22301,22001,'第一章 Java 基础','变量、流程控制与方法',10,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22302,22001,'第二章 面向对象','封装、继承、多态',20,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22303,22002,'第一章 线性结构','数组、链表、栈与队列',10,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22304,22002,'第二章 树和图','二叉树、图遍历与最短路径',20,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22305,22003,'第一章 极限与连续','函数极限的基本计算',10,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0),(22306,22003,'第二章 微分学','导数与应用',20,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),description=VALUES(description),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_lesson (id,course_id,chapter_id,title,content_type,content,video_url,estimated_minutes,sort_order,status,unlock_type,unlock_at,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22401,22001,22301,'Java 环境与第一个程序','RICH_TEXT','安装 JDK，编写并运行第一个 Java 程序。',NULL,25,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22402,22001,22301,'分支与循环','RICH_TEXT','使用条件判断和循环解决基础问题。',NULL,30,20,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22403,22001,22302,'类与对象','RICH_TEXT','从建模到对象协作。',NULL,35,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22404,22002,22303,'顺序表与链表','RICH_TEXT','比较两种线性表的访问和插入性能。',NULL,30,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22405,22002,22303,'栈和队列','RICH_TEXT','理解先进后出与先进先出。',NULL,25,20,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22406,22002,22304,'二叉树遍历','RICH_TEXT','掌握前序、中序、后序遍历。',NULL,40,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22407,22003,22305,'极限的概念','RICH_TEXT','认识函数极限和无穷小。',NULL,30,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0),(22408,22003,22305,'极限计算','RICH_TEXT','使用四则运算和夹逼准则。',NULL,35,20,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0),(22409,22003,22306,'导数及其应用','RICH_TEXT','求导并分析函数单调性。',NULL,35,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO sys_file (id,owner_user_id,original_name,object_key,storage_provider,file_size,mime_type,sha256,purpose,file_status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22501,1020,'Java课程导学.md','demo/course/java-guide.md','LOCAL',566,'text/markdown; charset=UTF-8','56f97495b48738184b27f450e4534ed920f2b0f4129bf9a631bf3c85e989cd55','COURSE_MATERIAL','ACTIVE',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22502,1020,'数据结构思维导图.md','demo/course/data-structure-map.md','LOCAL',974,'text/markdown; charset=UTF-8','ea0699e742cea2edb0c8ccabffe662e3878f956727309940ba68c4871e78e307','COURSE_MATERIAL','ACTIVE',CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22503,1021,'高数公式表.md','demo/course/calculus-formula.md','LOCAL',527,'text/markdown; charset=UTF-8','72ffe763f273ea2ffbec31e4bf263bde5911c9bdd0f00b3c3292c155fd8b4945','COURSE_MATERIAL','ACTIVE',CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0)
ON DUPLICATE KEY UPDATE original_name=VALUES(original_name),object_key=VALUES(object_key),file_size=VALUES(file_size),mime_type=VALUES(mime_type),sha256=VALUES(sha256),file_status='ACTIVE',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_material (id,course_id,chapter_id,lesson_id,name,material_type,file_id,file_key,file_url,file_size,mime_type,visibility,status,sort_order,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22601,22001,22301,22401,'Java课程导学','DOCUMENT',22501,NULL,NULL,566,'text/markdown; charset=UTF-8','LESSON','PUBLISHED',10,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22602,22002,22303,22404,'数据结构思维导图','DOCUMENT',22502,NULL,NULL,974,'text/markdown; charset=UTF-8','LESSON','PUBLISHED',10,CURRENT_TIMESTAMP,1020,CURRENT_TIMESTAMP,1020,0,0),(22603,22003,22305,22407,'高数公式表','DOCUMENT',22503,NULL,NULL,527,'text/markdown; charset=UTF-8','CHAPTER','PUBLISHED',10,CURRENT_TIMESTAMP,1021,CURRENT_TIMESTAMP,1021,0,0)
ON DUPLICATE KEY UPDATE name=VALUES(name),file_id=VALUES(file_id),file_size=VALUES(file_size),mime_type=VALUES(mime_type),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;
