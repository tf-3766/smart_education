CREATE TABLE edu_assignment (
    id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    max_score DECIMAL(7,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
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

CREATE TABLE edu_assignment_submission (
    id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    content TEXT NULL,
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
