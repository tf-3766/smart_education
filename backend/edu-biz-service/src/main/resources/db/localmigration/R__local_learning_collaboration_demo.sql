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
