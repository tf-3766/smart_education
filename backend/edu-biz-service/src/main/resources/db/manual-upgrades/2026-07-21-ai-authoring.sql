-- AI 自动流写操作：为可被 AI 生成的内容表增加来源标记 source（AI / HUMAN）。
-- source=AI 表示由 AI 自动流生成、尚待教师确认的草稿；教师确认后置为 HUMAN。
-- 脚本可重复执行（列已存在则跳过），不删除任何既有数据。

-- edu_question_bank.source
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_question_bank' AND column_name = 'source');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE edu_question_bank ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''HUMAN'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- edu_assignment.source
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_assignment' AND column_name = 'source');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE edu_assignment ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''HUMAN'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- edu_exam.source
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_exam' AND column_name = 'source');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE edu_exam ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''HUMAN'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- edu_announcement.source
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'edu_announcement' AND column_name = 'source');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE edu_announcement ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT ''HUMAN'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
