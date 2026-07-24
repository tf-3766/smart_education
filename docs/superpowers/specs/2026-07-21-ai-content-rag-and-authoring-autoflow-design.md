# AI 后端升级设计：内容级 RAG + 教师/管理员自动流

日期：2026-07-21
状态：课程内容创作阶段记录；总体目标已由 `2026-07-22-role-aware-ai-assistant-platform-design.md` 取代

> 本文保留 2026-07-21 课程 RAG 与四类创作工具的实现决策，不再限定 AI 助手的最终范围。
> 原“平台治理写操作非目标”等阶段性边界不适用于最终产品目标；后续统一按 2026-07-22 角色型 AI 助手平台设计实施。

## 背景与问题

现有 AI 服务存在两处能力错配：

1. **只能按文件名回答课程资料问题。** RAG 本身已支持按内容索引（`CourseKnowledgeBaseService.documents()` 使用 `material.extractedText()`），但：
   - 通用助手入口 `assistantChat` **不做向量检索**，只调用只读工具，`listCourseMaterials` 仅返回资料名称与定位信息 → LLM 只能按文件名作答。
   - 只有 `courseQa`（带 courseId）走 RAG，且依赖手动点击「同步知识库」。
2. **教师/管理员无法让 AI 执行指令。** 现有 tools（`RoleScopedPlatformTools`、`CourseContextTools`）全部只读；AI→biz 内部 Feign（`/_internal/v1/ai-context`）也只有 5 个读端点。因此「按课程资料出一组题库并落库」这类自动流无从实现。

## 目标

- **内容级问答**：三种角色的 AI 助手都能基于上传资料的正文（而非文件名）回答。
- **教师/管理员自动流**：自然语言指令 → AI 自主编排 → 落库为草稿 → 前端可见并由教师确认后正式生效。

## 已确认的关键决策

| 决策点 | 结论 |
|---|---|
| 执行模型 | **草稿待确认**：AI 直接写库但标记为 `DRAFT` + `source=AI`，前端立即可见并高亮，教师一键确认后 `PUBLISHED` 生效。可回滚。 |
| 自动流范围 | **四条全覆盖**：题库生成、组卷/考试生成、作业生成、课时大纲/公告生成。统一写工具框架复用同一骨架。 |
| 按内容回答 | **RAG 当工具 `searchCourseKnowledge` + 上传发布即自动索引**，三角色通用。 |
| 管理员定位 | 在课程上下文内拥有与教师同等的内容创作自动流；平台治理类高风险写操作第一版仍只读。 |
| 编排方式 | **LLM 工具调用自主编排**（与现有 tools 机制一致），非固定流水线。 |
| 前端风格 | **必须复用现有前端组件与设计令牌**，不自创样式（见组件 5）。 |

## 架构总览

```
① 内容级 RAG                       ② 自动流(Agent)
   资料上传→抽取正文→                       教师自然语言指令
   自动增量索引 Qdrant                          │
        │                              LLM 工具自主编排
   searchCourseKnowledge      ┌───────────┼──────────────┐
    (三角色通用 @Tool)          searchCourse  LLM 结构化    经内部写API
                              Knowledge     生成题目      落 DRAFT 草稿
                                                            │
                                          前端高亮角标 → 教师确认 → PUBLISHED
```

## 组件 1 — 内容级 RAG

**1.1 检索当工具。** 新增 `searchCourseKnowledge(query)`，包装 `CourseKnowledgeBaseService.retrieve()`，作为 `@Tool` 注入三种角色的助手。LLM 判断需要资料正文时自动调用，返回正文片段 + 引用（`AiCitationVO`）。这统一了 `courseQa`（有 RAG）与 `assistantChat`（无 RAG）的能力割裂。

- 学生：限本人已选课程；教师：限负责课程；管理员：可访问。检索时以 `courseId`（+可选 `lessonId`）作过滤，权限在 `AuthorizedAiContextService` 已有校验基础上收敛。
- 现有 `listCourseMaterials`（只返文件名）保留兜底，不删除。

**1.2 上传即索引。** 教师上传并**发布**（`PUBLISHED`）资料、且 biz 完成正文抽取后，自动触发 AI 服务对该课程的增量索引，去掉「必须手动点同步」。

- 触发路径：biz 资料发布成功后，通过（新增或复用的）内部通道通知 AI 服务 `syncKnowledgeBase(courseId)`；失败仅记日志、不阻断资料发布。
- 手动同步端点 `/courses/{courseId}/knowledge-base/sync` 保留为兜底。

**验收**：上传含正文的 PDF/DOCX → 发布 → 不点手动同步，在通用助手里提问资料内具体知识点，回答命中正文并给出引用，而非文件名。

## 组件 2 — 自动流写框架

统一骨架 + 四个写工具，全部 `@Tool`，由 LLM 自主编排调用。每个工具内部统一四步：

1. **权限校验**：教师限本人负责课程 / 管理员在课程上下文内；越权直接拒绝。
2. **取材**：调用 `searchCourseKnowledge` 拉取相关资料正文与知识点。
3. **结构化生成**：LLM 按指令参数（题型/数量/难度/知识点等）生成结构化 JSON。
4. **落库草稿**：经组件 3 的内部写 API 落 `DRAFT` + `source=AI`。

| 工具 | 落库对象 | 状态标记 |
|---|---|---|
| `generateQuestionBank` | question_banks + questions(+options) | `DRAFT` + `source=AI` |
| `assembleExamPaper` | exam_papers + exams | `DRAFT` + `source=AI` |
| `generateAssignment` | assignments | `DRAFT` + `source=AI` |
| `draftLessonOutlineOrAnnouncement` | lesson 大纲 / announcement | `DRAFT` + `source=AI` |

工具返回一个「草稿引用」（业务类型 + 主键 + 深链），供助手在对话流里回卡片。

## 组件 3 — AI→biz 内部写 API（新增）

新增 `BizAiAuthoringFeignClient`，基路径 `/_internal/v1/ai-authoring`，端点：

- `POST /question-banks`
- `POST /exam-papers`
- `POST /assignments`
- `POST /announcements`（含课时大纲落点）

要求：

- biz 侧 `InternalAiAuthoringController` 复用现有领域 service 落 `DRAFT`，写入 `source=AI` 与 `createdByAiTraceId`（溯源）。
- **服务端二次校验**教师对目标课程的归属权，不信任 AI 传入的 courseId/教师身份。
- 复用现有内部调用鉴权机制（与 `/_internal/v1/ai-context` 同一套）。

## 组件 4 — 草稿标记（最小 schema 改动）

- 为 `question_banks`、`exams`（及 exam_papers 若独立表）、`assignments`、`announcements` 各加 `source VARCHAR(16) NOT NULL DEFAULT 'HUMAN'`（取值 `AI` / `HUMAN`），可选 `ai_trace_id VARCHAR(64)` 溯源。
- 状态尽量复用已有 `DRAFT`；实测题库 `QuestionBankStatus` 原本只有 `ACTIVE/ARCHIVED`，已补充 `DRAFT` 值（AI 草稿落 `DRAFT`，教师确认转 `ACTIVE`，`source=AI` 作溯源保留）。作业/考试已有 `DRAFT` 可直接复用。
- 改动同步进唯一初始化脚本 `backend/edu-biz-service/src/main/resources/db/online_education_bootstrap.sql`（按团队规范：任何表结构变更只改这一个脚本，并在空 MySQL 8 验证）；已有数据库需备份后按该脚本重建。

## 组件 5 — 前端（草稿高亮 + 一键确认，复用现有风格）

**硬约束：全部复用现有组件与设计令牌，不自创样式。**

- **角标**：`source=AI && status=DRAFT` 的题库/作业/考试列表项，复用 `StatusBadge.vue` 以 `tone="amber"` 显示「AI 草稿」标签。
- **草稿卡片 / 采用**：对话流内工具执行完，复用 `AiResultPanel.vue` 的 `ai-card` / `ai-chip` / 引用样式回卡片：「已生成《XX》题库草稿，含 N 题 · 点此查看/确认」，深链到对应教师页面。
- **确认发布**：详情页教师可先编辑，「确认发布」按钮走现有 publish 接口 → `PUBLISHED`；发布后清除或保留 `source=AI` 溯源（保留，仅去高亮）。
- 复用 `src/styles/index.css` 令牌（`--ink`/`--muted`/`--tag-*` 等），不新增全局样式变量。
- 前端只在 **real 模式** 下访问真实自动流接口；demo 模式保持既有演示数据行为。

## 角色能力矩阵

| 能力 | 学生 | 教师 | 管理员 |
|---|---|---|---|
| 内容问答（RAG） | ✅ 本人课程 | ✅ 负责课程 | ✅ |
| 四条写自动流 | ❌ | ✅ 本人课程 | ✅ 课程上下文内，等同教师 |
| 平台治理写操作 | ❌ | ❌ | ❌（第一版仍只读） |

## 测试策略

- **RAG**：抽取→自动索引→`searchCourseKnowledge` 命中正文（非文件名）单测；扩展 `tests/integration` AI 冒烟脚本覆盖「发布资料→内容问答」。
- **写框架**：每条自动流「指令→落 DRAFT 草稿→越权课程被拒」契约测试；biz 侧课程归属校验单测。
- **前端**：AI 草稿角标渲染 + 确认发布流 vitest；沿用 `vite.config` 中单测强制 demo 模式的约定。

## 实现顺序（分批，不缩小范围）

1. 内容级 RAG（`searchCourseKnowledge` 工具 + 上传即索引）。
2. 写框架骨架 + 组件 3 内部写 API + `generateQuestionBank` 端到端打通。
3. 其余三条写工具（组卷/作业/大纲公告）。
4. schema 标记 + 前端草稿角标与确认 UI。

## 非目标（YAGNI）

- 管理员平台治理类高风险写操作（教师审核、选课时间、课程下线）的 AI 代执行。
- 多轮"改一改再落库"的复杂协商流（第一版：生成即落 DRAFT，教师在前端编辑）。
- 跨课程/批量自动流。
