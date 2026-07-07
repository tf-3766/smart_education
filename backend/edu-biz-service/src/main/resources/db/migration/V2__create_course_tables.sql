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
    PRIMARY KEY (id),
    CONSTRAINT uk_course_code UNIQUE (course_code)
);

CREATE INDEX idx_course_owner_status ON edu_course (owner_teacher_id, deleted, status, updated_at);
CREATE INDEX idx_course_review_queue ON edu_course (review_status, deleted, updated_at, id);
CREATE INDEX idx_course_catalog ON edu_course (status, review_status, enrollment_open_at, enrollment_close_at, deleted);
CREATE INDEX idx_course_term_category ON edu_course (term, category_id, deleted, id);

CREATE TABLE edu_course_teacher (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_teacher UNIQUE (course_id, teacher_id)
);

CREATE INDEX idx_course_teacher_teacher ON edu_course_teacher (teacher_id, deleted, role, course_id);
CREATE INDEX idx_course_teacher_course_role ON edu_course_teacher (course_id, role, deleted, teacher_id);

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
