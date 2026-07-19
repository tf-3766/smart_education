-- Manual, idempotent upgrade for existing local MySQL databases created before 2026-07-18.
-- Back up non-demo data first. Fresh databases should use online_education_bootstrap.sql instead.

SET @status_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'edu_course_teacher'
      AND COLUMN_NAME = 'status'
);
SET @ddl = IF(
    @status_column_exists = 0,
    'ALTER TABLE edu_course_teacher ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''ACTIVE'' AFTER role',
    'SELECT ''edu_course_teacher.status already exists'''
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE edu_course_teacher
SET status = 'ACTIVE'
WHERE status IS NULL OR status = '';

SET @teacher_index_columns = (
    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'edu_course_teacher'
      AND INDEX_NAME = 'idx_course_teacher_teacher'
);
SET @ddl = CASE
    WHEN @teacher_index_columns IS NULL THEN
        'ALTER TABLE edu_course_teacher ADD INDEX idx_course_teacher_teacher (teacher_id, deleted, status, role, course_id)'
    WHEN @teacher_index_columns <> 'teacher_id,deleted,status,role,course_id' THEN
        'ALTER TABLE edu_course_teacher DROP INDEX idx_course_teacher_teacher, ADD INDEX idx_course_teacher_teacher (teacher_id, deleted, status, role, course_id)'
    ELSE 'SELECT ''idx_course_teacher_teacher already current'''
END;
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @course_index_columns = (
    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'edu_course_teacher'
      AND INDEX_NAME = 'idx_course_teacher_course_role'
);
SET @ddl = CASE
    WHEN @course_index_columns IS NULL THEN
        'ALTER TABLE edu_course_teacher ADD INDEX idx_course_teacher_course_role (course_id, role, status, deleted, teacher_id)'
    WHEN @course_index_columns <> 'course_id,role,status,deleted,teacher_id' THEN
        'ALTER TABLE edu_course_teacher DROP INDEX idx_course_teacher_course_role, ADD INDEX idx_course_teacher_course_role (course_id, role, status, deleted, teacher_id)'
    ELSE 'SELECT ''idx_course_teacher_course_role already current'''
END;
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS edu_term_enrollment_window (
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
    CONSTRAINT uk_term_enrollment_window UNIQUE (term),
    INDEX idx_term_enrollment_window_term (deleted, term)
);

INSERT INTO edu_term_enrollment_window
    (id, term, enrollment_open_at, enrollment_close_at, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (20001, '2026 秋季', '2026-08-20 01:00:00', '2026-09-05 09:00:00', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE term = VALUES(term);
