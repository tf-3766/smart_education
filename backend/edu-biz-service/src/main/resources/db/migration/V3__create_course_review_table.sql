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
