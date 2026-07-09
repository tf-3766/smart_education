# 三人开发任务拆分计划

> 适用仓库：`E:\my_projects\smart_education`  
> 团队结构：后端 A、后端 B、前端 C  
> 目标：两名后端并行推进、前端可同步联调，避免同时争抢公共模块、数据库迁移和接口契约。

## 1. 分工总览

| 成员 | 主责 | 主要目录 | 交付标准 |
|---|---|---|---|
| 后端 A | Biz 主链和正式业务事实 | `edu-biz-service/auth`、`course`、`assignment`、`grade`、`forum`、`warning`、`shared` | 作业提交、批改成绩、论坛、预警可通过 Gateway 调用并有集成测试 |
| 后端 B | AI、考试题库、Gateway、部署联调 | `edu-ai-service`、`edu-gateway`、`edu-feign-api`、`edu-biz-service/exam`、`deploy`、CI | AI Fake/SSE、考试题库、智能组卷、Gateway 路由限流和启动脚本可验证 |
| 前端 C | 三端页面与接口联调 | `src/**`、`ui-spec.md`、`wireframes.md`、`sitemap.md` | 学生、教师、管理员页面接入真实 API，具备加载、错误、空状态和演示流程 |

## 2. 后端 A 任务包

### A1 作业与提交闭环

- 包：`assignment/**`
- 表：`edu_assignment`、`edu_assignment_attachment`、`edu_assignment_submission`
- 接口：
  - `GET/POST /api/v1/teacher/courses/{courseId}/assignments`
  - `PUT /api/v1/teacher/assignments/{assignmentId}`
  - `POST /api/v1/teacher/assignments/{assignmentId}/publish`
  - `GET /api/v1/student/courses/{courseId}/assignments`
  - `POST /api/v1/student/assignments/{assignmentId}/submissions`
- 验收：教师发布作业、学生提交、未选课/逾期/重复提交被正确处理。

### A2 批改、成绩与统计

- 包：`grade/**`，必要时扩展 `assignment/**`
- 表：`edu_grade_record`、`edu_assignment_submission`
- 接口：
  - `GET /api/v1/teacher/assignments/{assignmentId}/submissions`
  - `POST /api/v1/teacher/submissions/{submissionId}/grade`
  - `GET /api/v1/student/grades`
- 验收：教师保存评分、发布成绩，学生只能查看本人已发布成绩，教师能查看课程作业统计。

### A3 论坛与学习预警

- 包：`forum/**`、`warning/**`
- 表：`edu_forum_topic`、`edu_forum_reply`、`edu_learning_warning`、`edu_warning_evidence`
- 接口：
  - `GET/POST /api/v1/student/courses/{courseId}/forum/topics`
  - `POST /api/v1/student/forum/topics/{topicId}/replies`
  - `PATCH /api/v1/teacher/forum/topics/{topicId}/visibility`
  - `GET /api/v1/student/warnings`
  - `GET /api/v1/teacher/courses/{courseId}/warnings`
  - `POST /api/v1/teacher/warnings/{warningId}/handle`
- 验收：课程成员才能参与论坛；预警基于进度、缺交、低分生成，处理状态可追踪。

## 3. 后端 B 任务包

### B1 AI Fake Adapter 与 SSE

- 包：`edu-ai-service/**`、`edu-feign-api/**`
- 内部契约：`/_internal/v1/ai-context/course`
- 接口：
  - `POST /api/v1/ai/courses/{courseId}/qa/stream`
  - `POST /api/v1/ai/lessons/{lessonId}/summary-draft`
  - `POST /api/v1/ai/submissions/{submissionId}/comment-draft`
  - `POST /api/v1/ai/warnings/{warningId}/explanation`
  - `POST /api/v1/ai/exams/paper-suggestions`
- 验收：Fake Adapter 返回稳定示例；SSE 包含 `meta/delta/citation/done/error`；AI 不连接 Biz MySQL。

### B2 考试、题库与试卷草稿

- 包：`edu-biz-service/exam/**`
- 表：`edu_exam`、`edu_question_bank`、`edu_question`、`edu_question_option`、`edu_exam_paper`、`edu_exam_paper_question`
- 接口：
  - `GET/POST /api/v1/teacher/courses/{courseId}/question-banks`
  - `GET/POST /api/v1/teacher/question-banks/{bankId}/questions`
  - `GET/POST /api/v1/teacher/courses/{courseId}/exams`
  - `POST /api/v1/teacher/exams/{examId}/papers`
  - `GET /api/v1/student/courses/{courseId}/exams`
- 验收：教师能维护最小题库和考试安排，AI 组卷建议可保存为试卷草稿；首版不要求完整在线考试引擎。

### B3 Gateway、部署与联调

- 包：`edu-gateway/**`、`deploy/**`、CI 配置
- 工作：
  - 验证 `/api/v1/ai/**` SSE 转发不被缓冲。
  - 验证 JWT、CORS、traceId、AI 限流和 429 错误结构。
  - 补 `.gitlab-ci.yml` 或等价 CI 交付物。
  - 更新本地启动和演示数据说明。
- 验收：前端只访问 Gateway；Biz/AI 均可注册发现；`.\mvnw.cmd clean verify` 通过。

## 4. 前端 C 任务包

| 任务 | 依赖后端 | 验收 |
|---|---|---|
| 登录、角色切换、路由守卫 | Auth API | 未登录跳转登录，三类角色进入对应首页 |
| 学生课程与章节学习 | Course API、AI QA | 可选课、进入章节、完成课时、打开 AI 侧栏 |
| 学生作业与成绩 | A1/A2 | 可查看作业、提交、查看成绩和反馈 |
| 教师工作台与批改 | A1/A2、B1 | 可查看待批改、调用 AI 评语草稿、发布成绩 |
| 教师智能组卷 | B1/B2 | 可配置组卷条件、展示建议、保存草稿 |
| 管理员看板与课程审核 | Course/Admin API | 可查看课程审核队列并审核 |

## 5. 并行开发顺序

1. 共同冻结接口：`docs/api-contract-platform.md` 与 `docs/openapi/platform-api.openapi.yaml` 先合并。
2. 后端 A 先交作业/成绩最小闭环，后端 B 同时交 AI Fake/SSE 和 Gateway 验证。
3. 前端 C 使用 mock 或演示 seed 接入已冻结接口，后端接口完成后替换为真实调用。
4. A/B 分别补论坛预警、考试题库；C 补页面错误态和空状态。
5. 最后做 Gateway 端到端联调、演示数据、启动文档和验收脚本。

## 6. 禁止互相阻塞的规则

- 后端 B 需要 Biz 数据时，只改 `edu-feign-api` 契约或向后端 A 提 issue，不直接读 A 的 Mapper。
- 后端 A 需要 AI 草稿时，只调用 B 的 AI API 或内部契约，不引用 AI 实现类。
- 前端 C 不口头新增字段；任何字段变更先改 API 文档或 OpenAPI。
- Flyway 生产迁移只新增不修改，跨 A/B 表边界的变更必须登记到 `docs/migration-register.md`。
- `edu-common`、`edu-feign-api`、父 POM、Gateway 安全配置都算公共高风险区，单独 PR。

