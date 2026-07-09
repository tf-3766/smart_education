# 在线教育辅助教学系统 Sitemap 与路由规划

> 基于需求说明书、[`ui-spec.md`](./ui-spec.md) 与 [`docs/api-contract-platform.md`](./docs/api-contract-platform.md) 的页面、权限和接口边界继续细化。  
> 当前阶段只定义页面、导航、路由、权限与跳转关系，不包含 Vue 页面、组件或业务代码。

## 1. 规划结论

系统采用 **公共层 + 学生端 + 教师端 + 管理员端** 四个路由域：

- 公共层只承载登录、找回密码、错误页，以及可选的公开课程目录/公开公告。
- 三类角色分别使用 `/student`、`/teacher`、`/admin` 前缀，避免菜单、面包屑和权限混淆。
- “课程”是核心业务容器；作业、考试、成绩、论坛和公告提供跨课程聚合页，但不建立重复数据页面。
- 详情页、编辑页和工作台子页不占用一级菜单，通过列表、卡片或上下文导航进入。
- AI 主要嵌入章节学习、课时编辑、批改、预警和组卷流程；完整 AI 对话页只是补充入口，不是 AI 功能的唯一入口。

首版不追求把每个数据对象都拆成独立页面。能在一个稳定工作台内完成的筛选、查看、编辑和状态切换，优先留在同一页面中。

---

## 2. 标记说明

```text
[一级]  显示在角色主侧栏
[二级]  显示在一级菜单展开项、页面页签或角色工作台内
[详情]  不显示在主菜单，由列表、卡片或业务操作进入
[AI]    AI 功能的明确业务入口或 AI 增强区域
[公共]  无须登录即可访问
[可选公开]  仅在学校决定对外展示时开放；默认可关闭
```

---

## 3. 系统整体导航层级图

```text
在线教育辅助教学系统
├─ [公共] 身份与基础页面
│  ├─ 登录                                      /login
│  ├─ 找回密码                                  /forgot-password
│  ├─ [可选公开] 课程目录                       /catalog/courses
│  │  └─ [详情] 公开课程简介                    /catalog/courses/:courseId
│  ├─ [可选公开] 公告                           /announcements
│  │  └─ [详情] 公告详情                        /announcements/:announcementId
│  ├─ 无权限                                    /403
│  └─ 页面不存在                                /404
│
├─ [登录后公共] 账户区域
│  ├─ 消息中心                                  /notifications
│  └─ 个人中心                                  /profile
│
├─ 学生端                                       /student
│  ├─ [一级] 学习首页                           /student/dashboard
│  │  ├─ 今日待办、继续学习、考试提醒
│  │  ├─ [AI] 学习风险提醒与改进建议
│  │  └─ 最新公告
│  ├─ [一级] 我的课程                           /student/courses
│  │  ├─ [二级] 在学 / 可选 / 未开始 / 已结束（页内标签）
│  │  ├─ [详情] 课程详情                        /student/courses/:courseId
│  │  │  ├─ 课程概览、章节目录、近期任务
│  │  │  ├─ 课程作业 → /student/assignments?courseId=:courseId
│  │  │  ├─ 课程考试 → /student/exams?courseId=:courseId
│  │  │  ├─ 课程成绩 → /student/grades/courses/:courseId
│  │  │  ├─ 课程论坛 → /student/forums?courseId=:courseId
│  │  │  └─ 课程公告 → /student/announcements?courseId=:courseId
│  │  └─ [详情][AI] 章节学习                    /student/courses/:courseId/lessons/:lessonId
│  │     ├─ 正文 / 视频 / 随堂练习 / 笔记
│  │     ├─ 已发布章节知识点摘要
│  │     └─ 当前章节智能答疑侧栏
│  ├─ [一级] 学习任务
│  │  ├─ [二级] 我的作业                        /student/assignments
│  │  │  └─ [详情] 作业详情、提交与反馈          /student/assignments/:assignmentId
│  │  └─ [二级] 我的考试                        /student/exams
│  │     ├─ [详情] 考试说明                      /student/exams/:examId
│  │     ├─ [详情] 在线考试                      /student/exams/:examId/session
│  │     └─ [详情] 考试结果                      /student/exams/:examId/result
│  ├─ [一级] 成绩与进度
│  │  ├─ [二级] 成绩总览                        /student/grades
│  │  │  └─ [详情] 课程成绩详情                  /student/grades/courses/:courseId
│  │  └─ [二级][AI] 学习进度与预警               /student/progress
│  │     └─ [详情][AI] 预警详情与改进计划         /student/warnings/:warningId
│  ├─ [一级] 互动交流
│  │  ├─ [二级] 课程论坛                        /student/forums
│  │  │  └─ [详情] 主题详情与回复                /student/courses/:courseId/forum/topics/:topicId
│  │  └─ [二级] 公告中心                        /student/announcements
│  │     └─ [详情] 公告详情                      /student/announcements/:announcementId
│  └─ [一级][AI] AI 学习助手                    /student/ai-assistant
│     └─ 必须由课程、章节或已选上下文进入；保留历史会话与来源
│
├─ 教师端                                       /teacher
│  ├─ [一级] 教学工作台                         /teacher/dashboard
│  │  ├─ 待发布、待批改、待处理预警
│  │  └─ 负责课程运行摘要
│  ├─ [一级] 课程管理                           /teacher/courses
│  │  └─ [详情] 课程工作台                      /teacher/courses/:courseId
│  │     ├─ [二级] 章节与内容                   /teacher/courses/:courseId/content
│  │     │  └─ [详情][AI] 课时编辑              /teacher/courses/:courseId/lessons/:lessonId/edit
│  │     │     └─ 章节知识点摘要草稿生成与人工发布
│  │     ├─ [二级] 学生名单                     /teacher/courses/:courseId/students
│  │     ├─ [二级] 成绩册                       /teacher/courses/:courseId/gradebook
│  │     └─ [二级] 课程学情                     /teacher/courses/:courseId/progress
│  ├─ [一级] 作业与批改                         /teacher/assignments
│  │  ├─ [详情] 新建作业                        /teacher/courses/:courseId/assignments/new
│  │  ├─ [详情] 编辑作业                        /teacher/assignments/:assignmentId/edit
│  │  ├─ [详情] 提交情况                        /teacher/assignments/:assignmentId/submissions
│  │  └─ [详情][AI] 批改工作台                  /teacher/assignments/:assignmentId/grading/:submissionId
│  │     └─ AI 评语草稿、教师编辑、确认发布
│  ├─ [一级] 考试与题库                         /teacher/exams
│  │  ├─ [二级] 题库                            /teacher/question-bank
│  │  ├─ [详情] 新建考试                        /teacher/courses/:courseId/exams/new
│  │  ├─ [详情] 编辑考试                        /teacher/exams/:examId/edit
│  │  ├─ [详情][AI] 试卷编排                    /teacher/exams/:examId/paper
│  │  │  └─ 知识点、难度、题型分布与智能组卷建议
│  │  └─ [详情] 考试记录与结果                  /teacher/exams/:examId/records
│  ├─ [一级] 学情与预警
│  │  ├─ [二级] 学习预警                        /teacher/warnings
│  │  └─ [详情][AI] 预警详情                    /teacher/warnings/:warningId
│  └─ [一级] 课程互动
│     ├─ [二级] 课程公告                        /teacher/announcements
│     └─ [二级] 论坛管理                        /teacher/forums
│
└─ 管理员端                                     /admin
   ├─ [一级] 数据看板                           /admin/dashboard
   ├─ [一级] 用户管理                           /admin/users
   │  └─ [详情] 用户详情                        /admin/users/:userId
   ├─ [一级] 课程治理
   │  ├─ [二级] 课程分类                        /admin/course-categories
   │  ├─ [二级] 课程管理                        /admin/courses
   │  │  └─ [详情] 课程详情                     /admin/courses/:courseId
   │  └─ [二级] 课程审核                        /admin/course-reviews
   │     └─ [详情] 审核详情                     /admin/course-reviews/:reviewId
   ├─ [一级] 内容治理
   │  ├─ [二级] 公告管理                        /admin/announcements
   │  │  ├─ [详情] 新建公告                     /admin/announcements/new
   │  │  └─ [详情] 编辑公告                     /admin/announcements/:announcementId/edit
   │  └─ [二级] 论坛治理                        /admin/forum-moderation
   │     └─ [详情] 帖子处置                     /admin/forum-moderation/posts/:postId
   ├─ [一级] 数据统计                           /admin/statistics
   ├─ [一级][AI] AI 管理                        /admin/ai-management
   └─ [一级] 系统设置                           /admin/settings
```

### 3.1 菜单收敛说明

- 页面清单统计的是可直接访问和鉴权的路由记录，不等于必须设计同样数量的独立视觉模板。新建/编辑可复用同一表单骨架，学生/教师/管理员的公告详情可复用同一内容详情骨架，差异由权限和操作区决定。
- 学生“课程作业、课程考试、课程成绩”不再各建一套课程内列表，而是跳到对应聚合页并带 `courseId` 筛选，避免重复页面。
- 教师的作业、考试和互动采用跨课程聚合页；从课程工作台进入时同样携带 `courseId` 筛选。
- 章节知识点摘要不建立学生侧独立页面，直接出现在章节学习页；教师在课时编辑页生成、编辑和发布。
- AI 评语不建立独立“AI 评语管理页”，只在真实批改工作台中出现。
- 智能组卷不建立脱离考试的聊天页，固定进入具体考试的试卷编排页。
- 学习预警同时出现在学生首页/进度页和教师预警列表，但二者展示范围与可操作权限不同。

---

## 4. 推荐的路由规划

### 4.1 路由命名规则

| 规则 | 约定 | 示例 |
|---|---|---|
| 角色前缀 | 角色业务路由必须以 `/student`、`/teacher`、`/admin` 开头 | `/teacher/assignments` |
| 资源名 | 使用小写英文复数名词，使用连字符连接复合词 | `/admin/course-categories` |
| 动态参数 | 使用可读的 camelCase 参数名，不使用通用 `:id` | `:courseId`、`:assignmentId` |
| 详情页 | 资源列表后直接追加资源 ID | `/student/assignments/:assignmentId` |
| 新建/编辑 | 仅表单动作允许使用 `/new`、`/edit` | `/admin/announcements/new` |
| 路由名称 | 使用“角色 + 资源 + 页面类型”的 PascalCase | `TeacherAssignmentGrading` |
| 筛选上下文 | 不复制列表路由，用查询参数携带筛选 | `/teacher/assignments?courseId=123` |
| 菜单状态 | 详情页通过 `activeMenu` 归属到所属一级菜单 | 批改页仍高亮“作业与批改” |

不建议使用：

- `/page1`、`/manageData`、`/teacher-center/list` 等含义不稳定的路径。
- 同一资源同时出现 `/course`、`/courses`、`/courseManage` 多套命名。
- 把新增、查看、编辑都塞进 `/detail?type=...`。
- 将用户角色只放在查询参数中，例如 `/dashboard?role=teacher`。

### 4.2 路由根节点与默认跳转

| 进入路径 | 建议跳转 |
|---|---|
| `/` | 未登录跳 `/login`；已登录按当前有效角色跳对应 dashboard |
| `/student` | `/student/dashboard` |
| `/teacher` | `/teacher/dashboard` |
| `/admin` | `/admin/dashboard` |
| 已登录访问 `/login` | 跳当前角色 dashboard |
| 角色不匹配 | 跳 `/403`，不自动切换角色或循环重定向 |
| 资源不存在 | `/404` 或资源级空状态；由后端 404 结果决定 |

同一账号拥有多个角色时，先选择/切换“当前角色”，再进入对应路由域；角色切换后不保留上一角色的深层路径。

### 4.3 Vue 3 前后端分离适配原则

- 前端路由只负责页面访问控制和用户体验，后端接口必须再次校验角色、资源归属和数据范围。
- 页面刷新时先恢复登录态和当前角色，再挂载受保护路由，避免菜单短暂越权显示。
- 列表筛选、分页和排序进入 URL 查询参数，便于刷新、回退和分享同一工作视图。
- 新建页保存成功后跳到对应编辑页或详情页；不要让刷新导致重复提交。
- 详情页直接访问时必须能独立获取数据，不依赖从列表页传递的临时内存对象。
- 课程、作业、考试等 ID 采用后端稳定 ID；页面标题和名称不能作为路由参数。
- 在线考试页应使用独立页面框架，减少误触导航，但仍属于 `/student/exams/:examId/session` 权限域。

---

## 5. 公共与共享页面清单

| 页面名称 | 建议路由名称 | 路由建议 | 所属角色 | 页面用途 | 主要功能 | 模块类型 | 权限控制 |
|---|---|---|---|---|---|---|---|
| 登录 | `Login` | `/login` | 公共 | 建立登录态并确定可用角色 | 账号密码登录、错误提示、跳转找回密码 | 公共基础 | 无需登录；已登录用户不应重复进入 |
| 找回密码 | `ForgotPassword` | `/forgot-password` | 公共 | 发起密码重置 | 身份验证、发送重置入口、结果提示 | 公共基础 | 无需登录；接口需防刷 |
| 公开课程目录（可选） | `PublicCourseCatalog` | `/catalog/courses` | 公共 | 对外展示允许公开的课程 | 搜索、分类筛选、查看简介；不允许选课与学习 | 传统业务 | 可公开；由管理员控制课程可见性 |
| 公开课程简介（可选） | `PublicCourseDetail` | `/catalog/courses/:courseId` | 公共 | 展示公开课程介绍 | 课程简介、教师、章节摘要；登录后才能选课 | 传统业务 | 可公开；仅公开字段 |
| 公开公告（可选） | `PublicAnnouncementList` | `/announcements` | 公共 | 展示校级公开公告 | 公告列表、搜索、详情入口 | 传统业务 | 可公开；仅已发布且允许公开的公告 |
| 消息中心 | `NotificationCenter` | `/notifications` | 学生/教师/管理员 | 聚合当前用户通知 | 未读筛选、课程/作业/成绩通知、标记已读、业务跳转 | 传统业务 | 需要登录；仅本人消息 |
| 个人中心 | `Profile` | `/profile` | 学生/教师/管理员 | 管理个人资料与安全设置 | 基本资料、账号安全、通知偏好 | 公共基础 | 需要登录；仅本人信息 |
| 无权限 | `Forbidden` | `/403` | 公共技术页 | 告知角色或资源权限不足 | 返回可访问首页、显示最小必要原因 | 公共基础 | 无需额外权限 |
| 页面不存在 | `NotFound` | `/404` | 公共技术页 | 处理未知路径 | 返回首页、重新导航 | 公共基础 | 无需额外权限 |

默认建议只有登录、找回密码和错误页公开；公开课程与公开公告在学校明确提出对外展示需求后再启用。

---

## 6. 学生端页面清单

| 页面名称 | 建议路由名称 | 路由建议 | 所属角色 | 页面用途 | 主要功能 | 模块类型 | 权限控制 |
|---|---|---|---|---|---|---|---|
| 学习首页 | `StudentDashboard` | `/student/dashboard` | 学生 | 汇总下一步学习行动 | 今日待办、继续学习、考试提醒、公告、风险提示、业务快捷入口 | 传统 + AI 嵌入 | 登录 + `student`；仅本人数据 |
| 我的课程 | `StudentCourseList` | `/student/courses` | 学生 | 统一承载在学课程与选课 | 在学/可选/未开始/已结束标签、搜索、课程筛选、选课/退选规则提示 | 传统业务 | 登录 + `student`；选课动作校验开放范围与时间 |
| 课程详情 | `StudentCourseDetail` | `/student/courses/:courseId` | 学生 | 查看课程概览并进入具体学习活动 | 课程信息、进度、章节目录、近期作业/考试、公告、论坛入口 | 传统业务 | 登录 + `student`；已选课程可学习，可选课程仅看允许字段 |
| 章节学习 | `StudentLesson` | `/student/courses/:courseId/lessons/:lessonId` | 学生 | 完成课时学习 | 章节树、学习内容、完成状态、笔记、已发布摘要、AI 答疑侧栏 | 传统 + AI 嵌入 | 登录 + `student`；必须有课程学习权限，受章节发布/解锁状态限制 |
| 我的作业 | `StudentAssignmentList` | `/student/assignments` | 学生 | 跨课程查看作业任务 | 状态/课程/截止时间筛选、待完成优先、查看反馈入口 | 传统业务 | 登录 + `student`；仅本人相关作业 |
| 作业详情与提交 | `StudentAssignmentDetail` | `/student/assignments/:assignmentId` | 学生 | 查看要求、提交作业和接收反馈 | 作业说明、附件、保存草稿、正式提交、重交、评分与评语 | 传统业务 | 登录 + `student`；仅本人提交记录，受截止时间和重交规则控制 |
| 我的考试 | `StudentExamList` | `/student/exams` | 学生 | 跨课程查看考试安排与记录 | 待考试/进行中/已结束筛选、时间、状态、结果入口 | 传统业务 | 登录 + `student`；仅本人可参加的考试 |
| 考试说明 | `StudentExamDetail` | `/student/exams/:examId` | 学生 | 考前确认考试规则和入口状态 | 开考时间、时长、规则、次数、设备提示、进入考试 | 传统业务 | 登录 + `student`；校验报名/选课、时间窗和考试状态 |
| 在线考试 | `StudentExamSession` | `/student/exams/:examId/session` | 学生 | 完成在线答题 | 题目导航、答题、保存、倒计时、交卷、断线恢复提示 | 传统业务 | 登录 + `student`；严格校验本人考试会话、时间窗与重复进入 |
| 考试结果 | `StudentExamResult` | `/student/exams/:examId/result` | 学生 | 查看允许公开的考试结果 | 总分、题型得分、教师反馈、答案解析（按发布策略） | 传统业务 | 登录 + `student`；仅本人且成绩已发布 |
| 成绩总览 | `StudentGradeOverview` | `/student/grades` | 学生 | 跨课程查看成绩结论 | 学期筛选、课程成绩、成绩趋势、未发布状态、进入明细 | 传统业务 | 登录 + `student`；仅本人已授权成绩 |
| 课程成绩详情 | `StudentCourseGrade` | `/student/grades/courses/:courseId` | 学生 | 理解单门课程成绩构成 | 作业/考试/平时权重、逐项得分、评语、及格线、发布状态 | 传统业务 | 登录 + `student`；仅本人且已选课程 |
| 学习进度与预警 | `StudentProgress` | `/student/progress` | 学生 | 查看进度、风险和改进行动 | 课程进度、近期活跃、缺交/低分提醒、预警列表、建议动作 | 传统 + AI 嵌入 | 登录 + `student`；仅本人数据；AI 建议不可替代正式成绩 |
| 预警详情 | `StudentWarningDetail` | `/student/warnings/:warningId` | 学生 | 理解某条预警的原因与改进计划 | 触发依据、相关任务、风险解释、建议计划、已读/完成状态 | 传统 + AI 嵌入 | 登录 + `student`；仅本人预警；隐藏不应公开的模型内部信息 |
| 课程论坛 | `StudentForumList` | `/student/forums` | 学生 | 跨课程查看并参与讨论 | 课程筛选、帖子列表、发帖、我的帖子/回复 | 传统业务 | 登录 + `student`；仅已选课程论坛，遵守内容状态 |
| 主题详情 | `StudentForumTopic` | `/student/courses/:courseId/forum/topics/:topicId` | 学生 | 阅读主题并回复 | 主题、回复、编辑/删除本人内容、举报 | 传统业务 | 登录 + `student`；课程成员可见；只能管理本人内容 |
| 公告中心 | `StudentAnnouncementList` | `/student/announcements` | 学生 | 查看校级和课程公告 | 课程筛选、未读、置顶、详情、业务链接 | 传统业务 | 登录 + `student`；仅目标范围包含该学生的公告 |
| 公告详情 | `StudentAnnouncementDetail` | `/student/announcements/:announcementId` | 学生 | 阅读完整公告 | 正文、附件、发布时间、相关课程跳转 | 传统业务 | 登录 + `student`；校验公告受众与发布状态 |
| AI 学习助手 | `StudentAiAssistant` | `/student/ai-assistant` | 学生 | 在完整视图中延续课程答疑 | 会话历史、课程/章节上下文、来源、提示式问答、生成练习 | AI 模块 | 登录 + `student`；只能引用本人有权访问的课程材料；必须携带或选择上下文 |

### 6.1 学生端一级菜单建议

1. 学习首页
2. 我的课程
3. 学习任务（含作业、考试）
4. 成绩与进度
5. 互动交流（含论坛、公告）
6. AI 学习助手

学生端不单独增加“选课管理”一级菜单；选课是“我的课程”里的一个标签和业务状态，减少导航膨胀。

---

## 7. 教师端页面清单

| 页面名称 | 建议路由名称 | 路由建议 | 所属角色 | 页面用途 | 主要功能 | 模块类型 | 权限控制 |
|---|---|---|---|---|---|---|---|
| 教学工作台 | `TeacherDashboard` | `/teacher/dashboard` | 教师 | 汇总教师近期教学任务 | 待发布、待批改、待处理预警、今日截止、负责课程摘要 | 传统 + AI 提示 | 登录 + `teacher`；仅负责课程汇总 |
| 我的课程 | `TeacherCourseList` | `/teacher/courses` | 教师 | 查看负责课程并进入课程工作台 | 学期/状态筛选、课程卡片或列表、创建课程申请入口 | 传统业务 | 登录 + `teacher`；仅负责或获授权协作的课程 |
| 课程工作台 | `TeacherCourseDetail` | `/teacher/courses/:courseId` | 教师 | 管理单门课程的核心入口 | 运行概览、内容/学生/作业/考试/成绩/互动入口、发布状态 | 传统业务 | 登录 + `teacher` + 课程负责人/协作者关系 |
| 章节与内容 | `TeacherCourseContent` | `/teacher/courses/:courseId/content` | 教师 | 管理课程章节和课时结构 | 章节树、排序、发布/下线、添加课时、内容状态 | 传统业务 | 登录 + `teacher` + 该课程内容管理权限 |
| 课时编辑 | `TeacherLessonEdit` | `/teacher/courses/:courseId/lessons/:lessonId/edit` | 教师 | 编辑具体课时并准备知识点摘要 | 内容编辑、资料、预览、AI 摘要草稿、人工编辑与发布 | 传统 + AI 嵌入 | 登录 + `teacher` + 该课程编辑权限；AI 结果不能自动发布 |
| 课程学生 | `TeacherCourseStudents` | `/teacher/courses/:courseId/students` | 教师 | 查看本课程学生和选课状态 | 学生列表、选课状态、学习概览、进入学生学情 | 传统业务 | 登录 + `teacher`；只能查看负责课程内的必要学生数据 |
| 作业列表 | `TeacherAssignmentList` | `/teacher/assignments` | 教师 | 跨负责课程管理作业 | 课程/状态筛选、草稿/已发布/已截止、提交与批改进度 | 传统业务 | 登录 + `teacher`；仅负责课程作业 |
| 新建作业 | `TeacherAssignmentCreate` | `/teacher/courses/:courseId/assignments/new` | 教师 | 为指定课程发布作业 | 标题、要求、附件、截止时间、提交类型、评分规则、保存草稿/发布 | 传统业务 | 登录 + `teacher` + 该课程作业发布权限 |
| 编辑作业 | `TeacherAssignmentEdit` | `/teacher/assignments/:assignmentId/edit` | 教师 | 修改已有作业 | 修改草稿、受控修改已发布作业、查看影响范围 | 传统业务 | 登录 + `teacher` + 作业所属课程管理权限；发布后限制关键字段 |
| 作业提交情况 | `TeacherAssignmentSubmissions` | `/teacher/assignments/:assignmentId/submissions` | 教师 | 查看提交进度并选择待批改学生 | 已交/未交/逾期/已批改筛选、学生队列、批量提醒、进入批改 | 传统业务 | 登录 + `teacher`；仅所属课程学生提交数据 |
| 作业批改工作台 | `TeacherAssignmentGrading` | `/teacher/assignments/:assignmentId/grading/:submissionId` | 教师 | 连续查看作品、评分和发布评语 | 学生队列、提交预览、量规、分数、AI 评语草稿、发布并下一份 | 传统 + AI 嵌入 | 登录 + `teacher` + 评分权限；AI 不自动打分/发布；记录最终教师操作 |
| 考试列表 | `TeacherExamList` | `/teacher/exams` | 教师 | 跨负责课程管理考试 | 课程/状态筛选、考试安排、试卷状态、参考和阅卷进度 | 传统业务 | 登录 + `teacher`；仅负责课程考试 |
| 新建考试 | `TeacherExamCreate` | `/teacher/courses/:courseId/exams/new` | 教师 | 创建考试基本信息 | 名称、时间窗、时长、规则、保存草稿，创建后进入试卷编排 | 传统业务 | 登录 + `teacher` + 该课程考试管理权限 |
| 编辑考试 | `TeacherExamEdit` | `/teacher/exams/:examId/edit` | 教师 | 调整考试安排和发布状态 | 基本信息、参加范围、规则、发布/撤回、冲突提示 | 传统业务 | 登录 + `teacher` + 考试所属课程权限；开考后限制修改 |
| 试卷编排 | `TeacherExamPaper` | `/teacher/exams/:examId/paper` | 教师 | 手工或智能构建试卷 | 题型区块、选题、分值、知识点/难度分布、AI 组卷建议、预览 | 传统 + AI 嵌入 | 登录 + `teacher` + 该考试权限；AI 只建议，教师确认入卷 |
| 题库 | `TeacherQuestionBank` | `/teacher/question-bank` | 教师 | 管理可用于负责课程的题目 | 课程/知识点/题型筛选、新建/编辑题目、使用记录 | 传统业务 | 登录 + `teacher`；仅本人、课程共享或学校授权题库 |
| 考试记录与结果 | `TeacherExamRecords` | `/teacher/exams/:examId/records` | 教师 | 查看参考、提交、阅卷和结果发布 | 考生状态、异常记录、客观题结果、主观题评分、发布成绩 | 传统业务 | 登录 + `teacher` + 考试管理/阅卷权限 |
| 课程成绩册 | `TeacherGradebook` | `/teacher/courses/:courseId/gradebook` | 教师 | 汇总并发布课程成绩 | 成绩构成、作业/考试分数、总评、缺失状态、发布与导出 | 传统业务 | 登录 + `teacher` + 该课程成绩权限；发布操作需额外确认 |
| 课程学情 | `TeacherCourseProgress` | `/teacher/courses/:courseId/progress` | 教师 | 查看课程学习进展和学生差异 | 完成率、活跃趋势、任务完成、学生列表、预警入口 | 传统业务 | 登录 + `teacher`；仅该课程聚合及学生必要数据 |
| 学习预警 | `TeacherWarningList` | `/teacher/warnings` | 教师 | 跨负责课程处理风险学生 | 课程/风险等级/状态筛选、触发原因、处理状态、详情入口 | 传统 + AI 嵌入 | 登录 + `teacher`；仅负责课程预警 |
| 预警详情 | `TeacherWarningDetail` | `/teacher/warnings/:warningId` | 教师 | 查看依据并制定干预措施 | 学习证据、风险解释、AI 干预建议、教师备注、处理/关闭 | 传统 + AI 嵌入 | 登录 + `teacher`；仅负责课程；AI 建议不可自动通知学生 |
| 课程公告 | `TeacherAnnouncementList` | `/teacher/announcements` | 教师 | 管理负责课程公告 | 课程筛选、新建、编辑、定时/立即发布、查看已读情况 | 传统业务 | 登录 + `teacher`；只能向负责课程发布 |
| 论坛管理 | `TeacherForumList` | `/teacher/forums` | 教师 | 管理负责课程讨论区 | 课程筛选、置顶/精华、关闭讨论、处理举报、教师回复 | 传统业务 | 登录 + `teacher`；仅负责课程，删除需留操作记录 |

### 7.1 教师端一级菜单建议

1. 教学工作台
2. 课程管理
3. 作业与批改
4. 考试与题库
5. 学情与预警
6. 课程互动

“成绩册”属于具体课程工作台，不再单独做一个跨所有课程的一级菜单；教师从课程、作业、考试或工作台都可进入相应课程成绩册。

---

## 8. 管理员端页面清单

| 页面名称 | 建议路由名称 | 路由建议 | 所属角色 | 页面用途 | 主要功能 | 模块类型 | 权限控制 |
|---|---|---|---|---|---|---|---|
| 管理数据看板 | `AdminDashboard` | `/admin/dashboard` | 管理员 | 查看全局教学运行与异常 | 用户活跃、开课/选课、作业/考试完成、预警、系统状态、下钻入口 | 传统业务 | 登录 + `admin`；全局聚合数据，敏感明细按管理员级别限制 |
| 用户管理 | `AdminUserList` | `/admin/users` | 管理员 | 管理学生、教师和管理员账号 | 角色/状态/院系筛选、新建/导入、启停、进入详情 | 传统业务 | 登录 + `admin` + 用户管理权限 |
| 用户详情 | `AdminUserDetail` | `/admin/users/:userId` | 管理员 | 查看和维护单个用户 | 基本资料、角色、状态、组织归属、重置安全状态、必要操作记录 | 传统业务 | 登录 + `admin` + 用户管理权限；限制查看敏感信息 |
| 课程分类 | `AdminCourseCategoryList` | `/admin/course-categories` | 管理员 | 维护全局课程分类 | 新建、编辑、排序、启停、引用检查 | 传统业务 | 登录 + `admin` + 课程配置权限 |
| 课程管理 | `AdminCourseList` | `/admin/courses` | 管理员 | 查看全校课程及运行状态 | 分类/院系/状态筛选、上下线、审核状态、详情入口 | 传统业务 | 登录 + `admin` + 课程管理权限 |
| 课程详情 | `AdminCourseDetail` | `/admin/courses/:courseId` | 管理员 | 查看课程元数据和全局运行情况 | 基本信息、教师、选课、章节摘要、作业/考试运行、审核记录 | 传统业务 | 登录 + `admin` + 课程查看权限；默认不直接修改教师教学内容 |
| 课程审核 | `AdminCourseReviewList` | `/admin/course-reviews` | 管理员 | 集中处理待审核课程 | 待审核/通过/驳回筛选、提交人、提交时间、审核详情入口 | 传统业务 | 登录 + `admin` + 课程审核权限 |
| 课程审核详情 | `AdminCourseReviewDetail` | `/admin/course-reviews/:reviewId` | 管理员 | 对课程发布申请做出审核结论 | 课程信息对比、材料、审核意见、通过/驳回、历史记录 | 传统业务 | 登录 + `admin` + 审核权限；审核结果不可静默覆盖 |
| 公告管理 | `AdminAnnouncementList` | `/admin/announcements` | 管理员 | 管理校级与全局公告 | 状态/范围筛选、新建、编辑、发布/撤回、查看触达 | 传统业务 | 登录 + `admin` + 公告管理权限 |
| 新建公告 | `AdminAnnouncementCreate` | `/admin/announcements/new` | 管理员 | 创建校级或指定范围公告 | 标题、正文、附件、受众、时间、草稿/发布 | 传统业务 | 登录 + `admin` + 公告发布权限 |
| 编辑公告 | `AdminAnnouncementEdit` | `/admin/announcements/:announcementId/edit` | 管理员 | 修改公告 | 编辑草稿、受控修改已发布公告、撤回 | 传统业务 | 登录 + `admin` + 公告管理权限；保留发布历史 |
| 论坛治理 | `AdminForumModeration` | `/admin/forum-moderation` | 管理员 | 处理跨课程举报和违规内容 | 举报队列、内容状态、课程/风险筛选、处置入口 | 传统业务 | 登录 + `admin` + 内容治理权限 |
| 帖子处置 | `AdminForumPostModeration` | `/admin/forum-moderation/posts/:postId` | 管理员 | 核对举报证据并处理内容 | 帖子与上下文、举报原因、隐藏/恢复/删除、处置说明 | 传统业务 | 登录 + `admin` + 内容治理权限；所有操作写审计记录 |
| 数据统计 | `AdminStatistics` | `/admin/statistics` | 管理员 | 查看全局用户、课程和学习成效 | 时间/院系/课程筛选、趋势、完成率、成绩分布、明细与导出 | 传统业务 | 登录 + `admin` + 统计权限；导出单独授权 |
| AI 管理 | `AdminAiManagement` | `/admin/ai-management` | 管理员 | 管理 AI 功能可用范围和运行风险 | 功能开关、课程资料索引状态、使用量/失败率、内容安全、操作审计入口 | AI 管理模块 | 登录 + `admin` + AI 管理权限；默认不可浏览学生私人会话正文 |
| 系统设置 | `AdminSettings` | `/admin/settings` | 管理员 | 维护少量全局参数 | 学期、基础字典、登录安全、文件限制、通知模板 | 传统业务 | 登录 + `admin` + 系统配置权限；高风险操作二次确认 |

### 8.1 管理员端一级菜单建议

1. 数据看板
2. 用户管理
3. 课程治理（分类、课程、审核）
4. 内容治理（公告、论坛）
5. 数据统计
6. AI 管理
7. 系统设置

管理员不直接进入教师批改页面修改评分，不默认读取学生 AI 私人会话，也不替教师编辑课程正文。管理员负责全局账号、分类、审核、公告、治理和统计；教学内容与评分责任仍归课程教师。

---

## 9. 菜单与权限规划

### 9.1 页面访问矩阵

| 路由域 | 未登录 | 学生 | 教师 | 管理员 |
|---|---:|---:|---:|---:|
| `/login`、`/forgot-password`、错误页 | 允许 | 已登录时重定向 | 已登录时重定向 | 已登录时重定向 |
| `/catalog/*`、公开 `/announcements/*` | 可选允许 | 允许 | 允许 | 允许 |
| `/notifications`、`/profile` | 拒绝 | 允许 | 允许 | 允许 |
| `/student/*` | 拒绝 | 允许 | 拒绝，除非账号切换到学生角色 | 拒绝，除非账号切换到学生角色 |
| `/teacher/*` | 拒绝 | 拒绝 | 允许 | 拒绝，除非明确切换到教师角色 |
| `/admin/*` | 拒绝 | 拒绝 | 拒绝 | 允许 |

前端隐藏菜单只能改善体验，不能构成安全边界。每次请求课程、提交、成绩、用户等数据时，后端仍需检查角色和资源范围。

### 9.2 角色数据范围

#### 学生

- 只能查看本人资料、通知、选课、作业提交、考试记录、成绩、进度和预警。
- 只能学习已选且已发布的课程内容；可选课程只显示允许公开给学生的简介。
- 只能编辑/删除本人且规则允许修改的帖子、评论和作业草稿。
- AI 只能使用该学生有权访问的课程和章节资料，不能通过提问越权获取其他课程、试卷答案或其他学生数据。

#### 教师

- 只能管理“负责人、授课教师或授权协作者”关系覆盖的课程。
- 只能发布该范围内的章节、作业、考试、公告和论坛管理操作。
- 只能查看这些课程中的学生必要数据、提交、成绩和预警；不能搜索全校学生成绩。
- 题库需区分“本人私有、课程共享、学校公共”，教师不能修改没有编辑权的公共题目。
- AI 生成摘要、评语、干预建议和试卷建议时沿用同一数据范围，不能扩大原页面权限。

#### 管理员

- 可管理用户、课程分类、课程审核、校级公告、内容治理和全局统计。
- 可查看课程运行数据，但不默认替教师改作业分数、考试分数和教学正文。
- 管理员权限也建议细分为用户管理、课程审核、内容治理、统计、系统配置、AI 管理，而不是所有管理员永远拥有全部能力。
- AI 管理页默认展示用量、失败、索引和安全事件，不展示学生私人会话正文；确需审计正文时必须单独授权并留痕。

### 9.3 推荐权限粒度

| 权限类别 | 示例能力 | 典型角色与范围 |
|---|---|---|
| 账户 | 查看/编辑本人资料、查看本人通知 | 所有登录用户，仅本人 |
| 课程学习 | 查看已选课程、完成课时、选课 | 学生，本人和可选范围 |
| 作业提交 | 保存、提交、重交本人作业 | 学生，仅本人 |
| 考试参与 | 进入考试、保存答题、交卷、看已发布结果 | 学生，仅本人考试会话 |
| 课程管理 | 编辑章节、课时、学生名单、课程状态 | 教师，仅负责课程 |
| 作业评分 | 查看提交、评分、发布评语 | 教师，仅负责课程 |
| 考试管理 | 建考试、组卷、阅卷、发布结果 | 教师，仅负责课程 |
| 学情管理 | 查看课程进度、处理预警 | 教师，仅负责课程 |
| 用户管理 | 创建、启停、分配角色 | 管理员，按管理权限 |
| 课程治理 | 分类、审核、上下线 | 管理员，按治理权限 |
| 内容治理 | 公告、论坛举报处置 | 管理员，按治理权限 |
| 全局统计 | 查看和导出聚合数据 | 管理员，导出单独授权 |
| AI 使用 | 答疑、摘要、评语、预警解释、组卷建议 | 继承所在业务页面权限 |
| AI 管理 | 功能开关、索引、用量、安全与审计 | 指定管理员 |

### 9.4 登录与角色校验层级

访问一个受保护详情页时，依次检查：

1. 是否已登录。
2. 当前会话角色是否匹配路由域。
3. 是否拥有页面所需的功能权限。
4. 是否拥有该 `courseId`、`assignmentId`、`examId` 等资源的数据范围。
5. 资源当前状态是否允许操作，例如作业已截止、考试已开考、成绩未发布。

失败时应区分：未登录跳登录；角色/功能无权进入 `/403`；资源不存在返回 404；资源状态不允许时留在业务页并解释原因。

---

## 10. AI 功能的业务入口

| AI 功能 | 主业务入口 | 辅助入口 | AI 输出落点 | 不允许的设计 |
|---|---|---|---|---|
| 智能答疑助手 | `/student/courses/:courseId/lessons/:lessonId` 的右侧栏 | `/student/ai-assistant` 延续已有上下文 | 当前章节对话、引用来源、练习建议 | 进入聊天后不知道课程/章节；越权读取未选课程 |
| 章节知识点摘要 | 学生章节学习页查看教师已发布摘要 | 教师课时编辑页生成和编辑草稿 | 课时摘要区，由教师发布 | 给学生展示未经确认的生成草稿；另建孤立摘要中心 |
| 自动批改评语 | 教师作业批改工作台右栏 | 无需独立一级菜单 | 当前提交的可编辑评语草稿 | AI 自动打分、自动发布、覆盖教师原评语 |
| 学习风险预警 | 学生首页/进度页、教师预警列表 | 预警详情页查看解释与建议 | 可追溯的触发依据、风险解释、改进/干预动作 | 只显示模糊“高风险”；用绝对化结论代替依据 |
| 智能组卷建议 | 具体考试的试卷编排页 | 从题库筛选后发起建议 | 知识点、题型、难度分布和候选题 | 独立聊天生成无法追溯题目的整张试卷；未经教师确认入卷 |

完整 AI 学习助手页保留，是为了查看历史会话和跨章节继续学习；它不能替代章节内答疑入口，也不能承载教师评语或组卷功能。

---

## 11. 重点页面跳转关系

### 11.1 学生：课程学习、作业、成绩与 AI 答疑

```text
/student/dashboard
  ├─ 点击“继续学习”
  │   → /student/courses/:courseId/lessons/:lessonId
  │      ├─ 完成课时 → 下一 lessonId
  │      ├─ 点击“智能答疑” → 当前页打开 AI 侧栏
  │      └─ 点击“完整对话”
  │         → /student/ai-assistant?courseId=:courseId&lessonId=:lessonId
  │
  ├─ 点击课程卡片
  │   → /student/courses/:courseId
  │      ├─ 点击章节 → /student/courses/:courseId/lessons/:lessonId
  │      ├─ 点击课程作业 → /student/assignments?courseId=:courseId
  │      └─ 点击课程成绩 → /student/grades/courses/:courseId
  │
  └─ 点击待办作业
      → /student/assignments/:assignmentId
         ├─ 保存草稿 → 留在当前页，更新草稿时间
         ├─ 确认提交 → 留在当前页，状态改为“已提交”
         └─ 成绩发布后点击“查看反馈”
            → 当前页反馈区或 /student/grades/courses/:courseId
```

选课流程：

```text
/student/courses?scope=available
  → /student/courses/:courseId
  → 确认选课
  → /student/courses?scope=enrolled
  → /student/courses/:courseId
```

同一课程详情页根据学生是否已选课展示不同操作，避免再创建一套“选课课程详情页”。

### 11.2 学生：考试与结果

```text
/student/dashboard 或 /student/exams
  → /student/exams/:examId
  → 满足开考条件后 /student/exams/:examId/session
  → 确认交卷
  → /student/exams/:examId（显示已交卷、等待结果）
  → 教师发布成绩后 /student/exams/:examId/result
  → /student/grades/courses/:courseId
```

在线考试页不允许通过普通后退操作重复创建考试会话；重新进入时恢复或提示已有会话状态。

### 11.3 教师：课程、发布作业与批改

```text
/teacher/dashboard
  → /teacher/courses/:courseId
     ├─ 管理章节 → /teacher/courses/:courseId/content
     │  └─ 编辑课时 → /teacher/courses/:courseId/lessons/:lessonId/edit
     │     └─ 生成摘要草稿 → 教师编辑 → 发布给学生
     │
     └─ 新建作业 → /teacher/courses/:courseId/assignments/new
        → 保存草稿后 /teacher/assignments/:assignmentId/edit
        → 发布后 /teacher/assignments/:assignmentId/submissions
        → 选择学生 /teacher/assignments/:assignmentId/grading/:submissionId
        → 生成 AI 评语草稿 → 教师修改 → 发布并进入下一份
```

批改完成后，提交列表、成绩册和学生作业详情读取同一正式评分结果，不建立独立“AI 批改结果”。

### 11.4 教师：学习预警

```text
/teacher/dashboard（待处理预警）或 /teacher/warnings
  → /teacher/warnings/:warningId
  → 查看缺交、低分、进度等触发依据
  → 查看 AI 风险解释与干预建议
  → 教师填写处理记录 / 调整处理状态
  → 返回 /teacher/warnings
```

教师关闭预警表示“已处理”，不应删除学生历史依据；学生端是否显示该预警由业务状态和可见策略决定。

### 11.5 教师：智能组卷

```text
/teacher/exams
  → /teacher/courses/:courseId/exams/new
  → 保存考试基本信息
  → /teacher/exams/:examId/paper
     ├─ 手工选题
     ├─ 设置知识点、题型、难度与分值目标
     ├─ 请求智能组卷建议
     ├─ 教师逐题接受 / 替换 / 删除
     └─ 预览并确认试卷
  → /teacher/exams/:examId/edit
  → 发布考试
```

AI 给出候选题和分布建议，最终试卷仍由教师确认；候选题必须来自教师有权使用的题库。

### 11.6 管理员：用户、课程审核、公告与统计

```text
/admin/dashboard
  ├─ 用户异常/用户指标 → /admin/users → /admin/users/:userId
  ├─ 待审核课程 → /admin/course-reviews
  │  → /admin/course-reviews/:reviewId
  │  → 通过/驳回 → 返回审核列表并更新课程状态
  ├─ 发布公告 → /admin/announcements/new
  │  → 保存/发布 → /admin/announcements
  └─ 全局指标 → /admin/statistics
     → 使用学期、院系、课程筛选下钻
```

课程审核详情可以查看课程提交快照和变更，不直接跳进教师编辑器替教师修改内容。

---

## 12. 开发优先级

优先级以“先闭合真实业务，再扩展考试与互动，最后做高级治理”为原则。阶段指页面与路由开发顺序，不代表后端可以忽略相应的数据模型和权限设计。

### 第一阶段：课程—作业—评分最小闭环

**目标：** 先跑通学生学习、提交，教师发布、批改，管理员管理基础数据和审核的核心链路；同时验证两处最有价值的 AI 嵌入。

优先页面：

- 公共：登录、找回密码、403、404、个人中心。
- 学生：学习首页、我的课程、课程详情、章节学习、我的作业、作业详情与提交、成绩总览、课程成绩详情。
- 教师：教学工作台、我的课程、课程工作台、章节与内容、课时编辑、作业列表、新建/编辑作业、提交情况、批改工作台、成绩册。
- 管理员：数据看板基础版、用户管理/详情、课程分类、课程管理/详情、课程审核/详情。
- AI：章节学习中的智能答疑；课时编辑中的知识点摘要草稿；批改工作台中的 AI 评语草稿。

原因：

- 这组页面形成“管理员准备基础数据 → 教师建课与发作业 → 学生学习与提交 → 教师批改 → 学生查成绩”的完整数据闭环。
- 章节答疑和批改评语都有明确输入、输出与人工确认，适合最早验证 AI 是否真正进入业务，而不是先做空聊天框。
- 暂不把论坛、完整考试、复杂统计混进首轮，能降低页面数量和联调范围。

### 第二阶段：考试、进度预警与课程互动

**目标：** 补齐完整教学过程，覆盖考试、学情干预、公告与论坛，并上线其对应 AI 能力。

开发页面：

- 学生：我的考试、考试说明、在线考试、考试结果、学习进度与预警、预警详情、论坛/帖子、公告中心。
- 教师：考试列表、新建/编辑考试、试卷编排、题库、考试记录与结果、课程学情、学习预警/详情、课程公告、论坛管理。
- 管理员：公告管理、论坛治理、数据统计基础版。
- AI：学习风险解释与改进/干预建议、智能组卷建议、完整 AI 学习助手页与会话历史。

原因：

- 考试涉及时间窗、会话恢复、交卷、阅卷和结果发布，复杂度高于普通作业，应在第一阶段权限和状态体系稳定后实施。
- 预警必须建立在已有学习、提交和成绩数据上，过早开发只能得到假数据页面。
- 论坛与公告重要但不阻断第一阶段学习—作业闭环，可在核心状态模型稳定后接入。

### 第三阶段：治理、分析深化与体验完善

**目标：** 面向运行质量，而不是继续堆基础 CRUD 页面。

开发内容：

- 管理员：全局统计深化、AI 管理、系统设置、审计与导出权限。
- 公共：确有对外展示需要时启用公开课程目录和公开公告。
- 三端：消息中心完善、跨端响应式细化、可访问性、空/错/无权限状态、深链接恢复。
- 数据：统计下钻、报表导出、AI 使用量/失败率/索引状态、操作审计。

原因：

- 高级统计和 AI 治理需要真实运行数据，否则只能展示装饰图表。
- 系统设置与精细权限应基于已经验证的管理职责建设，避免首期制造大量无人使用的配置项。
- 移动端优先完善学生学习、提交和查成绩；教师批改和管理员后台以桌面端为主。

---

## 13. 建议暂缓开发的低优先级功能

| 暂缓功能 | 暂缓原因 | 建议重新评估时机 |
|---|---|---|
| 学生自由拖拽/自定义首页组件 | 增加布局持久化和适配成本，对核心学习闭环帮助有限 | 核心首页使用数据稳定后 |
| 徽章、积分、排行榜、复杂学习热力图 | 容易变成装饰性功能，且涉及激励公平性 | 有明确运营目标和真实活动数据后 |
| 教师批量一键采用 AI 评语 | 风险高，容易把未经核对的内容直接发布给学生 | 单份 AI 草稿质量和审核流程稳定后 |
| AI 自动评分或自动发布成绩 | 责任与可解释性不足，超出“评语草稿”边界 | 有正式教学政策、评测和人工复核机制后 |
| 无上下文的全站 AI 聊天入口 | 容易与课程业务割裂，无法证明教学价值 | 章节答疑和业务上下文链路成熟后再评估 |
| 可视化拖拽报表设计器 | 开发量大，管理员早期通常只需要固定核心指标和导出 | 固定统计无法满足明确需求时 |
| 自定义角色/权限设计器 | 首期三角色清晰，过早开放会显著增加权限测试复杂度 | 出现教务员、助教、审核员等稳定新角色后 |
| 多校区/多租户/白标主题 | 不属于当前单系统核心教学流程 | 项目确定产品化或多机构部署后 |
| 教师端与管理员端完整移动适配 | 批改、组卷、统计更适合桌面大屏 | 桌面版稳定且有真实移动办公需求后 |
| 实时私信、群聊、音视频课堂 | 涉及实时通信、审核、存储和运维，范围远超课程论坛 | 核心 LMS 稳定并明确集成策略后 |
| 复杂课程版本回滚与多人实时协同编辑 | 实现与冲突处理成本高 | 单教师编辑流程稳定且出现真实协同需求后 |
| 管理员查看全部 AI 私人会话正文 | 隐私与权限风险高，非日常管理必需 | 仅在法规/安全审计明确要求且有严格授权时 |

---

## 14. 路由与页面开发验收清单

- [ ] 所有角色业务路径分别位于 `/student`、`/teacher`、`/admin`。
- [ ] 每个详情页刷新后能独立加载，不依赖列表页内存。
- [ ] 学生不能进入教师或管理员路由，教师不能进入管理员路由。
- [ ] 教师即使手动替换 URL 中的资源 ID，也不能访问非负责课程数据。
- [ ] 管理员能力集中在用户、分类、审核、公告、治理和统计，不越权替教师评分。
- [ ] 课程页进入作业、考试、成绩、论坛时复用聚合页并携带课程筛选，不重复实现列表。
- [ ] 智能答疑能带入课程与章节上下文，并限制资料范围。
- [ ] AI 摘要、评语和组卷建议均为草稿/建议，必须人工确认。
- [ ] 预警页面显示触发依据和处理状态，不只显示一个风险分数。
- [ ] 未登录、无角色权限、无资源权限、资源不存在、资源状态不允许具有不同反馈。
- [ ] 第一阶段可以完成课程—学习—作业—批改—成绩的数据闭环，再进入第二阶段。
