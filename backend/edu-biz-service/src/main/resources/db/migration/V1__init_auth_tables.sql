CREATE TABLE sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    user_status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE INDEX idx_user_status_deleted ON sys_user (user_status, deleted, id);

CREATE TABLE sys_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (role_code)
);

CREATE TABLE sys_permission (
    id BIGINT NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_role_user ON sys_user_role (user_id, deleted, role_id);
CREATE INDEX idx_user_role_role ON sys_user_role (role_id, deleted, user_id);

CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permission_role ON sys_role_permission (role_id, deleted, permission_id);
CREATE INDEX idx_role_permission_permission ON sys_role_permission (permission_id, deleted, role_id);

INSERT INTO sys_role
    (id, role_code, role_name, enabled, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (2001, 'STUDENT', '学生', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (2002, 'TEACHER', '教师', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (2003, 'ADMIN', '管理员', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_permission
    (id, permission_code, permission_name, enabled, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (3001, 'auth:profile:read', '查看本人资料', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3002, 'student:access', '访问学生入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3003, 'teacher:access', '访问教师入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (3004, 'admin:access', '访问管理员入口', 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);

INSERT INTO sys_role_permission
    (id, role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted, version)
VALUES
    (5001, 2001, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5002, 2001, 3002, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5003, 2002, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5004, 2002, 3003, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5005, 2003, 3001, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0),
    (5006, 2003, 3004, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, 0, 0);
