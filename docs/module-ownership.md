# 三人开发中的后端模块归属与协作边界

> 本文冻结三人开发中的后端协作基线。它划分的是代码、数据和评审责任，不代表新增微服务。除 `edu-gateway` 和 `edu-ai-service` 外，传统业务仍部署在 `edu-biz-service`；`edu-common` 与 `edu-feign-api` 都只是 Maven 模块。前端成员的页面与联调任务见 `docs/team-division.md`。

## 1. 基线决策

- 成员 A 负责 Biz 主链和正式业务事实。
- 成员 B 负责 AI、考试题库、平台公告/分类/统计、网关、Docker、CI 和联调。
- 成员 C 负责 Vue 前端页面、API client、路由守卫、加载/错误/空状态和演示流程联调。
- `dev` 是唯一后端集成分支。
- `backend-1`、`backend-2` 只作为个人备份或临时分支，不作为长期集成分支。
- 所有 `feature/*`、`fix/*`、`docs/*` 从最新 `dev` 创建，通过 PR 回到 `dev`。
- 每日公共 PR 由成员 B 负责排队和执行合并；涉及数据库、权限、Biz shared、Bootstrap SQL 的 PR 必须成员 A review 后才能合并。
- 最终 Docker、CI、启动文档由成员 B 主负责；最终 Biz 演示数据、账号、课程/作业/成绩/预警 seed 由成员 A 主负责。

## 2. 总览

| 范围 | 成员 A | 成员 B |
|---|---|---|
| 主责 | Biz 主链、正式业务事实、数据库初始化规则、权限和资源范围评审 | AI 服务、考试题库、平台治理、Gateway、Docker、CI、公共 PR 排队 |
| 主要服务 | `edu-biz-service` | `edu-ai-service`、`edu-gateway`，以及 `edu-biz-service` 中的 `exam/**` |
| 独占包 | `auth/**` 主维护，`storage/**`，`course/**` 基线维护，`forum/**`，`assignment/**`，`grade/**`，`warning/**` | `com.zhongruan.edu.ai/**`，`exam/**`，`edu-gateway/**`，`backend/scripts/**`，CI 配置 |
| 共同契约模块 | `edu-common` 技术协议主审 | `edu-feign-api` Feign 契约主审，A 必须 review Biz 上下文和数据字段 |
| 独占数据 | auth 表、课程表、课程论坛表、作业/提交/成绩/预警表 | 考试安排、题库、答卷、公告、课程分类表；AI 自有 Redis namespace、Qdrant collection |
| 依赖对方 | AI 评语草稿、风险解释、组卷建议、Gateway 路由 | 课程、选课、课时、提交、成绩、预警的授权后 context |
| 禁止越界 | 不直接改 AI provider、SSE 协议、Gateway 限流、Qdrant 配置 | 不直接改作业、成绩、预警、课程正式表或 Biz Mapper |

## 3. 成员 A 独占范围

### 3.1 负责的包

```text
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/auth/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/storage/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/course/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/forum/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/assignment/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/grade/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/warning/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/shared/**
```

`shared/**` 是高风险公共区，成员 A 主审，成员 B 必须 review 涉及 AI/Gateway 的影响。

### 3.2 已有独占表

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
edu_course
edu_course_teacher
edu_course_enrollment
edu_course_chapter
edu_course_lesson
edu_course_material
edu_lesson_learning_record
edu_course_review
edu_assignment
edu_assignment_attachment
edu_assignment_submission
sys_file
edu_grade_record
edu_forum_topic
edu_forum_reply
edu_learning_warning
edu_warning_evidence
edu_ai_generation_record
```

### 3.3 后续扩展表

以下表名仍属于成员 A 的所有权边界。创建前需要先补契约、状态规则和迁移登记：

```text
edu_submission_attachment
edu_submission_version
edu_rubric
edu_rubric_item
edu_submission_rubric_score
edu_grade_version
edu_warning_action
```

### 3.4 业务边界

- 学生和教师由公开注册创建；`SUPER_ADMIN` 只由 Bootstrap 初始化，并独占普通管理员角色的授予与撤销能力。
- 作业成绩由成员 A 的作业/成绩模块保存和发布。
- 课程总成绩由成员 A 聚合，首版只读取已发布作业成绩；考试成绩后续只能通过接口或事件接入。
- 学习预警由成员 A 生成、保存和推进状态；成员 B 的 AI 只能提供解释和建议草稿。
- 论坛最小闭环固定为：列表、发帖、回复、教师或管理员治理可见性。
- AI 评语草稿只有被教师确认后，才能由成员 A 的 Biz 用例保存为正式评语。

## 4. 成员 B 独占范围

### 4.1 负责的包

```text
backend/edu-ai-service/src/main/java/com/zhongruan/edu/ai/**
backend/edu-gateway/src/main/java/com/zhongruan/edu/gateway/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/exam/**
backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/platform/**
backend/edu-feign-api/src/main/java/com/zhongruan/edu/feign/**
backend/scripts/**
.gitlab-ci.yml
```

### 4.2 已有考试与题库表

```text
edu_exam
edu_question_bank
edu_question
edu_question_option
edu_exam_paper
edu_exam_paper_question
edu_exam_attempt
edu_exam_answer
edu_course_category
edu_announcement
```

首版已具备题库、试卷、答题记录的基础表。后续如需要复杂在线考试会话、断线恢复、监考和自动判分，可扩展 `edu_exam_session`、`edu_question_version`、`edu_exam_proctor_log`、`edu_exam_auto_grade_rule` 等表，必须先更新 MVP 范围和 ADR。

### 4.3 AI 自有数据

AI 服务不拥有 Biz MySQL 表。成员 B 可维护：

```text
Qdrant collection: course_knowledge_v1
Redis namespace: ai:task:*
Redis namespace: ai:conversation-runtime:*
RabbitMQ indexing queues/exchanges
prompt/config version metadata
```

AI 向量 payload 只保存检索引用所需的最小字段，例如 `courseId`、`resourceId`、`resourceVersion`、`chunkId`、`locator`、`accessScope`。不得复制成绩、完整提交、考试答案或用户敏感信息。

### 4.4 业务边界

- 考试成绩首版不进入课程总成绩。
- 智能组卷只能返回候选题、理由和试卷草稿建议，不能自动发布考试或发布成绩。
- Gateway 只做入口、安全、路由、CORS、限流、SSE 转发和健康检查，不写课程、作业、成绩、考试业务逻辑。
- `.gitlab-ci.yml` 作为交付物补充到当前 GitHub 仓库；第 0 阶段不迁移到 GitLab。

## 5. AI 禁止修改区域

`edu-ai-service` 禁止：

- 配置 Biz MySQL 数据源。
- 引用 `edu-biz-service` 的 Entity、Mapper、Service 实现类。
- 直接写课程、作业、成绩、评语、预警、题库、试卷、论坛等正式业务表。
- 自行扩大用户可访问的课程资料、提交内容或题库范围。
- 返回系统 prompt、模型原始推理过程、密钥或未授权资料片段。

AI 只能返回：

```text
answer
draft
suggestion
citation
explanation
taskStatus
```

正式采用、保存、发布、撤回、关闭等动作必须由 `edu-biz-service` 完成。

## 6. 高风险公共区

| 位置 | Owner | 规则 |
|---|---|---|
| `backend/pom.xml`、各模块 `pom.xml` | A+B | 单独 PR，说明依赖影响 |
| `backend/edu-common/**` | A 主审 | 只放技术协议，不放业务 Entity 或跨服务业务 DTO |
| `backend/edu-feign-api/**` | B 主审，A review | 只放服务间 Feign Client、内部 DTO 和契约常量，不放实现逻辑 |
| `backend/edu-biz-service/src/main/resources/db/online_education_bootstrap.sql` | A 主审 | 唯一初始化来源；结构和演示数据变更须在空 MySQL 8.4 验证 |
| `backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/shared/**` | A 主审 | 权限、审计、异常、trace 改动需 B review |
| `backend/edu-gateway/**` | B 主审 | 业务模块只提路由/限流需求，不写业务逻辑 |
| `backend/edu-ai-service/**` | B 主审 | Biz 只提 context 和契约需求 |
| `application*.yml`、Nacos 配置 | B 主审 | 不提交真实密钥 |
| `.gitlab-ci.yml`、Docker、启动脚本 | B 主审 | 提供可复现启动和验证方式 |
| `docs/api-reference.md` | 对应模块 owner | 破坏性变更必须双方确认 |

## 7. 跨模块接力规则

1. 请求方先写契约或 issue，不直接改对方 owned 包和表。
2. 数据所有者负责定义表结构、状态枚举和正式写入命令。
3. AI 或考试模块需要成绩、提交、课程信息时，只能通过成员 A 提供的 application interface、内部 HTTP contract 或事件读取授权后的最小 context。
4. 成员 A 需要 AI 草稿时，只能通过成员 B 提供的 AI API/adapter 调用，不能直接引用 AI 实现类。
5. 合并顺序通常为：契约 -> 提供方 -> 消费方 -> 端到端验证。

## 8. 第 1 阶段前检查清单

- [ ] `docs/adr/0001-ai-boundary.md` 已合并。
- [ ] Bootstrap SQL 初始化规则已确认并同步到团队文档。
- [ ] `docs/adr/0003-ai-stack.md` 已合并。
- [ ] `docs/migration-register.md` 已合并。
- [ ] `docs/module-ownership.md` 已更新为双人 owner。
- [ ] 当前第 0 阶段文档 PR 从 `dev` 创建，并准备合回 `dev`。
- [x] 作业、考试、AI 采用审计、论坛、预警、公告和课程分类基础表已合入唯一的 `online_education_bootstrap.sql`。
- [x] 作业、成绩、论坛、预警和文件能力已完成 Controller/Service 与集成测试。
- [ ] 成员 A 和成员 B 明确明天的第一个分支。
