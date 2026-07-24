-- AI 学习干预计划通常超过旧版 500 字限制；接口与存储统一扩展到 4000 字。
ALTER TABLE edu_learning_warning
    MODIFY COLUMN handle_remark VARCHAR(4000) NULL;
