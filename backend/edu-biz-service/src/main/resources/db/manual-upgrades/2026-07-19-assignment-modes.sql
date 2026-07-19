-- Multi-mode assignments and structured student answers.
-- Safe to run repeatedly on MySQL 8.x; existing data is preserved.

SET @assignment_mode_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_assignment' AND COLUMN_NAME = 'response_mode'
    ),
    'SELECT 1',
    'ALTER TABLE edu_assignment ADD COLUMN response_mode VARCHAR(32) NOT NULL DEFAULT ''MIXED'' AFTER description'
);
PREPARE assignment_mode_stmt FROM @assignment_mode_sql;
EXECUTE assignment_mode_stmt;
DEALLOCATE PREPARE assignment_mode_stmt;

SET @assignment_questions_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_assignment' AND COLUMN_NAME = 'questions_json'
    ),
    'SELECT 1',
    'ALTER TABLE edu_assignment ADD COLUMN questions_json TEXT NULL AFTER response_mode'
);
PREPARE assignment_questions_stmt FROM @assignment_questions_sql;
EXECUTE assignment_questions_stmt;
DEALLOCATE PREPARE assignment_questions_stmt;

SET @submission_answers_sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_assignment_submission' AND COLUMN_NAME = 'answers_json'
    ),
    'SELECT 1',
    'ALTER TABLE edu_assignment_submission ADD COLUMN answers_json TEXT NULL AFTER content'
);
PREPARE submission_answers_stmt FROM @submission_answers_sql;
EXECUTE submission_answers_stmt;
DEALLOCATE PREPARE submission_answers_stmt;