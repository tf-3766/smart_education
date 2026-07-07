# 课程与学习基础模块交付说明

> 阶段：后端第 2 阶段 A–E  
> 服务：`edu-biz-service`  
> 数据库版本：Flyway `V3`  
> 联调契约：`course-api-contract.md`、`openapi/course-module.openapi.yaml`

## 1. 实现结论

已实现教师创建课程、管理员审核、负责人发布、教师团队管理、章节/课时/资料元数据维护、学生目录与选课、授权内容访问、课时开始/完成和课程进度聚合。课程权限不是只看角色：每次资源操作都会继续校验课程教师关系、选课关系、跨表归属、发布状态和解锁状态。

未实现作业、考试、成绩、论坛、公告、预警、消息和 AI；没有引入 Multipart、对象存储、向量库或真实文件服务。

## 2. 文件清单

### 2.1 设计与联调文档

- `CONTEXT.md`：课程领域通用语言。
- `docs/course-module-design.md`：聚合、状态机、权限、索引和后续依赖。
- `docs/course-api-contract.md`：42 个课程模块接口的 DTO/VO、权限和错误契约。
- `docs/openapi/course-module.openapi.yaml`：可导入 Swagger Editor、Postman、Apifox 的 OpenAPI 3.1 文件。
- `docs/course-module-delivery.md`：本交付说明。
- `docs/local-development.md`：补充课程本地数据与第二教师账号。

### 2.2 数据库

- `V2__create_course_tables.sql`：课程、教师关系、选课、章节、课时、资料、学习记录七张核心表。
- `V3__create_course_review_table.sql`：审核历史表。
- `R__local_course_test_data.sql`：仅 local/test profile 加载的课程联调数据。

### 2.3 领域与持久化

- 枚举：`LabeledEnum`、`CourseStatus`、`CourseReviewStatus`、`CourseTeacherRole`、`EnrollmentStatus`、`ChapterStatus`、`LessonStatus`、`LessonContentType`、`LessonUnlockType`、`MaterialType`、`MaterialVisibility`、`MaterialStatus`、`LearningStatus`。
- Entity：`CourseEntity`、`CourseTeacherEntity`、`CourseEnrollmentEntity`、`CourseChapterEntity`、`CourseLessonEntity`、`CourseMaterialEntity`、`LessonLearningRecordEntity`、`CourseReviewEntity`。
- Mapper：与八个 Entity 一一对应的 MyBatis-Plus Mapper。

### 2.4 API 与应用层

- Query：`CourseListQuery`、`CourseMaterialListQuery`。
- Request DTO：课程创建/更新、教师添加、审核批准/驳回、章节创建/更新、课时创建/更新、资料创建/更新。
- VO：教师/学生课程列表、课程详情、教师关系、选课、审核、章节/课时详情、资料、大纲树、学习记录、进度和授权资料访问。
- Assembler：`CourseAssembler`、`CourseContentAssembler`。
- Service：`CoursePermissionService`、`CourseManagementService`、`CourseReviewService`、`StudentCourseService`、`CourseContentService`、`StudentLearningService`。
- Controller：`TeacherCourseController`、`AdminCourseReviewController`、`StudentCourseController`、`TeacherCourseContentController`、`StudentLearningController`。

### 2.5 测试

- `CourseStateMachineTest`。
- `CourseSchemaMigrationTest`。
- `CourseLifecycleApiIntegrationTest`。
- `CourseContentLearningApiIntegrationTest`。

## 3. Flyway 版本与表结构

### 3.1 版本

| 版本 | 内容 | 环境 |
|---|---|---|
| `V2` | 七张课程与学习核心表、唯一约束和查询索引 | 所有环境 |
| `V3` | `edu_course_review` 审核历史表 | 所有环境 |
| `R__local_course_test_data` | 固定联调账号关联、课程、章节、课时、资料和审核数据 | local/test |

生产与共享库只执行版本化迁移，禁止直接手工建表或修改列。重复迁移不是生产种子。

### 3.2 表、归属与删除策略

| 表 | 模块事实 | 删除策略 | 关键约束/索引 |
|---|---|---|---|
| `edu_course` | 课程、唯一负责人、运行与审核当前状态 | 逻辑删除 | `uk_course_code`；负责人列表、审核队列、目录和 term/category 索引 |
| `edu_course_teacher` | OWNER/COLLABORATOR 关系 | 逻辑删除 | `uk_course_teacher(course_id,teacher_id)`；教师反查和课程角色索引 |
| `edu_course_enrollment` | 学生唯一选课关系 | 状态流转，不物理删除 | `uk_course_enrollment(course_id,student_id)`；学生/课程状态列表索引 |
| `edu_course_chapter` | 章节与排序 | 逻辑删除，不级联学习记录 | 课程排序、课程状态索引 |
| `edu_course_lesson` | 课时正文、发布和解锁 | 逻辑删除，不级联学习记录 | 章节排序、课程状态、解锁索引 |
| `edu_course_material` | 文件元数据与课程层级关联 | 逻辑删除 | course/chapter/lesson 三组访问索引 |
| `edu_lesson_learning_record` | 学生本人课时学习事实 | 保留历史 | `uk_lesson_student(lesson_id,student_id)`；学生课程进度索引 |
| `edu_course_review` | 追加式审核结论、审核人、时间和原因 | 保留审计历史 | 课程审核历史和审核状态统计索引 |

八张表统一使用 Long 雪花 ID，并包含 `id/created_at/created_by/updated_at/updated_by/deleted/version`。不建数据库物理外键，Service 强制校验归属；所有列表与权限热路径均有复合索引。

## 4. API 总览

完整请求字段、响应 VO、查询参数、权限和错误码见 `course-api-contract.md`；课程模块包含 42 个 operation，OpenAPI 另收录登录接口，共 33 个 path、43 个 operation。

| 分组 | 接口数 | 前缀 |
|---|---:|---|
| 教师课程与成员 | 10 | `/api/v1/teacher/courses` |
| 管理员审核 | 4 | `/api/v1/admin/course-reviews` |
| 学生目录与选课 | 5 | `/api/v1/student/courses` |
| 教师章节与课时 | 13 | `/api/v1/teacher/courses|chapters|lessons` |
| 教师资料 | 4 | `/api/v1/teacher/courses|materials` |
| 学生学习与进度 | 6 | `/api/v1/student/courses|lessons|materials` |

创建课时示例：

```http
POST /api/v1/teacher/chapters/22001/lessons
Authorization: Bearer <teacher-token>
Content-Type: application/json

{
  "courseId": "21001",
  "title": "链表基础",
  "contentType": "RICH_TEXT",
  "content": "# 链表基础\n本课时正文统一使用 Markdown。",
  "estimatedMinutes": 30,
  "sortOrder": 30,
  "unlockType": "IMMEDIATE"
}
```

学生大纲响应示例：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "courseId": "21001",
    "courseName": "已发布测试课程",
    "status": {"code": "PUBLISHED", "label": "已发布"},
    "chapters": [{
      "chapterId": "22001",
      "title": "第一章 已发布",
      "sortOrder": 10,
      "lessons": [{
        "lessonId": "23001",
        "title": "公开课时",
        "sortOrder": 10,
        "contentType": {"code": "RICH_TEXT", "label": "富文本"},
        "estimatedMinutes": 30,
        "unlocked": true,
        "completed": false,
        "learningStatus": {"code": "NOT_STARTED", "label": "未开始"}
      }]
    }]
  },
  "errors": [],
  "traceId": "...",
  "timestamp": "2026-07-07T00:00:00Z"
}
```

资源越权与状态冲突分别返回：

```json
{"code":"FORBIDDEN","message":"你无权访问该课程资料","data":null,"errors":[],"traceId":"...","timestamp":"2026-07-07T00:00:00Z"}
```

```json
{"code":"OPERATION_NOT_ALLOWED","message":"课时尚未解锁","data":null,"errors":[],"traceId":"...","timestamp":"2026-07-07T00:00:00Z"}
```

## 5. 状态机

- 课程运行状态：`DRAFT → PENDING_REVIEW → PUBLISHED → ONGOING → FINISHED → OFFLINE`；非下线状态可按规则转到 `OFFLINE`。
- 审核状态：`NOT_SUBMITTED → PENDING → APPROVED` 或 `REJECTED → PENDING`。
- 章节/课时：`DRAFT → PUBLISHED → OFFLINE`。
- 选课：`ENROLLED → WITHDRAWN` 或 `COMPLETED`。
- 学习：`NOT_STARTED → IN_PROGRESS → COMPLETED`，允许首次调用 complete 直接完成。

`APPROVED` 只属于审核状态；批准不自动发布课程，发布课程也不自动发布章节/课时。

## 6. 本地测试数据

| 角色 | ID | 用户名 | 密码 | 用途 |
|---|---:|---|---|---|
| 学生 | 1001 | `student` | `Student@123` | 已选课程 21001，学习/越权测试 |
| 教师 | 1002 | `teacher` | `Teacher@123` | 课程 21001、21004 负责人 |
| 管理员 | 1003 | `admin` | `Admin@123` | 课程审核 |
| 第二教师 | 1004 | `teacher2` | `Teacher@123` | 课程 21002、21003 负责人，教师越权测试 |

固定资源：21001 已发布且学生已选；21002 草稿；21003 其他教师已发布课程；21004 已下线。22001/23001/24001 是可访问链路，23002 定时到 2099 年解锁，22002/23003 是未发布章节链路，24002 是其他课程资料。

## 7. OpenAPI / Postman 联调

1. 启动 Nacos、MySQL、Redis、RabbitMQ 后，以 local profile 启动 gateway 和 biz-service。
2. 将 `docs/openapi/course-module.openapi.yaml` 导入 Postman、Apifox 或 Swagger Editor。
3. Server 使用 `http://localhost:8080`；先执行 `/api/v1/auth/login`。
4. 在集合 Authorization 中选择 Bearer Token，并分别填入教师、学生、管理员 token。
5. 按“创建 → 提交审核 → 管理员批准 → 发布 → 内容发布 → 学生选课/学习”顺序联调。
6. 每次更新请求使用上一次响应中的 `version`；409 时重新获取详情，不盲目重放旧版本。

自动测试命令：

```powershell
cd E:\my_projects\zhongruan\backend
.\mvnw.cmd clean verify
```

## 8. 已实现与暂未实现

已实现：状态与审核分离、资源级权限、教师协作、选课幂等、学习完成幂等、Markdown 内容、资料元数据、逻辑删除、乐观锁、统一分页/响应/错误、审计字段和 traceId。

暂未实现：负责人转移、审核撤回、多级审批、拖拽批量排序、自动推进 ONGOING/FINISHED、真实上传/签名 URL、学习时长可信统计、缓存进度、完成课程自动标记、存在作业/考试后的退选策略、长期资料保留策略及任何 AI 索引。

## 9. 下一位组员协作说明

### 9.1 作业模块

- 只保存 `course_id`，按需保存 `chapter_id/lesson_id`；引用 `edu_course/edu_course_chapter/edu_course_lesson` 的 ID，不复制课程字段。
- 发布作业前调用课程域能力确认教师 `canEditCourseContent`，并校验章节/课时属于同一课程。
- 学生提交前确认 `edu_course_enrollment` 为 ENROLLED/COMPLETED；不要直接写选课或学习记录表。
- 作业模块拥有自己的发布、提交、批改状态；课程下线时如何冻结提交需单独形成契约。

### 9.2 考试模块

- 试卷/考试引用 `course_id`；若考试绑定教学单元，再引用 chapter/lesson ID。
- 组卷与发布复用课程教师关系和学生选课范围，不能把课程发布状态当作考试状态。
- 考试模块不写课程、章节、课时、选课或学习记录表；考试结束后的选课 COMPLETED 策略由跨模块用例协调。

### 9.3 AI 服务

- `edu-ai-service` 不能读取 biz 数据库，也不能复制课程、章节、课时、资料表。
- 后续由 biz-service 增加内部授权上下文接口：输入当前用户权限上下文与 course/chapter/lesson/material ID，输出允许使用的最小资料清单。
- AI 只使用资料 ID 请求内容/临时地址；不能依赖 `file_url/file_key`，不能绕过课程发布、选课和层级权限。
- 索引消息可在资料发布/下线后通过 RabbitMQ 异步通知；问答前的用户授权必须同步校验。AI 返回建议、引用和任务状态，不修改课程事实。

跨模块如需新增课程字段、索引、公共错误码或修改本模块 API，必须先更新 `course-api-contract.md`，由课程模块负责人评审并通过独立 Flyway migration 实施。

## 10. 待确认的后续决策

- 课程分类由独立字典表还是现有公共字典提供；本阶段只保存可空 `category_id`。
- 真实附件服务的 `attachmentId/fileKey` 契约和签名 URL TTL。
- OWNER 转移流程及其审核要求。
- `FINISHED` 课程对资料与内容的长期保留范围。
- 课程状态由显式命令还是调度器自动推进到 ONGOING/FINISHED。

这些决策不阻塞本阶段运行，也未被提前写死在业务代码中。
