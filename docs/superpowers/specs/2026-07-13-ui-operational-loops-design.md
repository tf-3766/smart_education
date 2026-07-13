# 前端操作闭环设计（朝实用方向优化）

日期：2026-07-13
状态：已获用户批准
前置：契约 API 层已与真实后端联调对齐（109 PASS，见 `tests/integration/`），旧 mock 层已移除（lms 仓库 `eaa1dea`）。

## 背景与目标

后端与前端契约层支持完整业务流，但页面只暴露了少部分操作（18 个页面合计约 800 行，
多数只有列表）。目标：让学生/教师/管理员三个角色都能在页面上走完真实业务闭环。

推进顺序（用户指定）：教师课程内容 → 作业全流程 → 考试全流程 → 管理员治理。

## 总原则

- 完全沿用现有约定：`AppButton`/`AppModal`/`AsyncState`/`StatusBadge` 组件、
  `usePageState` 加载与错误、`flash()` 操作提示、紧凑单行模板风格、`filter-bar`+`table` 布局。
- 数据只经 `@/services/api` 契约层（demo/real 双模式天然可用）；写操作携带乐观锁
  `version`，成功后刷新列表；`RESOURCE_CONFLICT` 由 AsyncState 展示后用户重试。
- 不做拖拽排序（`sortOrder` 数字输入）、不做富文本编辑器（textarea 存 Markdown/纯文本）。
- 每条链路：先写 jsdom 挂载测试（demo 模式驱动，先红后绿）→ 实现 → vitest 全绿 → 提交。
  真实模式回归复用 `tests/integration/harness.mjs`。

## 链路 1：教师课程内容

- `CourseManagePage` 表格行加「内容管理」链接。
- 新路由 `/teacher/courses/:courseId/content` → 新页面 `CourseContentPage.vue`。
- 章节面板：`courseContentApi.listChapters/createChapter/updateChapter/deleteChapter/
  publishChapter/offlineChapter`；行内编辑弹窗（标题/描述/排序值）。
- 课时面板（选中章节）：`listLessons/createLesson/updateLesson/deleteLesson/
  publishLesson/offlineLesson`；字段：标题、contentType（RICH_TEXT/VIDEO）、内容或
  videoUrl、时长、解锁方式（IMMEDIATE/SCHEDULED+时间）。
- 资料面板：`listMaterials/createMaterial/deleteMaterial`；新增支持外链 URL 或
  `filesApi.upload` 上传得 fileId。

## 链路 2：作业全流程

- `GradingWorkspacePage`：新建作业弹窗（标题/说明/满分/截止时间，
  `assignmentsApi.create`）+ `publish/close` 操作；每行「查看提交」展开
  `listSubmissions`：学生、状态、内容预览、打分+评语（`grade`，传 submission
  version）、发布成绩（`publishGrade`，传 `gradeVersion`）。
- `StudentAssignmentsPage`：详情弹窗（`studentDetail`）；文本作答
  `saveDraft`/`submit`；已批改行显示分数与评语（`studentGrades`）。

## 链路 3：考试全流程

- `QuestionBankPage` 重构为三段：
  1. 题库：`examsApi.listBanks/createBank`；
  2. 题目：`listQuestions/createQuestion/updateQuestion/deleteQuestion`，四种题型
     （SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/SHORT_ANSWER），选项编辑器校验
     正确项数量（单选/判断恰 1 个，多选 ≥1）；
  3. 考试：`listExams/createExam`（标题/窗口/时长/总分）、组卷 `createPaper`
     （从题库选题定分，校验合计=总分）、`publishPaper`、答卷列表 `listAttempts`
     + 简答阅卷 `gradeAttempt`。
- 学生端：`StudentExamsPage` 行内「进入考试」→ 新路由
  `/student/exams/:examId/attempt` → 新页面 `ExamAttemptPage.vue`：
  `startAttempt`（幂等续答）、按题型渲染作答控件、`submitAttempt`（传 version）、
  交卷后展示得分与判分状态。

## 链路 4：管理员治理

- `CourseReviewPage`：待审列表（`courseReviewsApi.list`）+ 详情 + 通过/驳回
  （驳回原因必填弹窗）+ 审核历史展示。
- `UserManagementPage`：教师待审批 `approveTeacher/rejectTeacher`、
  `grantAdministrator/revokeAdministrator`。
- `ContentGovernancePage`：公告管理（`announcementsApi.adminList/adminCreate/
  adminWithdraw`，受众 ALL/STUDENT/TEACHER）；论坛治理沿用既有可见性接口。

## 提交策略

仓库存在用户未提交 WIP（约 51 个文件）；每条链路提交时只 `git add` 本链路涉及
文件，文件内既有 WIP 随之入库（用户已知悉并同意）。
