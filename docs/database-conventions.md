# 数据库设计与初始化规范

> 适用范围：`edu-biz-service` 所有 MySQL 8.0 表。AI 向量集合由 `edu-ai-service` 独立管理，不得与业务 MySQL 共享账号或连接。

## 1. 统一决策

| 项目 | 决策 |
|---|---|
| 业务数据库 | MySQL 8.4，schema 名 `smart_education` |
| 字符集/排序规则 | `utf8mb4` + `utf8mb4_0900_ai_ci`，所有环境一致 |
| 存储引擎 | InnoDB |
| 主键 | MySQL `BIGINT`、Java `Long`、MyBatis-Plus `ASSIGN_ID` 雪花 ID |
| 对外 ID | JSON 字符串，避免 JavaScript 超过安全整数范围 |
| 数据库初始化 | 只允许 `online_education_bootstrap.sql`；禁止 `ddl-auto=update` |
| 时间 | `DATETIME(3)`，统一存 UTC |
| 逻辑删除 | 核心表统一 `deleted TINYINT NOT NULL DEFAULT 0` |
| 乐观锁 | 核心表统一 `version INT NOT NULL DEFAULT 0` |
| 外键 | 不创建数据库物理外键；关联完整性由应用事务、唯一约束、索引和一致性审计保证 |

### 1.1 为什么选雪花 `Long`

- 多人并行开发和未来多实例部署时，不依赖单库自增序列。
- MyBatis-Plus 原生支持，MVP 不需额外 ID 服务。
- 使用有符号 `BIGINT` 与 Java `Long` 完全对应，不使用 `BIGINT UNSIGNED` 制造类型差异。
- 雪花 ID 不能对外暴露业务规模含义；接口统一按字符串序列化。

注意：必须固定机器/节点 ID 来源，生产环境不可让多个实例使用相同 worker/datacenter 配置。该配置纳入部署检查。

## 2. 表与字段命名

### 2.1 表名

- 使用小写 `snake_case`、单数名词。
- 基础权限表使用 `sys_` 前缀：`sys_user`、`sys_role`。
- 教学业务表使用 `edu_` 前缀：`edu_course`、`edu_assignment_submission`。
- 消息/outbox 表使用明确前缀：`sys_outbox_event`。
- 关联表使用双方资源名：`sys_user_role`、`edu_course_teacher`。
- 禁止 `t_user`、`userInfo`、`course-table`、`data1` 等命名。

### 2.2 字段名

- 主键统一 `id`；外部引用统一 `<resource>_id`，如 `course_id`。
- 时间统一 `_at`：`published_at`、`submitted_at`；纯日期使用 `_date`。
- 数量使用 `_count`，比例使用 `_rate`，金额/分数使用明确单位或语义。
- 布尔字段使用形容词/状态词：`enabled`、`visible`、`deleted`，类型为 `TINYINT`。
- 状态统一 `<resource>_status` 或明确语义，如 `review_status`、`publication_status`。
- 禁止使用 MySQL 保留字和含混字段：`key`、`value`、`type1`、`flag`、`status1`。
- 禁止用逗号分隔字符串保存多个 ID；使用关联表或 JSON（仅适合真正的非查询结构化扩展数据）。

## 3. 核心表统一审计字段

每张核心表必须包含且顺序建议统一为：

```sql
id          BIGINT       NOT NULL COMMENT '雪花主键',
-- 业务字段
created_at  DATETIME(3)  NOT NULL COMMENT 'UTC 创建时间',
created_by  BIGINT       NOT NULL COMMENT '创建人；0 表示系统任务',
updated_at  DATETIME(3)  NOT NULL COMMENT 'UTC 更新时间',
updated_by  BIGINT       NOT NULL COMMENT '更新人；0 表示系统任务',
deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除，1=逻辑删除',
version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
PRIMARY KEY (id)
```

规则：

- `created_at/created_by` 创建后不得修改。
- `updated_at/updated_by/version` 每次业务更新时更新。
- 系统任务统一使用保留 actor ID `0`；业务用户雪花 ID 必须大于 0。审计人字段不建立指向 `sys_user` 的物理外键。
- `deleted=1` 不代表可以复用唯一业务编号；唯一约束策略必须在建表时说明。
- 纯关联表、历史表、outbox 表如因语义需要特殊字段，也必须在 migration 注释中说明偏差；核心业务表不得省略上述七个字段。

## 4. 字段类型

| 语义 | 类型 | 规则 |
|---|---|---|
| ID | `BIGINT` | 不用 `INT`、UUID 字符串混用 |
| 短 code | `VARCHAR(32/64)` | 角色、状态、业务编号，长度按实际上限 |
| 名称/标题 | `VARCHAR(128/255)` | 不无脑使用 `TEXT` |
| 长文本 | `TEXT/MEDIUMTEXT` | 课程正文、评语等；禁止放二进制 |
| 时间点 | `DATETIME(3)` | 存 UTC |
| 日期 | `DATE` | 不含时区的业务日期 |
| 布尔 | `TINYINT` | 只允许 0/1，应用层映射 boolean |
| 状态 | `VARCHAR(32)` | 存业务枚举 code，不用 ordinal |
| 分数 | `DECIMAL(7,2)` | 精度按课程规则调整，不用 float/double |
| 比例 | `DECIMAL(7,4)` | 统一约定 0～1 或 0～100，并在字段注释中写明 |
| JSON 扩展 | `JSON` | 必须有 schema/版本；高频查询字段必须拆列 |
| 文件 | 元数据 + object key | 数据库不存文件二进制和 base64 |

所有字段尽量 `NOT NULL`。只有“业务上确实未知/不适用”才允许 `NULL`；空字符串、0、`1970-01-01` 不能冒充未知值。

## 5. 状态字段与流转

### 5.1 通用规则

- 状态必须对应 Java 业务枚举，数据库存稳定 code。
- 状态只能通过应用 Service 的明确命令流转，禁止 Controller/Mapper 随意改字符串。
- 每个状态枚举必须写：code、中文含义、允许前置状态、是否终态、谁可操作。
- “逾期”“进行中”等可由时间和基础状态推导的展示状态，不应重复落库，除非需要历史快照。
- 审核状态、发布状态、生命周期状态含义不同，禁止塞入一个万能 `status`。

### 5.2 MVP 状态基线

#### 课程

课程生命周期 `course_status`：

| code | 含义 | 允许流转 |
|---|---|---|
| `DRAFT` | 教师编辑中 | → `OPEN_ENROLLMENT` 或先完成审核 |
| `OPEN_ENROLLMENT` | 可选课 | → `IN_PROGRESS`、`OFFLINE` |
| `IN_PROGRESS` | 教学进行中 | → `FINISHED`、`OFFLINE` |
| `FINISHED` | 正常结束，只读 | → `OFFLINE` |
| `OFFLINE` | 管理下线 | 按治理规则恢复到原有效状态，需审计 |

课程审核 `review_status` 单独保存：`NOT_SUBMITTED → PENDING → APPROVED/REJECTED`；`PENDING → WITHDRAWN`。驳回后修改可再次提交，生成新的审核记录，不覆盖旧意见。

#### 课时

`lesson_status`：`DRAFT → PUBLISHED → OFFLINE`；`OFFLINE → PUBLISHED` 需权限和审计。学生只能读取 `PUBLISHED` 且满足解锁规则的课时。

#### 作业与提交

作业 `assignment_status`：`DRAFT → PUBLISHED → CLOSED`；已发布作业修改截止时间、满分、提交类型必须记录影响范围。

提交 `submission_status`：

- `DRAFT → SUBMITTED`
- `SUBMITTED → GRADED` 或 `RETURNED`
- `RETURNED → SUBMITTED`（仅允许重交时）
- `GRADED` 是批改状态，不等于成绩已对学生发布；发布状态单独表示。

`OVERDUE` 通常由截止时间、有效提交和迟交规则计算，不作为唯一持久状态。

#### 成绩

`grade_status` 首版为 `DRAFT → PUBLISHED`。未发布成绩对学生接口不可见，不能返回 0；后续如增加撤回，必须先补接口、操作原因和历史版本设计。

#### 考试

`exam_status`：`DRAFT → PUBLISHED → ENDED → RESULTS_PUBLISHED`；任何允许状态可按规则转 `CANCELLED`。页面“待开始/进行中”由 `PUBLISHED + start_at/end_at` 推导。交卷是学生考试会话状态，不直接改考试状态。

考试会话 `exam_session_status`：`CREATED → IN_PROGRESS → SUBMITTED`；超时服务端转 `AUTO_SUBMITTED`，异常中止使用 `INTERRUPTED` 并留恢复依据。

#### 学习预警

`warning_status` 首版为 `OPEN → HANDLED/IGNORED`。AI 只能生成解释/建议，不能推进状态；教师处理或忽略预警时不删除证据。

#### AI 任务

AI 服务自有短期状态：`PENDING → RUNNING → SUCCEEDED/FAILED/CANCELLED`。该状态保存在 AI 自有 Redis 或任务存储中，不能代表业务内容已采用或发布。

## 6. 索引规范

命名：

- 主键：`PRIMARY KEY`。
- 唯一索引：`uk_<table_without_prefix>_<columns>`。
- 普通索引：`idx_<table_without_prefix>_<columns>`。

规则：

- 所有外部引用字段必须评估并通常建立索引。
- 高频列表按真实查询建立联合索引，等值条件在前，范围/排序字段在后。
- 联合索引遵守最左前缀；避免为联合索引的每个列再机械建单列索引。
- 低区分度的 `deleted`、boolean、status 不单独建索引；与 owner/时间等组合。
- 长文本不建普通 B-Tree 索引；搜索需求使用专用检索方案，不写 `%keyword%` 扫全表。
- 唯一约束优先由数据库保证，例如用户名、课程业务编号、同一学生对同一课程的有效选课。
- 所有索引必须能对应一个查询/约束场景；PR 中说明新增索引服务的 SQL。

示例：

```sql
CREATE INDEX idx_assignment_course_status_deadline
    ON edu_assignment (course_id, assignment_status, deadline_at, id);
```

## 7. 外键策略

- 全项目不创建数据库物理外键，也不使用 `ON DELETE CASCADE`。
- 关联字段必须建立适合查询方向的索引；多对多绑定必须建立唯一索引。
- 创建、绑定、删除和恢复操作由应用 Service 在事务中校验关联对象与资源权限。
- 定期一致性审计发现孤儿关系；修复必须走脚本评审或业务补偿，不能手工改共享库。
- 跨服务只传稳定 ID 和版本化契约，不允许跨库关联查询。

## 8. 删除策略

| 数据类别 | 默认策略 | 说明 |
|---|---|---|
| 用户、课程、章节、作业、考试、题目 | 逻辑删除/停用 | 有历史引用，不允许级联物理删除 |
| 提交、成绩、审核、预警、审计 | 不允许业务删除 | 需要纠正时追加版本/撤回/更正记录 |
| 关联关系 | 状态化或逻辑删除 | 如退选应保留时间和原因 |
| 草稿附件/临时上传 | 到期物理清理 | 由任务按明确 TTL 清理，写日志 |
| outbox/消费幂等记录 | 归档后物理清理 | 保留期由运维配置 |
| AI 向量 | 按资源版本删除/重建 | 由 AI 服务负责，不由 Biz SQL 删除 |

任何删除接口必须先说明：谁可删、删除后谁不可见、历史是否保留、关联对象如何处理、是否可恢复。

## 9. 核心表数据归属、权限与删除矩阵

> 表名是 MVP 建议，正式 migration 前由模块负责人提交表设计。未列出的新核心表也必须补充同等信息。

| 核心表 | 模块所有者 | 数据范围/权限 | 删除策略 |
|---|---|---|---|
| `sys_user` | 基础与权限 | 本人看脱敏资料；管理员按权限管理 | 停用 + 逻辑删除，不物理删历史账号 |
| `sys_role`、`sys_permission` | 基础与权限 | 指定管理员维护；普通用户只读自己的授权结果 | 内置项禁删；自定义项先做引用检查 |
| `sys_user_role`、`sys_role_permission` | 基础与权限 | 指定管理员授权，全部写审计 | 撤销关系保留操作记录 |
| `sys_audit_log` | 基础与权限 | 审计权限可查；业务用户不可修改 | 只追加，按保留策略归档 |
| `sys_file` | 基础与权限 | 上传者、管理员及业务对象授权用户访问；下载再次鉴权 | 未被引用的文件可由所有者删除；头像、课程资料和作业引用存在时禁止删除 |
| `edu_course_category` | 课程与学习 | 管理员维护，教师/学生只读启用项 | 有引用时禁删，只停用 |
| `edu_course` | 课程与学习 | 教师仅负责/协作课程；管理员治理；学生按选课/公开范围 | 逻辑删除或下线，保留历史 |
| `edu_course_teacher` | 课程与学习 | 课程负责人或管理员授权 | 状态化解除，保留授权历史 |
| `edu_course_review` | 课程与学习 | 提交教师、审核管理员按范围查看 | 只追加，不覆盖历史审核 |
| `edu_course_chapter`、`edu_course_lesson` | 课程与学习 | 课程编辑者写；已选学生读已发布内容 | 逻辑删除/下线，存在进度时禁物理删 |
| `edu_course_material` | 课程与学习 | 课程成员按资源可见范围；AI 仅取授权版本 | 逻辑删除；异步触发向量清理 |
| `edu_ai_conversation`、`edu_ai_message` | 课程与学习 | 仅会话本人；管理员默认不可读正文 | 软删仅影响本人视图，安全审计按保留策略保存 |
| `edu_course_enrollment` | 课程与学习 | 学生本人、课程教师、授权管理员 | 退选状态化，保留选退时间 |
| `edu_lesson_learning_record` | 课程与学习 | 学生本人读写自身；教师读负责课程 | 不允许业务删除，纠错留审计 |
| `edu_assignment` | 作业成绩预警 | 课程教师写；已选学生读已发布 | 逻辑删除/关闭，已有提交后禁物理删 |
| `edu_assignment_submission` | 作业成绩预警 | 学生本人提交；课程教师批改 | 不删除，重交产生版本/次数 |
| `edu_rubric`、`edu_rubric_item` | 作业成绩预警 | 课程教师管理；学生按发布策略读 | 被使用后版本化，不覆盖历史 |
| `edu_grade_record` | 作业成绩预警 | 教师写负责课程；学生只读本人已发布 | 不删除，后续更正/撤回需版本化 |
| `edu_learning_warning`、`edu_warning_evidence` | 作业成绩预警 | 学生本人和负责教师按视图读取 | 不删除证据，状态化处理 |
| `edu_warning_action` | 作业成绩预警 | 学生本人/负责教师写各自允许动作 | 只追加或受控更正 |
| `edu_question` | 考试题库 | 本人私有、课程共享、学校公共三种范围 | 已入卷后不可物理删，只停用/版本化 |
| `edu_exam`、`edu_exam_paper` | 考试题库 | 课程教师管理；学生按发布/时间范围读 | 取消/结束，不物理删 |
| `edu_exam_session`、`edu_exam_answer` | 考试题库 | 学生本人作答；授权教师阅卷 | 不删除，异常修复留记录 |
| `edu_announcement` | 课程与学习/互动 | 发布者管理；用户按受众读取 | 撤回/逻辑删除，保留发布历史 |
| `edu_forum_topic`、`edu_forum_reply` | 课程与学习/互动 | 课程成员参与；教师/管理员按范围治理 | 用户软删；治理处置保留证据 |
| `sys_outbox_event` | 公共基础 | 仅系统任务访问 | 发送成功后按保留期归档清理 |

## 10. Bootstrap SQL 维护规范

### 10.1 唯一初始化来源

```text
backend/edu-biz-service/src/main/resources/db/
└─ online_education_bootstrap.sql
```

- `online_education_bootstrap.sql` 是项目唯一的建表和演示数据初始化脚本。
- Compose 仅在 MySQL 数据卷为空时自动执行该脚本；应用启动不修改表结构。
- 禁止 Hibernate/MyBatis 自动建表更新共享环境。

### 10.2 变更要求

含数据库变更的 PR 必须同时修改该脚本，并说明：

- 表/字段所有者和业务目的。
- 新增/删除索引依据。
- 演示数据是否需要同步更新。
- 空 MySQL 8 数据库执行脚本的验证结果。

### 10.3 重建与备份

- Bootstrap SQL 面向空数据库；不用于保留既有业务数据的升级。
- 需要重建本地环境时，停止 Compose 后删除本项目 MySQL 数据卷，再重新执行 Compose。
- 有真实业务数据的环境必须先备份，并由管理员审核重建计划。

## 11. 多人协作规则

- 一张核心表只由 [`module-ownership.md`](./module-ownership.md) 指定模块维护。
- 跨模块新增字段先由数据所有者确认，不允许在自己的 PR 顺手修改他人表。
- `online_education_bootstrap.sql` 是公共高冲突文件；开发前先拉取 `dev`，合并前在空 MySQL 8 数据库完整执行脚本。
- 系统运行必需的角色、权限和演示课程都随 Bootstrap SQL 一起维护，避免表结构与本地数据分散。

## 12. 验证清单

每次数据库 PR 至少执行：

1. 全新 MySQL 8 空库执行 Bootstrap SQL 成功。
2. Mapper 集成测试通过。
3. 检查关联列/高频查询索引、唯一约束、无物理外键和逻辑删除条件。
4. 检查所有核心表包含七个统一审计字段。
5. 检查状态字段有枚举、含义和流转说明。
6. 检查数据库账号没有跨服务授权。
