-- 仅 local/test profile 加载。密码为 BCrypt 哈希，明文只记录在开发文档中。
INSERT INTO sys_user
    (id, username, password_hash, display_name, user_status, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (1001, 'student', '$2b$10$pM7Fds4bn4SUFowh9hH.uuev0Cjyk8nbXkb1uNEEBzC04RXhWg3Z.', '测试学生', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (1002, 'teacher', '$2b$10$.64FnHJo8pzAoAV1QYz/WeuHQOG47AjComkIXA6OFuc6/Gk8zQRgO', '测试教师', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (1003, 'admin', '$2b$10$LVDnPxq8oKMtaI3L17F4yuItOcHH7oLg6Zy8du1uqiA/sBuW25uB2', '测试管理员', 'ENABLED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
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
    (4003, 1003, 2003, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0)
ON DUPLICATE KEY UPDATE deleted = 0, updated_at = CURRENT_TIMESTAMP;
