# 双人后端协作计划

> 适用仓库：`E:\my_projects\smart_education`
>
> 生成日期：2026-07-07
>
> 目标：让两名后端成员在不互相踩表、不反复改公共配置、不让 AI 服务越界写业务事实的前提下，并行完成可演示的在线教育辅助教学系统后端 MVP。

## 0. 从旧计划迁移到当前仓库的调整

旧计划是在 `E:\my_projects\zhongruan` 中制定的，当时最大的前置问题是 `.git` 不可用。当前仓库已经移动并整理为 `E:\my_projects\smart_education`，因此第 0 阶段不再按“修复空 Git 仓库”处理，而是按“确认远端基线、统一集成分支、保护公共区”处理。

当前已确认的差异：

| 项目 | 当前事实 | 对计划的影响 |
|---|---|---|
| 仓库目录 | `E:\my_projects\smart_education` | 文档和命令全部使用当前目录 |
| Git 状态 | 生成本文前当前分支为 `backend-2`，工作区干净，本地领先 `origin/backend-2` 1 个提交 | Git 可用；先提交本文，再 push 或 PR 当前后端提交 |
| 远端 | `origin = https://github.com/tf-3766/smart_education.git` | 可以按 PR/分支流程协作 |
| 远端默认分支 | `origin/HEAD -> origin/dev` | 日常集成分支使用 `dev`，不是旧计划里的 `develop` |
| 已有后端目录 | `backend/edu-common`、`backend/edu-feign-api`、`backend/edu-gateway`、`backend/edu-biz-service`、`backend/edu-ai-service` | 继续保持三个应用服务，`edu-common` 与 `edu-feign-api` 是 Maven 模块 |
| 已有文档目录 | `docs/`，已有架构、规范、课程契约、模块归属、团队 Git 流程 | 本文只补“双人执行计划”，不替代通用规范 |

建议先阅读并保持一致的文档：

- `docs/backend-architecture.md`
- `docs/backend-conventions.md`
- `docs/database-conventions.md`
- `docs/api-style.md`
- `docs/module-ownership.md`
- `docs/team-development-workflow.md`
- `docs/course-api-contract.md`
- `docs/course-module-delivery.md`
- `backend/README.md`

### 0.1 实训选题中的技术栈约束

来自 `E:\武汉理工实训选题.docx` 中“选题8：在线教育辅助教学系统”和“技术栈清单参考”的要求，本项目不能随意扩大架构，也不要把 AI 做成一个脱离业务闭环的展示模块。

| 约束项 | 文档要求 | 本计划落地方式 |
|---|---|---|
| 传统业务域 | 课程管理、学生选课、作业提交与打分、考试安排、成绩统计、论坛讨论区 | Biz 服务必须覆盖这些功能；论坛做课程内最小讨论闭环，不做复杂治理 |
| AI 智能域 | 课程知识库 RAG 答疑、作业批改评语、学习进度预警、章节知识点摘要、智能组卷建议 | AI 服务提供建议、草稿、引用和解释；正式业务事实仍由 Biz 保存 |
| 架构 | `1 个网关 + 1 个业务微服务 + 1 个 AI 智能体微服务` | 保持 `edu-gateway`、`edu-biz-service`、`edu-ai-service`，不再拆更多业务微服务 |
| 基础底座 | JDK 21、Spring Boot 3.x | 后端统一 JDK 21；AI SSE/长连接可优先使用虚拟线程或响应式方案 |
| 微服务框架 | Spring Cloud Alibaba 轻量级，只采用 Nacos、Gateway、OpenFeign | 不引入 Sentinel、Seata 等额外组件，避免配置地狱 |
| 持久层 | MyBatis-Plus、MySQL 8.0 | 传统业务事实全部在 Biz + MySQL，Flyway 管理结构 |
| 缓存与消息 | Redis、RabbitMQ | Redis 用于限流、缓存、锁和短期任务状态；RabbitMQ 用于 AI 索引、异步建议任务等解耦场景 |
| AI 框架 | Spring AI / LangChain4j | 默认选 Spring AI；如改 LangChain4j 必须写 ADR |
| 向量数据库 | Milvus 或 Qdrant | 默认选 Qdrant，优先降低本地部署复杂度；如改 Milvus 必须写 ADR |
| 前端与工程化 | Vue 3、Docker、GitLab CI/CD | 当前仓库远端在 GitHub；如老师验收要求 GitLab CI/CD，第 0 阶段确认是否镜像到 GitLab，并至少准备 `.gitlab-ci.yml` |

默认选型结论：

```text
JDK 21 + Spring Boot 3.x
Spring Cloud Alibaba: Nacos + Gateway + OpenFeign only
MyBatis-Plus + MySQL 8.0 + Flyway
Redis + RabbitMQ
Spring AI + Qdrant
Vue 3 + Docker + GitLab CI/CD deliverable
```

如果后续要改变 `Spring AI/Qdrant/GitLab CI/CD` 这三个默认结论，必须先同步并记录到 `docs/adr/`。

## 1. 当前项目状态检查

### 1.1 已有模块和服务

| 模块 | 当前职责 | 状态判断 |
|---|---|---|
| `edu-common` | 统一响应、错误码、JWT、trace/request context 等技术协议 | 已有基础能力，是高风险公共区 |
| `edu-feign-api` | 服务间 Feign Client、内部 DTO 和版本化上下文契约 | 新增契约模块，不部署，不放业务 Entity/Mapper |
| `edu-gateway` | 统一入口、JWT 网关过滤、traceId、AI 限流配置、路由 | 已有骨架，后续需验证 SSE、限流和 CORS |
| `edu-biz-service` | 认证权限、课程学习、MySQL/Flyway、业务事实 | 已有 auth 和 course 主体，后续业务都应先落在这里 |
| `edu-ai-service` | AI 服务入口、安全过滤、异常处理、健康检查 | 目前是可启动骨架，尚未形成 RAG/摘要/评语/组卷闭环 |

### 1.2 已有数据库迁移

当前 Flyway 生产迁移目录：

```text
backend/edu-biz-service/src/main/resources/db/migration/
```

已有迁移：

```text
V1__init_auth_tables.sql
V2__create_course_tables.sql
V3__create_course_review_table.sql
```

已有本地演示数据目录：

```text
backend/edu-biz-service/src/main/resources/db/localmigration/
```

规则：`V1` 到 `V3` 一旦共享后不要再改。后续新增表建议使用时间戳版本，例如：

```text
V202607071430__create_assignment_grade_tables.sql
V202607071500__create_exam_question_tables.sql
V202607071530__create_warning_tables.sql
```

### 1.3 能力完成度

| 能力 | 当前状态 | 备注 |
|---|---|---|
| Maven 多模块 | 已完成 | 根在 `backend/pom.xml` |
| JWT 登录认证 | 已有 | 需要后续补刷新/撤销等增强时单独评审 |
| 学生、教师、管理员角色 | 已有基础 | 资源级权限仍需每个业务模块自己校验 |
| 统一响应、异常、错误码、traceId | 已有 | 公共错误码新增需 review |
| Flyway | 已接入 | 后续只新增 migration，不改历史 migration |
| MyBatis-Plus 审计、逻辑删除、乐观锁 | 已有基础 | 新表必须沿用统一规则 |
| 课程、章节、课时、资料、选课、学习进度 | 已有主体实现 | 后续作为作业、AI、考试的上游依赖 |
| 作业、提交、评分、成绩 | 未开始或未形成闭环 | MVP 必须完成 |
| 学习预警 | 未开始 | MVP 做规则型最小实现 |
| 考试安排、题库、智能组卷建议 | 未开始 | MVP 做题库和组卷建议，不做完整考试引擎 |
| RAG 答疑、章节摘要、AI 评语草稿 | 未开始 | AI 服务不能直接写正式业务表 |
| 论坛讨论区、公告、管理员统计 | 未开始或仅规划 | 论坛讨论区做最小闭环；公告和统计可简化或延后 |

### 1.4 当前协作风险

高风险公共区：

- `backend/pom.xml` 和各模块 `pom.xml`
- `backend/edu-common/**`
- `backend/edu-feign-api/**`
- `backend/edu-gateway/**`
- `backend/edu-biz-service/src/main/resources/db/migration/**`
- `backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/shared/**`
- `backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/auth/**`
- `backend/edu-biz-service/src/main/resources/application*.yml`
- `backend/edu-ai-service/src/main/resources/application*.yml`
- `docs/api-style.md`
- `docs/database-conventions.md`
- `docs/module-ownership.md`
- `docs/team-development-workflow.md`
- `docs/openapi/**`

已经收敛的技术决策：

- 日常集成分支使用 `dev`，`backend-1`、`backend-2` 只作为个人备份或临时集成分支。
- 后续 Flyway 使用时间戳版本，不继续争抢 `V4/V5`。
- AI 框架默认使用 Spring AI，LangChain4j 只作为 ADR 变更后的备选。
- 向量库默认使用 Qdrant，Milvus 只作为 ADR 变更后的备选。
- 网关只做入口、安全、路由、限流和 CORS，不承担课程、作业、评分、考试等业务校验。

仍需要第一次同步会确认的决策：

- AI 首版使用真实模型，还是先用 fake adapter 保证联调。
- 当前 GitHub 远端是否需要镜像到 GitLab，或只提交 `.gitlab-ci.yml` 作为 CI/CD 交付物。
- 作业成绩、考试成绩、学习预警之间的表边界。
- 课程论坛最小闭环做到“发帖/回复/删除”还是只做“发帖/回复/列表”。

## 2. 推荐工作流

结合旧线程的 `/ask-matt` 结论和当前仓库状态，推荐流程如下：

| 顺序 | 工作流 | 用途 | 执行频率 |
|---|---|---|---|
| 1 | 项目状态检查 | 确认当前代码、文档、Git、数据库迁移真实状态 | 每次大迁移或换目录后执行 |
| 2 | 领域模型确认 | 统一课程、作业、成绩、预警、AI 建议、考试草稿等词汇 | 每个新业务域开工前执行 |
| 3 | 模块边界设计 | 明确谁 owns 表、接口、包、错误码、状态枚举 | 每个跨模块功能前执行 |
| 4 | 契约先行 | 先写 API/事件/内部 context，再写实现 | 每个 feature 必做 |
| 5 | TDD 或最小集成测试 | 先覆盖权限、状态流转、越权、冲突场景 | 每个 feature 必做 |
| 6 | Code Review | 特别检查权限、迁移、公共模块、AI 边界 | 每个 PR 必做 |

本次只创建协作计划，不批量生成 Controller、Service、Mapper 或数据库表。

## 3. 双人职责矩阵

最终推荐分工：成员 A 负责 Biz 主链和正式业务事实，成员 B 负责 AI/考试/Gateway/部署联调。这样两个人都有可展示成果，同时减少对同一批核心表的并发修改。

| 维度 | 后端成员 A | 后端成员 B |
|---|---|---|
| 主责方向 | Biz 主链：认证权限基线、课程学习基线、课程论坛、作业、提交、评分、成绩统计、学习预警正式记录 | AI 服务、考试安排/题库/试卷草稿、智能组卷、Gateway、Docker/CI 联调 |
| 主要服务 | `edu-biz-service` | `edu-ai-service`、`edu-gateway`，以及 `edu-biz-service` 内 exam 包 |
| 独占包 | `auth/**` 主维护、`course/**` 基线维护、`forum/**`、`assignment/**`、`grade/**`、`warning/**` | `com.zhongruan.edu.ai/**`、`exam/**`、`edu-gateway/**`、`backend/scripts/**`、CI 配置 |
| 独占表 | `sys_user`、`sys_role`、`sys_permission`、课程 8 表、论坛最小表、作业/提交/成绩/预警新增表 | 考试安排、题库、试卷草稿新增表；AI 自有 Redis namespace、Qdrant collection |
| 可读不可随意改 | `edu-ai-service`、`edu-gateway`、考试题库表 | `course/**`、`assignment/**`、`grade/**`、`warning/**`、正式业务表 |
| 高风险公共区职责 | A 主审数据库、Biz shared、权限、错误码 | B 主审 Gateway、AI 配置、部署、OpenAPI 聚合 |
| 依赖对方 | 需要 B 提供 AI 评语草稿、风险解释、组卷建议接口 | 需要 A 提供课程、作业、提交、成绩、预警的内部授权 context |
| 完成定义 | 教师发布作业、学生提交、教师评分、学生查成绩和风险可以走通 | 学生课程 AI 答疑、教师 AI 评语草稿、教师智能组卷建议可以走通 |

关键边界：

- AI 服务不能配置 Biz MySQL 数据源，不能引用 Biz Entity/Mapper。
- AI 只能返回建议、草稿、引用、任务状态；正式保存由 Biz 用例完成。
- B 做考试时不能直接写 A 的成绩总表。考试成绩如进入总成绩，必须通过 A 提供的成绩接口或事件。
- A 做作业和预警时不能直接调用 B 的 AI 实现类。只能通过 HTTP 契约、Feign client 或明确 adapter。
- 两个人都不能“顺手”修改 `edu-common`、`edu-feign-api`、父 POM、历史 Flyway、JWT claims、Gateway 安全规则。

## 4. MVP 功能范围

必须保证最终演示链路：

```text
管理员审核课程
-> 教师创建课程、章节和作业
-> 学生选课并学习章节
-> 学生使用课程 AI 答疑
-> 学生提交作业
-> 教师评分并使用 AI 生成评语草稿
-> 学生查看成绩和学习风险
-> 教师查看智能组卷建议
```

### 4.1 必须完成

| 功能 | Owner | 说明 |
|---|---|---|
| 课程审核、教师课程、章节、课时 | A | 已有主体，后续作为稳定上游 |
| 学生选课、学习记录、课程进度 | A | 已有主体，补联调数据和契约稳定性 |
| 作业发布 | A | 支持文本说明、截止时间、附件元数据 |
| 学生作业提交 | A | 支持草稿/正式提交/截止校验 |
| 教师评分和成绩发布 | A | 支持未发布和已发布成绩 |
| 成绩统计 | A | 支持课程作业成绩列表、平均分、未交/低分统计 |
| 学习预警 | A | 使用进度、缺交、低分做规则型预警 |
| 课程论坛讨论区 | A | 课程下发帖、回复、列表；管理员或教师删除违规内容 |
| 课程 AI 答疑 | B | 支持 fake adapter 和引用结构，后续可替换真实模型 |
| AI 评语草稿 | B | 由 AI 返回草稿，教师确认后 A 写入正式评语 |
| 考试安排、题库和智能组卷建议 | B | 支持考试时间窗、题库、建议和试卷草稿，不做完整在线考试会话 |
| Gateway 路由和联调 | B | 验证 Biz、AI、SSE、限流和错误响应 |

### 4.2 可以简化

- 文件上传先只保存附件元数据和 URL，不做完整对象存储。
- RAG 首版可以 fake vector adapter，先保证接口、引用、无来源响应稳定。
- 学习预警先规则计算，不做复杂模型。
- 智能组卷先返回候选题和理由，不自动发布试卷。
- 考试安排只做到教师创建考试计划、学生查看安排，不做在线答题引擎。
- 论坛讨论区只做课程内帖子和回复，不做复杂审核流、推荐流和敏感词系统。
- 管理员统计先做少量聚合接口或演示数据，不做大屏。
- RabbitMQ 可先保留事件契约，首版关键路径允许同步调用。

### 4.3 延后开发

- 完整在线考试引擎。
- 完整消息中心。
- AI 管理后台。
- 真实对象存储、病毒扫描、签名 URL。
- 批量 AI 批改。
- 复杂学习风险模型。
- 多租户、组织架构、班级复杂权限。
- 移动端专项接口。

## 5. 分阶段计划

| 阶段 | 目标 | 成员 A 任务 | 成员 B 任务 | 共同任务 | 迁移和契约 | 合并顺序 | 验证方式 | 主要冲突 |
|---|---|---|---|---|---|---|---|---|
| 第 0 阶段 | 协作基线 | 确认 Biz owner、数据库命名、现有课程状态 | 确认 Gateway/AI owner、部署端口、AI 首版策略 | push 或 PR 当前 `backend-2` 提交；确认 `dev` 为集成分支；更新 owner 文档 | 不新增业务表；先确认时间戳 migration 规则 | docs/chore 先合入 `dev` | `git status` 干净，远端分支可拉取 | `dev`、`backend-1`、`backend-2` 的用途不清 |
| 第 1 阶段 | 稳固基础 | 冻结 auth/course 公共接口；补缺失权限测试 | 验证 Gateway 到 Biz/AI 路由、401/403/429、SSE 预留 | 补 API style、错误码和 `edu-feign-api` 契约约定 | 不改 V1-V3；新增契约文档和后续时间戳迁移 | 公共配置 PR 先合，业务 PR 后合 | `mvn clean verify`，Gateway smoke test | `edu-common`、`edu-feign-api`、JWT、Gateway 配置 |
| 第 2 阶段 | 作业、成绩和论坛契约 | 设计作业/提交/评分/成绩/论坛最小表和 API | 设计 AI 评语草稿输入输出 | 确认教师/学生/无关教师权限场景 | A 写 assignment/grade/forum migration；B 写 AI grading contract | 契约 -> migration -> A 实现 -> B 接入 | 作业发布、提交、评分、论坛发帖集成测试 | 作业是否绑定 lesson，成绩是否允许更正，论坛是否属于课程域 |
| 第 3 阶段 | 作业成绩和论坛闭环 | 实现作业发布、学生提交、教师评分、成绩发布、课程论坛列表/发帖/回复 | 提供 fake AI 评语草稿接口 | 联调教师采用 AI 草稿后保存正式评语 | AI 不写成绩表；正式评语由 A 保存 | A 的 Biz API 先合，B 的 AI 接入后合 | 正常、越权、截止后、重复提交、发布后修改、论坛越权测试 | AI 评语和正式评语边界 |
| 第 4 阶段 | 课程 AI 答疑和章节摘要 | 提供课程/资料/选课授权 context | 实现课程 QA SSE、引用、无来源、摘要草稿 | 统一 SSE 事件和错误码 | 课程 context 走 `/_internal/v1/ai-context/**` | A context -> B AI -> Gateway 路由 | 学生只能问自己有权课程，SSE 可中断 | AI 越权读取课程资料 |
| 第 5 阶段 | 预警、考试安排、题库和组卷建议 | 实现学习预警正式记录和学生/教师查看，补成绩统计 | 实现考试安排、题库、试卷草稿、智能组卷建议 | 确认考试成绩是否进入总成绩 | A warning migration；B exam/question migration | 题库契约 -> 考试安排 -> 组卷建议 -> 预警聚合 | 低分、缺交、进度落后预警；学生查看考试安排；题库不足错误 | 考试模块直接写成绩 |
| 第 6 阶段 | 总联调和答辩准备 | 准备业务演示数据和验收脚本 | 准备 AI/Gateway/部署脚本和健康检查 | 统一演示脚本、README、环境变量模板 | dev seed 与生产 migration 分离 | 所有 feature PR 合入 `dev` 后再发发布 PR | 完整演示链路，空库迁移，三角色验收 | demo 数据、端口、密钥、Docker 环境 |

### 5.1 18 天详细开发排期

如果实训周期按约 18 天推进，推荐按下面节奏执行。每一天的产物都要能被提交、评审或演示，不把风险堆到最后。

| 天数 | 成员 A：Biz 主链 | 成员 B：AI/考试/Gateway | 当天共同产物 |
|---|---|---|---|
| Day 1 | 核对 auth/course 现状，列出 Biz 表 owner | 核对 gateway/ai 现状，确认端口和服务启动方式 | 确认 `dev` 集成分支、Spring AI + Qdrant 默认选型、时间戳 Flyway 规则 |
| Day 2 | 写作业/成绩/论坛 API 契约和表草图 | 写 AI QA/评语/摘要/组卷 API 契约和 SSE 事件 | 合并第一批 docs PR，确认内部 `/_internal/v1/ai-context/**` 形状 |
| Day 3 | 新增作业/提交/成绩/论坛 migration 草案和测试骨架 | 建 Spring AI fake adapter、AI SSE 基础响应、Gateway AI 路由验证 | `mvn clean verify` 通过，migration 命名无冲突 |
| Day 4 | 实现教师创建/发布作业，补权限测试 | 实现 AI 评语草稿 fake endpoint，定义请求/响应样例 | 教师作业发布接口和 AI 评语契约可联调 |
| Day 5 | 实现学生草稿/正式提交/截止校验 | 实现 AI 任务错误码、无来源/失败响应格式 | 学生提交作业闭环通过集成测试 |
| Day 6 | 实现教师评分、成绩发布、课程成绩统计 | 接入提交 context，返回评语草稿和评分建议理由 | “提交 -> AI 草稿 -> 教师确认 -> 成绩发布”打通 |
| Day 7 | 实现课程论坛发帖/回复/列表/删除最小闭环 | 补 Gateway 401/403/429/SSE 断流测试，准备 Docker env | 论坛不阻塞主链，Gateway 行为有测试证据 |
| Day 8 | 提供课程资料/选课/课时授权 context | 实现课程 AI 答疑 fake RAG、citation、无来源提示 | 学生只能问已选课程，越权返回 403 |
| Day 9 | 提供章节摘要发布/采用的 Biz 保存接口 | 实现章节知识点摘要草稿 | “AI 摘要草稿 -> 教师确认 -> Biz 保存”打通 |
| Day 10 | 实现学习预警正式表、规则计算、学生/教师查看 | 实现风险解释/建议 AI 草稿接口 | 预警由 Biz 生成，AI 只解释和建议 |
| Day 11 | 补成绩统计和预警边界测试 | 实现考试安排、题库、题目基础 CRUD | 学生可查看考试安排，教师可维护题库 |
| Day 12 | 提供课程/成绩/章节约束给组卷 context | 实现智能组卷建议和试卷草稿保存 | “题库 -> AI 建议 -> 教师保存草稿”打通 |
| Day 13 | 补 dev seed：课程、学生、作业、成绩、预警、论坛 | 补 Docker Compose/Qdrant/RabbitMQ/Redis/Nacos 配置和健康检查 | 一键启动依赖，演示数据可重复 |
| Day 14 | 串联管理员、教师、学生完整 Biz 演示链路 | 串联 AI QA、评语、摘要、组卷建议链路 | 完成第一轮端到端演示 |
| Day 15 | 补越权、状态冲突、截止时间、重复提交测试 | 补 AI 超时、SSE 中断、限流、无来源测试 | 后端测试和主要联调用例通过 |
| Day 16 | 整理数据库 ER、接口说明、实训报告中的传统业务部分 | 整理 AI 架构、RAG、CI/CD、Docker 部分 | 文档和代码能力一一对应 |
| Day 17 | 准备演示脚本、三角色账号、答辩问题 | 准备启动脚本、CI 截图、AI fake/真实 provider 切换说明 | 冻结功能，只修 bug 和文档 |
| Day 18 | 最终回归和答辩演示 | 最终回归和答辩演示 | `dev` 合入发布分支，产出最终交付物 |

### 5.2 进度不一致时的协作规则

如果某个人进度更快，不会天然影响项目，反而可以提升整体质量；真正有风险的是“快的人直接改慢的人 owned 表、Mapper、Service 或公共配置”。因此处理原则是：进度可以接力，所有权不能乱。

规则：

1. 快的人优先从“备用任务池”拿任务，不直接接管对方核心实现。
2. 需要改对方模块时，先写契约、测试或 TODO PR，由 owner review 后再合并。
3. 被对方阻塞超过半天时，先用 fake adapter、stub client、dev seed 或 contract test 继续推进。
4. 对方模块尚未完成时，只依赖已合并的 API 契约，不依赖未合并代码。
5. 公共文件由 owner 主持合并，快的人可以提交 PR，但不能自己绕过评审。
6. 每天下班前同步“今天合了什么、明天会碰哪些高风险文件、卡在哪里”。

备用任务池：

| 情况 | 快的人可以做 | 不能做 |
|---|---|---|
| A 比 B 快 | 补作业/成绩/论坛测试；补 dev seed；整理 ER 图；补接口文档；补成绩统计；准备演示数据 | 直接改 AI provider、SSE 协议、Gateway 限流、Qdrant 配置 |
| B 比 A 快 | 补 Docker/CI；补 Gateway smoke test；完善 fake AI；整理 AI 文档；补 Qdrant/RabbitMQ 健康检查；写 contract test | 直接改作业/成绩/预警正式表，直接写 Biz Mapper 或替 A 定义状态枚举 |
| 任意一人快 | 补 README、ADR、OpenAPI 示例、Postman/HTTP 请求样例、答辩脚本、错误码表、越权用例 | 修改历史 migration、改 JWT claims、改 `edu-common` 或 `edu-feign-api` 公共类型而不评审 |

影响判断：

- 正向影响：快的人补测试、文档、seed、CI、契约示例，可以让慢的人更容易联调。
- 中性影响：快的人提前做自己后续模块，只要不依赖未合并代码，就不会阻塞对方。
- 负面影响：快的人跨 owner 直接改表和核心业务，会导致 merge 冲突、状态语义不一致、答辩时解释不清。
- 最坏情况处理：某人连续 2 天无法推进时，把其模块缩到最小可演示闭环，另一人只能接“接口实现或测试补齐”，仍保留原 owner 负责最终解释和 review。

## 6. Git 协作计划

### 6.1 当前分支策略

当前远端已有：

```text
origin/dev
origin/main
origin/backend-1
origin/backend-2
origin/frontend
```

推荐用途：

| 分支 | 用途 | 规则 |
|---|---|---|
| `main` | 稳定发布和答辩版本 | 只从 `dev` 发发布 PR |
| `dev` | 后端日常集成分支 | 所有后端功能 PR 默认合入这里 |
| `backend-1` | 成员 A 的个人集成/备份分支 | 不作为长期公共集成分支 |
| `backend-2` | 成员 B 的个人集成/备份分支 | 当前本地在此分支，先同步远端 |
| `feature/*` | 单个功能开发 | 从最新 `dev` 创建，合并后删除 |
| `fix/*` | 缺陷修复 | 从最新 `dev` 创建，合并后删除 |
| `docs/*` | 文档和契约 | 从最新 `dev` 创建，小 PR 快速合并 |

第一步建议：

```powershell
git status
git fetch origin
git push origin backend-2
```

如果当前 `backend-2` 这 1 个本地提交已经是要共享的后端结构调整，就先推送并开 PR 到 `dev`。后续新功能不要继续堆在 `backend-2` 上，改从 `dev` 切 `feature/*`。

### 6.2 第一批分支

成员 A：

```text
docs/module-ownership-two-person
feature/assignment-grading
feature/course-forum-minimal
feature/warning-minimal
fix/course-doc-drift
```

成员 B：

```text
docs/api-contract-ai
feature/ai-course-qa
feature/ai-grading-comment
feature/exam-paper-suggestion
fix/gateway-ai-sse-routing
chore/gitlab-ci-docker
```

共同或任选一人发起：

```text
docs/backend-two-person-plan
docs/adr-ai-boundary
docs/adr-flyway-versioning
docs/adr-ai-stack-spring-ai-qdrant
```

### 6.3 必须单独 PR 的改动

- 父 `backend/pom.xml` 和各模块 `pom.xml`
- `edu-common` 公共类型
- `edu-feign-api` 跨服务契约类型
- JWT claims、权限码、公共错误码
- Gateway 路由、安全过滤器、限流
- Flyway 历史脚本或基础表
- AI provider、向量库、SSE 事件协议
- GitLab CI/CD 或 GitHub/GitLab 镜像策略
- RabbitMQ exchange、queue、routing key、事件 schema
- Docker Compose、端口、Nacos 配置、环境变量名

### 6.4 PR 最低检查项

- [ ] 从最新 `dev` 创建或已 rebase/merge 最新 `dev`。
- [ ] 只修改本分支目标范围内的文件。
- [ ] API 契约、请求、响应、错误码已更新。
- [ ] 角色权限、资源归属、越权场景已测试。
- [ ] Flyway migration 不修改历史文件，空库和升级均可执行。
- [ ] `edu-common`、`edu-feign-api`、POM、Gateway、配置文件变更已单独说明影响面。
- [ ] AI 功能只返回建议/草稿/引用/任务状态，不直接写 Biz 正式表。
- [ ] 通过后端构建和相关集成测试。

### 6.5 提交信息规范

```text
feat(assignment): add submission publish flow
feat(ai): add course qa sse contract
fix(course): align course delivery docs
docs(db): define timestamp flyway versioning
test(auth): cover teacher resource scope
chore(build): align gateway dependencies
```

### 6.6 接口变更通知方式

接口变更必须同时满足：

1. 更新 `docs/*api*` 或 `docs/openapi/**`。
2. PR 描述写清楚破坏性变化、兼容策略、需要谁同步。
3. 在双人同步中明确口头确认一次。
4. 涉及对方模块时，先合契约 PR，再写实现。

### 6.7 必须暂停编码先同步的情况

- 想改对方 owned 表或 owned package。
- 需要修改 `edu-common`、`edu-feign-api`、JWT、权限框架、Gateway。
- 需要修改已共享的 Flyway 历史 migration。
- AI 想直接保存课程、成绩、评语、预警、试卷等正式业务数据。
- 考试成绩要进入 A 的成绩总表。
- 删除、重命名或改变已有接口字段含义。
- 同一个状态枚举在两个模块里出现不同含义。

## 7. 第一周任务清单

### 7.1 后端成员 A

- 将 `docs/module-ownership.md` 从多人 owner 调整为当前双人 owner，或新增双人 owner 附录。
- 修正课程相关文档与当前代码完成度的漂移。
- 写 `docs/api-contract-assignment.md`。
- 写或补 `docs/api-contract-forum.md`，论坛只做课程内帖子/回复/删除最小闭环。
- 设计作业、提交、评分、成绩统计、论坛、预警表。
- 明确作业状态、提交状态、成绩发布状态、预警状态。
- 实现前先补作业发布、提交、评分的测试样例。
- 和 B 确认 AI 评语草稿所需的提交 context。

### 7.2 后端成员 B

- 写 `docs/api-contract-ai.md`。
- 写 `docs/api-contract-exam.md` 或 `docs/api-contract-paper-suggestion.md`。
- 按默认选型准备 Spring AI + Qdrant 的配置草案；真实模型 key 缺失时使用 fake adapter 保证联调。
- 设计考试安排、题库、题目、试卷草稿、组卷建议的最小表。
- 验证 Gateway 到 AI 的路由、SSE 转发、AI 限流和错误响应。
- 明确 AI 引用结构、无来源响应、任务失败响应。
- 和 A 确认课程、作业、提交、预警 context 的内部接口。
- 准备 `.gitlab-ci.yml` 草案，至少包含 Maven verify、测试报告和 Docker 构建占位。

### 7.3 共同任务

- 确认 `dev` 是日常集成分支。
- 处理当前 `backend-2` 本地领先 1 个提交：推送或 PR。
- 确认后续 Flyway 统一使用时间戳版本。
- 确认 GitHub 远端是否需要镜像到 GitLab 以满足 GitLab CI/CD 交付要求。
- 新建或补充 `docs/adr/`，至少记录 AI 边界和 Flyway 版本规则。
- 统一端口、环境变量和本地启动命令。
- 每天结束前把正在改的高风险文件同步给对方。

## 8. 第一批需要完成的文档

| 文档 | 动作 | Owner |
|---|---|---|
| `docs/backend-two-person-collaboration-plan.md` | 新建，记录本文计划 | A 或当前执行者 |
| `docs/module-ownership.md` | 调整为双人 owner，或增加双人映射表 | A 主，B review |
| `docs/mvp-scope.md` | 新建 MVP 范围和延后功能 | A 主 |
| `docs/api-contract-assignment.md` | 新建作业/提交/评分/成绩接口契约 | A |
| `docs/api-contract-forum.md` | 新建课程论坛最小闭环接口契约 | A |
| `docs/api-contract-ai.md` | 新建 AI QA、摘要、评语、风险解释、组卷建议契约 | B |
| `docs/api-contract-exam.md` | 新建题库、试卷草稿、智能组卷契约 | B |
| `docs/adr/0001-ai-boundary.md` | AI 只能返回建议/草稿，Biz 写正式事实 | A+B |
| `docs/adr/0002-flyway-versioning.md` | 时间戳 migration 和历史脚本不可变 | A+B |
| `docs/adr/0003-ai-stack.md` | 默认 Spring AI + Qdrant，记录切换条件 | B 主，A review |
| `.gitlab-ci.yml` | GitLab CI/CD 交付物，包含 Maven verify 和 Docker 构建阶段 | B |
| `backend/README.md` | 补当前启动、验证、服务状态入口 | B 主，A review |

## 9. 当前不应开始做的功能

- 完整在线考试引擎。
- 自动批量阅卷。
- 复杂论坛治理。
- 全功能消息中心。
- AI 管理后台。
- 大规模向量索引管理后台。
- 多租户和复杂组织权限。
- 真实文件对象存储完整能力。
- 复杂统计大屏。
- Sentinel、Seata 等非要求的 Spring Cloud Alibaba 组件。
- 为了“看起来微服务化”继续拆更多服务。

## 10. 两个人第一次同步会议问题

1. 当前 `backend-2` 领先远端的 1 个提交是否已经可以推送？
2. 日常集成是否统一使用 `dev`？
3. `backend-1`、`backend-2` 是否只作为个人备份分支，不作为功能开发长期堆叠分支？
4. 成员 A/B 是否接受本文职责分工？
5. Flyway 是否从下一次开始统一使用时间戳版本？
6. 首版 AI 是否先用 fake adapter，真实模型作为可替换 provider？
7. 是否接受默认 `Spring AI + Qdrant`；如不接受，谁负责写 ADR 并承担切换成本？
8. 当前 GitHub 远端是否需要镜像到 GitLab，还是只准备 `.gitlab-ci.yml` 作为交付物？
9. 作业提交首版支持文本、附件元数据，还是要真实上传？
10. 课程论坛最小闭环做到“发帖/回复/列表”，还是加入教师/管理员删除？
11. 考试成绩首版是否进入成绩总表？
12. 学习预警首版规则包括哪些证据：进度、缺交、低分、活跃度？
13. 智能组卷首版是只返回建议，还是保存试卷草稿？
14. 谁负责每日合并公共 PR？
15. 谁负责最终演示数据和启动脚本？

## 11. 最小验收标准

阶段性验收命令建议：

```powershell
cd E:\my_projects\smart_education\backend
.\mvnw.cmd clean verify
```

第一版 MVP 的验收场景：

1. 管理员登录并审核课程。
2. 教师创建课程、章节、课时、作业。
3. 学生选课并完成至少一个课时。
4. 学生进入课程论坛发帖或回复，教师/管理员可以处理违规内容。
5. 学生进入课程 AI 答疑，收到回答、引用或无来源提示。
6. 学生提交作业。
7. 教师查看提交，调用 AI 评语草稿，人工确认并保存评分。
8. 学生查看成绩统计和学习风险。
9. 学生查看考试安排。
10. 教师基于题库查看智能组卷建议并保存草稿。
11. 无关教师、未选课学生、未登录用户的越权场景均返回正确错误。
12. 所有正式业务事实都由 `edu-biz-service` 写入，`edu-ai-service` 不直接写 Biz 表。
