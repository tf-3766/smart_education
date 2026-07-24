SET NAMES utf8mb4;

-- course_code identifies a reusable course definition; edu_course rows are teaching offerings.
SET @drop_course_code_unique = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND index_name = 'uk_course_code'),
    'ALTER TABLE edu_course DROP INDEX uk_course_code',
    'SELECT 1');
PREPARE course_definition_stmt FROM @drop_course_code_unique;
EXECUTE course_definition_stmt;
DEALLOCATE PREPARE course_definition_stmt;

SET @create_course_code_index = IF(
    NOT EXISTS(SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'edu_course' AND index_name = 'idx_course_code'),
    'CREATE INDEX idx_course_code ON edu_course (course_code, deleted, id)',
    'SELECT 1');
PREPARE course_definition_stmt FROM @create_course_code_index;
EXECUTE course_definition_stmt;
DEALLOCATE PREPARE course_definition_stmt;

-- The application copies these packaged Markdown resources to FILE_STORAGE_ROOT at startup.
UPDATE sys_file SET
    original_name='Java课程导学.md', object_key='demo/course/java-guide.md', storage_provider='LOCAL',
    file_size=566, mime_type='text/markdown; charset=UTF-8',
    sha256='56f97495b48738184b27f450e4534ed920f2b0f4129bf9a631bf3c85e989cd55',
    purpose='COURSE_MATERIAL', file_status='ACTIVE', deleted=0, updated_at=CURRENT_TIMESTAMP
WHERE id=22501;
UPDATE sys_file SET
    original_name='数据结构思维导图.md', object_key='demo/course/data-structure-map.md', storage_provider='LOCAL',
    file_size=974, mime_type='text/markdown; charset=UTF-8',
    sha256='ea0699e742cea2edb0c8ccabffe662e3878f956727309940ba68c4871e78e307',
    purpose='COURSE_MATERIAL', file_status='ACTIVE', deleted=0, updated_at=CURRENT_TIMESTAMP
WHERE id=22502;
UPDATE sys_file SET
    original_name='高数公式表.md', object_key='demo/course/calculus-formula.md', storage_provider='LOCAL',
    file_size=527, mime_type='text/markdown; charset=UTF-8',
    sha256='72ffe763f273ea2ffbec31e4bf263bde5911c9bdd0f00b3c3292c155fd8b4945',
    purpose='COURSE_MATERIAL', file_status='ACTIVE', deleted=0, updated_at=CURRENT_TIMESTAMP
WHERE id=22503;

UPDATE edu_course_material SET file_size=566, mime_type='text/markdown; charset=UTF-8', updated_at=CURRENT_TIMESTAMP WHERE file_id=22501;
UPDATE edu_course_material SET file_size=974, mime_type='text/markdown; charset=UTF-8', updated_at=CURRENT_TIMESTAMP WHERE file_id=22502;
UPDATE edu_course_material SET file_size=527, mime_type='text/markdown; charset=UTF-8', updated_at=CURRENT_TIMESTAMP WHERE file_id=22503;
