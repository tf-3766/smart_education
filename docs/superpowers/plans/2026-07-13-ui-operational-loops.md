# 前端操作闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让学生/教师/管理员在页面上走完真实业务闭环（课程内容、作业、考试、治理），数据只经 `@/services/api` 契约层。

**Architecture:** 在现有 18 个轻页面上扩展/新增页面；沿用 AppButton/AppModal/AsyncState/StatusBadge + usePageState + flash 约定；每个写操作带乐观锁 version，成功后刷新。新增 2 个页面（教师课程内容、学生答题）+ 2 条路由，其余在既有页面内扩展。

**Tech Stack:** Vue 3 `<script setup>` + vue-router + 契约 API 层（demo/real 双模）+ vitest/jsdom/@vue/test-utils。

## Global Constraints

- 页面只允许 import `@/services/api`（boundary 测试强制）；demo/real 行为一致。
- 写操作失败由 `state.run` 捕获 → AsyncState 展示 RuntimeError（含 traceId）。
- 不做拖拽排序、不做富文本编辑器；sortOrder 数字输入、内容用 textarea。
- 每个任务：失败测试 → 实现 → `npx vitest run` 全绿 → 只 add 本任务文件并 commit（lms 仓库）。
- demo 种子锚点：课程 21001（教师 '2'）、章节 22001、课时 23001、作业 31001/31002、题库 51001、题目 52001-52004、考试 53001、学生 '4'。

---

### Task 0: 测试基建

**Files:**
- Modify: `package.json`（devDeps + @vue/test-utils）
- Create: `src/tests/pageTestUtils.ts`
- Test: 复用后续任务测试

**Interfaces:**
- Produces: `mountPage(component, { path, route })` → `{ wrapper, router }`；`settle(ms=200)` 等待 demoDelay；`freshDemo()` 调 `resetDemoData()` 并清 localStorage session。

- [ ] Step 1: `CI=true pnpm add -D @vue/test-utils`（在 smart-education-frontend 目录）
- [ ] Step 2: 写 `pageTestUtils.ts`：createRouter(createMemoryHistory) 注入被测路由 + catch-all；mount 全局 plugins=[router]；`settle` = flushPromises + setTimeout 轮询。
- [ ] Step 3: 冒烟：用它挂载现有 `CourseManagePage`，断言渲染出「Python 程序设计」（demo 种子）。先跑失败（文件未建）再通过。
- [ ] Step 4: Commit `test(frontend): 页面挂载测试基建`

### Task 1: 教师课程内容页——章节面板

**Files:**
- Create: `src/domains/teacher/CourseContentPage.vue`
- Modify: `src/router/index.ts`（`/teacher/courses/:courseId/content`，meta.title 内容管理）
- Modify: `src/domains/teacher/CourseManagePage.vue`（行内「内容管理」router-link）
- Test: `src/tests/courseContentPage.test.ts`

**Interfaces:**
- Consumes: `courseContentApi.listChapters/createChapter/updateChapter/deleteChapter/publishChapter/offlineChapter`；`teacherCoursesApi.getDetail`
- Produces: 页面内 `selectedChapterId` ref（Task 2 课时面板挂其下）；`reloadChapters()`。

- [ ] Step 1: 失败测试：挂载 `/teacher/courses/21001/content`，断言渲染章节 22001 标题「第一章 Python 基础语法」；点击「新建章节」填表提交 → 列表出现新章节；对 22001 点「下线」→ StatusBadge 变草稿/下线态。
- [ ] Step 2: 实现页面：页头（课程名 via getDetail + 返回链接）；章节表格（标题/排序/状态/课时数/操作：编辑、发布|下线、删除）；新建/编辑共用 AppModal（title/description/sortOrder/version 隐藏携带）；删除 confirm()。
- [ ] Step 3: vitest 绿 + 全量绿。
- [ ] Step 4: Commit `feat(frontend): 教师课程内容页-章节管理`

### Task 2: 课时面板

**Files:** Modify `CourseContentPage.vue`；Test 追加 `courseContentPage.test.ts`

**Interfaces:** Consumes `courseContentApi.listLessons/createLesson/updateLesson/deleteLesson/publishLesson/offlineLesson`

- [ ] Step 1: 失败测试：选中章节 22001 → 渲染课时 23001「变量与数据类型」；新建课时（RICH_TEXT+内容）→ 出现且可发布。
- [ ] Step 2: 实现：章节行点击选中；课时面板表格 + 弹窗表单（title/contentType select RICH_TEXT|VIDEO/content textarea|videoUrl/estimatedMinutes/sortOrder/unlockType select IMMEDIATE|SCHEDULED + unlockAt datetime-local）。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 课程内容页-课时管理`

### Task 3: 资料面板

**Files:** Modify `CourseContentPage.vue`；Test 追加

**Interfaces:** Consumes `courseContentApi.listMaterials/createMaterial/deleteMaterial`、`filesApi.upload`、`fileContentUrl`

- [ ] Step 1: 失败测试：资料面板渲染 demo 资料；新增外链资料（name/materialType/fileUrl/visibility COURSE）→ 出现；删除 → 消失。
- [ ] Step 2: 实现：表格（名称/类型/可见性/操作删除）+ 弹窗：来源二选一（外链 URL 输入 或 `<input type=file>` → filesApi.upload('COURSE_MATERIAL') 取 fileId）。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 课程内容页-资料管理`

### Task 4: 教师作业创建/发布/截止

**Files:** Modify `src/domains/teacher/GradingWorkspacePage.vue`；Test: `src/tests/gradingWorkspace.test.ts`

**Interfaces:** Consumes `assignmentsApi.teacherList/create/publish/close`；课程下拉 `teacherCoursesApi.list`

- [ ] Step 1: 失败测试：页面渲染 demo 作业 31001；「新建作业」（标题/说明/满分/截止 datetime-local）→ 列表出现 DRAFT；点发布 → PUBLISHED。
- [ ] Step 2: 实现：课程选择器 + 作业表格（标题/状态/截止/提交数/操作）+ 新建弹窗 + publish/close 行内操作。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 作业创建与发布`

### Task 5: 提交列表+批改+发布成绩

**Files:** Modify `GradingWorkspacePage.vue`；Test 追加

**Interfaces:** Consumes `assignmentsApi.listSubmissions/grade/publishGrade`（grade 传 submission.version；publishGrade 传 `gradeVersion`）

- [ ] Step 1: 失败测试：作业 31001 点「查看提交」→ 渲染提交 demo 行；打分 92+评语提交 → gradeStatus DRAFT；发布成绩 → PUBLISHED。
- [ ] Step 2: 实现：行展开或弹窗内提交表格（学生/状态/提交时间/内容预览/分数）；批改弹窗（score number/maxScore 只读/teacherComment textarea）；发布按钮用返回的 gradeId+gradeVersion。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 提交批改与成绩发布`

### Task 6: 学生作业提交与成绩

**Files:** Modify `src/domains/student/StudentAssignmentsPage.vue`；Test: `src/tests/studentAssignments.test.ts`

**Interfaces:** Consumes `assignmentsApi.studentList/studentDetail/saveDraft/submit/studentGrades`

- [ ] Step 1: 失败测试：详情弹窗展示说明；textarea 填内容「已完成」→ 提交 → 状态 SUBMITTED；成绩区渲染 studentGrades。
- [ ] Step 2: 实现：行内「查看/作答」弹窗：说明+附件列表+textarea+存草稿/提交按钮（提交后禁用）；页面底部成绩面板。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 学生作业作答与成绩查看`

### Task 7: 题库与题目管理

**Files:** Modify `src/domains/teacher/QuestionBankPage.vue`；Test: `src/tests/questionBank.test.ts`

**Interfaces:** Consumes `examsApi.listBanks/createBank/listQuestions/createQuestion/updateQuestion/deleteQuestion`
- Produces: 页面 tabs 结构（banks|questions|exams），Task 8 在 exams tab 扩展。

- [ ] Step 1: 失败测试：渲染题库 51001；建库；选库后渲染题目 52001；新建单选题（2 选项 B 正确）→ 出现；单选勾 2 个正确项时提交按钮禁用。
- [ ] Step 2: 实现：题库列表+建库弹窗；题目表格（题干/题型/难度/分值/操作）；题目弹窗：题型 select、题干、难度、分值、选项编辑器（label 自动 A-F、content、correct checkbox/radio、增删行）、简答无选项；前端校验正确项数量。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 题库与题目管理`

### Task 8: 考试创建、组卷与发布

**Files:** Modify `QuestionBankPage.vue`；Test 追加

**Interfaces:** Consumes `examsApi.listExams/createExam/getPaper?/createPaper/publishPaper/getExam`

- [ ] Step 1: 失败测试：exams tab 渲染 53001；创建考试（窗口/时长/总分）；组卷弹窗从题库选 52001 定 5 分 → createPaper；发布试卷 → 考试状态 PUBLISHED。
- [ ] Step 2: 实现：考试表格（标题/窗口/时长/总分/状态/操作组卷|发布）；组卷弹窗：题目多选表 + 每题分值输入 + 合计校验=totalScore。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 考试创建与组卷发布`

### Task 9: 答卷列表与简答阅卷

**Files:** Modify `QuestionBankPage.vue`；Test 追加

**Interfaces:** Consumes `examsApi.listAttempts/getAttempt/gradeAttempt`（answers: [{questionId, score}], version）

- [ ] Step 1: 失败测试：demo 考试 53001 有已交答卷 → 渲染列表；打开阅卷弹窗给简答题打分 → 状态 GRADED、总分合计。
- [ ] Step 2: 实现：考试行「答卷」→ 答卷表（学生/状态/得分/交卷时间）；阅卷弹窗按题渲染答案，简答题分值输入，客观题只读。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 答卷阅卷`

### Task 10: 学生在线答题页

**Files:**
- Create: `src/domains/student/ExamAttemptPage.vue`
- Modify: `src/router/index.ts`（`/student/exams/:examId/attempt`）、`StudentExamsPage.vue`（「进入考试」链接，仅窗口内可点）
- Test: `src/tests/examAttempt.test.ts`

**Interfaces:** Consumes `examsApi.startAttempt/getAttempt/submitAttempt`、`studentExams`

- [ ] Step 1: 失败测试：挂载 attempt 路由（demo 考试 53001 窗口内）→ startAttempt 渲染题目；单选选 B、判断选、简答填文本 → 交卷 → 展示得分与状态。
- [ ] Step 2: 实现：顶部考试信息+剩余时长（durationMinutes 静态展示即可，不做倒计时强制）；题目按 questionOrder 渲染（radio/checkbox/textarea）；answers 组装 `{questionId, answerContent}`（多选逗号拼 label）；交卷 confirm + 传 attempt.version；已交卷状态直接展示结果。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 学生在线答题`

### Task 11: 管理员课程审核

**Files:** Modify `src/domains/admin/CourseReviewPage.vue`；Test: `src/tests/adminGovernance.test.ts`

**Interfaces:** Consumes `courseReviewsApi.list/detail/approve/reject`（reject.reason 必填）

- [ ] Step 1: 失败测试：待审课程行「驳回」原因空时确认按钮禁用；填原因驳回 → 状态 REJECTED；「通过」→ APPROVED；详情弹窗渲染审核历史。
- [ ] Step 2: 实现：状态筛选（PENDING 默认）+ 操作弹窗（通过备注可选/驳回原因必填）+ 历史时间线。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 课程审核操作闭环`

### Task 12: 管理员用户管理

**Files:** Modify `src/domains/admin/UserManagementPage.vue`；Test 追加 adminGovernance.test.ts

**Interfaces:** Consumes `adminUsersApi.list/approveTeacher/rejectTeacher/grantAdministrator/revokeAdministrator`

- [ ] Step 1: 失败测试：demo 待审教师（user '7' 高翔 PENDING）行「通过」→ ENABLED；对启用教师「授予管理员」→ roles 含 ADMIN。
- [ ] Step 2: 实现：角色/状态筛选 + 行内操作按钮（按状态/角色条件显隐）。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 用户管理操作`

### Task 13: 管理员公告管理

**Files:** Modify `src/domains/admin/ContentGovernancePage.vue`；Test 追加

**Interfaces:** Consumes `announcementsApi.adminList/adminCreate/adminWithdraw`

- [ ] Step 1: 失败测试：发布 TEACHER 公告 → 列表出现；撤回 → 状态 WITHDRAWN/消失。
- [ ] Step 2: 实现：公告面板（标题/受众/状态/时间/撤回操作）+ 发布弹窗（标题/内容/受众 select）。
- [ ] Step 3: 绿。Step 4: Commit `feat(frontend): 公告管理`

### 收尾

- [ ] 全量 vitest + vue-tsc 应用区 0 错误
- [ ] 真实后端冒烟：`node tests/integration/harness.mjs`（workspace 仓库）确认契约未回归
- [ ] 更新计划勾选状态并提交计划文档
