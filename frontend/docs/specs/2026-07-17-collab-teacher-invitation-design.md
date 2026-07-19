# 协作教师邀请/同意流程 — 设计

日期：2026-07-17　状态：已批准

## 背景与问题
课程"课程团队"里，负责人（OWNER）此前可以直接把任意教师拉进协作团队，一拉即入、
无对方同意，既随意又不安全。改为**邀请 → 被邀请人接受/拒绝**的双向确认流程。

## 数据模型
- `edu_course_teacher` 增加 `status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'`。
  - OWNER 行永远 `ACTIVE`。
  - 邀请协作教师 → 新增 `COLLABORATOR` 行，`status=PENDING`。
  - 接受 → `PENDING → ACTIVE`；拒绝 → 软删该行。
- 保留 `UNIQUE(course_id, teacher_id)`：同一教师对同一课程仅一条记录；重复邀请报冲突。

## 权限（关键正确性点）
所有"是否为课程团队成员/可编辑课程内容"的判定，一律加 `status = ACTIVE` 过滤：
`canEditCourseContent`、教师课程列表 `listForTeacher`、`requireEditor/requireOwner` 相关、
文件访问 `canAccessMaterial` 等。`PENDING` 不授予任何权限——被邀请人接受前：
看不到该课、不进其课程列表、不能编辑内容。

## 后端接口（教师端，需重编译进 jar）
- 改 `POST /api/v1/teacher/courses/{courseId}/teachers`：语义为"发出邀请"，建 `PENDING` 行。
- 新 `GET /api/v1/teacher/courses/collab-invitations`：我收到的待确认邀请（courseId、课程名、邀请人名、邀请时间）。
- 新 `POST /api/v1/teacher/courses/collab-invitations/{courseId}/accept`：`PENDING→ACTIVE`。
- 新 `POST /api/v1/teacher/courses/collab-invitations/{courseId}/reject`：软删本人的 PENDING 行。
- 改 `DELETE /api/v1/teacher/courses/{courseId}/teachers/{teacherId}`：owner 撤回 pending 或移除 active（OWNER 不可删，复用现逻辑）。
- `CourseTeacherVO` 增 `status`（CodeLabel：已加入/待确认）。

## 前端
- 课程管理页顶部：加载 `collab-invitations`，非空则显示"待我确认的协作邀请"卡片区，每条 `[接受] [拒绝]`。
- 团队弹窗：成员显示状态徽章（已加入/待确认）；owner 对 pending 显示"撤回邀请"、对 active 显示"移除"。
- 文案："添加" → "邀请"；成功提示 "邀请已发出，等待对方确认"。
- demo 模式同步：`db.courseTeachers` 行加 `status`；`addTeacher` 建 PENDING；新增 accept/reject/invitations demo 逻辑；demo 权限判定（`requireTeacherCourse` 等）只算 active。

## 通知
主入口即课程管理页顶部。站内通知推送为可选加分项：若现有 `NotificationApplicationService`
是低成本调用则顺手推一条给被邀请人，否则不阻塞主流程。

## YAGNI 取舍
不做邀请过期、不做邀请留言、拒绝不留审计（直接软删）、撤回=删 pending 行复用 removeTeacher。

## 测试
- 前端用例：邀请后对方课程列表无该课；team 面板显示"待确认"；接受后可见可编辑；拒绝后消失。
- 后端：改动重编译进运行中的 jar 后实测邀请/接受/拒绝三接口。
