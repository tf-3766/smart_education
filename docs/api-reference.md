# 前后端 API 参考

> 状态：2026-07-10
> 网关地址：`http://localhost:18080`
> 本文用于前后端联调；已实现接口的字段以当前 Controller DTO/VO 为准，未实现接口的字段必须在实现前补充到本文。

## 1. 使用规则

所有公开接口通过 Gateway 的 `/api/v1` 访问。除登录外，携带：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Trace-Id: <optional-trace-id>
```

统一响应为 `ApiResponse<T>`：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "errors": [],
  "traceId": "trace-id",
  "timestamp": "2026-07-10T03:00:00Z"
}
```

对外 ID 一律按字符串处理；时间使用 RFC 3339；分页查询使用 `page`、`size`、`keyword`、`status` 等参数。

## 2. 接口状态

| 标记 | 含义 | 前端处理方式 |
|---|---|---|
| `已实现` | 当前后端 Controller 已提供 | 可直接联调 |
| `契约待实现` | 文档/OpenAPI 已定义，但没有 Controller | 只能用 Mock，不能请求真实后端 |
| `内部接口` | 仅供服务间 Feign 调用 | 前端不得调用 |
| `测试接口` | 用于权限自动化测试 | 前端不得作为业务功能使用 |

当前公开可联调的是认证、课程、课程内容、选课、学习进度和课程审核。作业、成绩、论坛、预警、考试和 AI 目前均为契约待实现。

## 3. 已实现公开接口

### 3.1 认证

| 方法 | 路径 | 角色 | 请求 | 响应 |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | 匿名 | `LoginRequest` | `LoginVO` |
| `GET` | `/api/v1/auth/me` | 已登录 | 无 | `CurrentUserVO` |
| `POST` | `/api/v1/auth/logout` | 已登录 | 无 | `LogoutVO` |

登录示例：

```json
{"username":"student","password":"Student@123"}
```

### 3.2 教师课程管理

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| `POST` | `/api/v1/teacher/courses` | `CreateCourseRequest` | `CourseDetailVO`，201 |
| `GET` | `/api/v1/teacher/courses` | `CourseListQuery` | `PageResponse<TeacherCourseListItemVO>` |
| `GET` | `/api/v1/teacher/courses/{courseId}` | 路径参数 | `CourseDetailVO` |
| `PUT` | `/api/v1/teacher/courses/{courseId}` | `UpdateCourseRequest` | `CourseDetailVO` |
| `POST` | `/api/v1/teacher/courses/{courseId}/submit-review` | 无 | `CourseDetailVO` |
| `POST` | `/api/v1/teacher/courses/{courseId}/publish` | 无 | `CourseDetailVO` |
| `POST` | `/api/v1/teacher/courses/{courseId}/offline` | 无 | `CourseDetailVO` |
| `GET` | `/api/v1/teacher/courses/{courseId}/teachers` | 无 | `List<CourseTeacherVO>` |
| `POST` | `/api/v1/teacher/courses/{courseId}/teachers` | `AddCourseTeacherRequest` | `CourseTeacherVO` |
| `DELETE` | `/api/v1/teacher/courses/{courseId}/teachers/{teacherId}` | 路径参数 | `void` |

### 3.3 教师课程内容管理

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/chapters` | 无 | `List<ChapterDetailVO>` |
| `POST` | `/api/v1/teacher/courses/{courseId}/chapters` | `CreateChapterRequest` | `ChapterDetailVO`，201 |
| `PUT` | `/api/v1/teacher/chapters/{chapterId}` | `UpdateChapterRequest` | `ChapterDetailVO` |
| `DELETE` | `/api/v1/teacher/chapters/{chapterId}` | 路径参数 | `void` |
| `POST` | `/api/v1/teacher/chapters/{chapterId}/publish` | 无 | `ChapterDetailVO` |
| `POST` | `/api/v1/teacher/chapters/{chapterId}/offline` | 无 | `ChapterDetailVO` |
| `GET` | `/api/v1/teacher/chapters/{chapterId}/lessons` | 无 | `List<LessonDetailVO>` |
| `POST` | `/api/v1/teacher/chapters/{chapterId}/lessons` | `CreateLessonRequest` | `LessonDetailVO`，201 |
| `GET` | `/api/v1/teacher/lessons/{lessonId}` | 路径参数 | `LessonDetailVO` |
| `PUT` | `/api/v1/teacher/lessons/{lessonId}` | `UpdateLessonRequest` | `LessonDetailVO` |
| `DELETE` | `/api/v1/teacher/lessons/{lessonId}` | 路径参数 | `void` |
| `POST` | `/api/v1/teacher/lessons/{lessonId}/publish` | 无 | `LessonDetailVO` |
| `POST` | `/api/v1/teacher/lessons/{lessonId}/offline` | 无 | `LessonDetailVO` |
| `GET` | `/api/v1/teacher/courses/{courseId}/materials` | `CourseMaterialListQuery` | `PageResponse<CourseMaterialVO>` |
| `POST` | `/api/v1/teacher/courses/{courseId}/materials` | `CreateCourseMaterialRequest` | `CourseMaterialVO`，201 |
| `PUT` | `/api/v1/teacher/materials/{materialId}` | `UpdateCourseMaterialRequest` | `CourseMaterialVO` |
| `DELETE` | `/api/v1/teacher/materials/{materialId}` | 路径参数 | `void` |

### 3.4 学生课程与学习

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| `GET` | `/api/v1/student/courses/catalog` | `CourseListQuery` | `PageResponse<StudentCourseListItemVO>` |
| `GET` | `/api/v1/student/courses` | `CourseListQuery` | `PageResponse<StudentCourseListItemVO>` |
| `GET` | `/api/v1/student/courses/{courseId}` | 路径参数 | `CourseDetailVO` |
| `POST` | `/api/v1/student/courses/{courseId}/enroll` | 无 | `EnrollmentVO` |
| `POST` | `/api/v1/student/courses/{courseId}/withdraw` | 无 | `EnrollmentVO` |
| `GET` | `/api/v1/student/courses/{courseId}/outline` | 路径参数 | `CourseOutlineVO` |
| `GET` | `/api/v1/student/lessons/{lessonId}` | 路径参数 | `StudentLessonDetailVO` |
| `POST` | `/api/v1/student/lessons/{lessonId}/start` | 无 | `LearningRecordVO` |
| `POST` | `/api/v1/student/lessons/{lessonId}/complete` | 无 | `LearningRecordVO` |
| `GET` | `/api/v1/student/courses/{courseId}/progress` | 路径参数 | `CourseProgressVO` |
| `GET` | `/api/v1/student/materials/{materialId}` | 路径参数 | `MaterialAccessVO` |

### 3.5 管理员课程审核

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| `GET` | `/api/v1/admin/course-reviews` | `CourseListQuery` | `PageResponse<CourseReviewListItemVO>` |
| `GET` | `/api/v1/admin/course-reviews/{courseId}` | 路径参数 | `CourseReviewDetailVO` |
| `POST` | `/api/v1/admin/course-reviews/{courseId}/approve` | `ReviewCourseRequest` | `CourseReviewVO` |
| `POST` | `/api/v1/admin/course-reviews/{courseId}/reject` | `RejectCourseRequest` | `CourseReviewVO` |

### 3.6 已实现接口的数据格式

除下载、SSE 等特殊接口外，请求体均为 JSON；所有成功和失败结果均使用
`ApiResponse<T>` 包裹。`T` 是下方表格列出的响应 `data` 类型；响应中的所有
业务 ID 均为字符串，时间均为 RFC 3339 字符串。

成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {"courseId": "101", "name": "Java Web"},
  "errors": [],
  "traceId": "a6d8e30f",
  "timestamp": "2026-07-10T11:00:00Z"
}
```

字段校验失败时，`data` 为 `null`，`errors` 为 `[{"field":"name","reason":"...","rejectedValue":"..."}]`。
删除接口的成功结果为 `{"code":"SUCCESS", "data":null, ...}`。

路径参数 `{courseId}`、`{chapterId}`、`{lessonId}`、`{materialId}`、`{teacherId}` 等
均为正整数，前端以字符串拼接 URL 即可。`POST`、`PUT` 请求的请求体字段如下，`*`
表示必填，未标记字段可省略或传 `null`。

| 请求体类型 | JSON 字段 |
|---|---|
| `LoginRequest` | `username*: string(1-64)`，`password*: string(1-128)` |
| `CreateCourseRequest` | `courseCode*: string(1-64, A-Z/a-z/0-9/._-)`，`name*: string(1-160)`，`summary?: string(<=4000)`，`coverUrl?: string(<=1024)`，`categoryId?: number`，`term?: string(<=32)`，`department?: string(<=128)`，`credit?: number(0-99.99)`，`enrollmentOpenAt?`，`enrollmentCloseAt?`，`startAt?`，`endAt?` |
| `UpdateCourseRequest` | 与创建课程相同，但没有 `courseCode`；另有 `version*: number(>=0)` |
| `AddCourseTeacherRequest` | `teacherId*: number(>0)` |
| `CreateChapterRequest` | `title*: string(1-120)`，`description?: string(<=2000)`，`sortOrder*: number(0-100000)` |
| `UpdateChapterRequest` | `title*: string(1-120)`，`description?: string(<=2000)`，`sortOrder*: number(0-100000)`，`version*: number(>=0)` |
| `CreateLessonRequest` | `courseId?: number(>0)`，`title*: string(1-160)`，`contentType*: string`，`content?: string(<=60000)`，`videoUrl?: string(<=1024)`，`estimatedMinutes?: number(1-10000)`，`sortOrder*: number(0-100000)`，`unlockType*: string`，`unlockAt?: RFC3339` |
| `UpdateLessonRequest` | 创建课时的全部字段，另有 `version*: number(>=0)` |
| `CreateCourseMaterialRequest` | `chapterId?: number`，`lessonId?: number`，`name*: string(1-160)`，`materialType*: string`，`fileKey?: string(<=512)`，`fileUrl?: string(<=1024)`，`fileSize?: number(>=0)`，`mimeType?: string(<=128)`，`visibility*: string`，`status?: string`，`sortOrder*: number(0-100000)` |
| `UpdateCourseMaterialRequest` | 创建资料的全部字段，但 `status*` 必填；另有 `version*: number(>=0)` |
| `ReviewCourseRequest` | `remark?: string(<=500)`；审批接口允许不传请求体 |
| `RejectCourseRequest` | `reason*: string(1-500)` |

时间字段应传带时区的 ISO 8601/RFC 3339 值，例如 `"2026-09-01T08:00:00+08:00"`。
`version` 是乐观锁版本号，前端必须使用详情或列表最新返回值回传。枚举字段必须传后端枚举的 `code`，不要传中文 `label`。

`GET` 列表接口使用查询参数，不传时使用默认值：

| 查询类型 | 可用参数 |
|---|---|
| `CourseListQuery` | `page=1`，`size=20`（1-100），`keyword?`（<=100），`status?`，`reviewStatus?`，`enrollmentStatus?`，`term?`（<=32），`categoryId?`，`sort=createdAt,desc`（<=32） |
| `CourseMaterialListQuery` | `page=1`，`size=20`（1-100），`keyword?`，`status?`，`visibility?` |

`PageResponse<T>` 的 `data` 格式固定为：

```json
{"records": [], "page": 1, "size": 20, "total": 0, "totalPages": 0}
```

下表是已实现接口中 `data` 的字段结构。`CodeLabel` 固定为
`{"code":"PUBLISHED","label":"已发布"}`；`[]` 表示数组，`?` 表示字段可为 `null`。

| `data` 类型 | 字段 |
|---|---|
| `LoginVO` | `accessToken`，`tokenType`，`expiresIn`，`expiresAt`，`user: CurrentUserVO`，`roles: string[]`，`permissions: string[]` |
| `CurrentUserVO` | `userId`，`username`，`displayName`，`activeRole`，`roles: string[]`，`permissions: string[]` |
| `LogoutVO` | `mode`，`serverSideRevoked: boolean` |
| `CourseDetailVO` | `courseId`，`courseCode`，`name`，`summary?`，`coverUrl?`，`categoryId?`，`term?`，`department?`，`credit?`，`ownerTeacherId`，`ownerTeacherName`，`status: CodeLabel`，`reviewStatus: CodeLabel`，`enrollmentOpenAt?`，`enrollmentCloseAt?`，`startAt?`，`endAt?`，`latestReviewReason?`，`version` |
| `TeacherCourseListItemVO` | `courseId`，`courseCode`，`name`，`term?`，`ownerTeacherId`，`ownerTeacherName`，`status: CodeLabel`，`reviewStatus: CodeLabel`，`startAt?`，`endAt?`，`updatedAt` |
| `StudentCourseListItemVO` | `courseId`，`courseCode`，`name`，`summary?`，`coverUrl?`，`term?`，`credit?`，`ownerTeacherName`，`status: CodeLabel`，`enrollmentStatus: CodeLabel`，`enrollable: boolean`，`startAt?`，`endAt?` |
| `CourseTeacherVO` | `relationId`，`courseId`，`teacherId`，`teacherName`，`role: CodeLabel`，`version` |
| `ChapterDetailVO` | `chapterId`，`courseId`，`title`，`description?`，`sortOrder`，`status: CodeLabel`，`publishedAt?`，`version` |
| `LessonDetailVO` | `lessonId`，`courseId`，`chapterId`，`title`，`contentType: CodeLabel`，`content?`，`videoUrl?`，`estimatedMinutes?`，`sortOrder`，`status: CodeLabel`，`unlockType: CodeLabel`，`unlockAt?`，`publishedAt?`，`version` |
| `CourseMaterialVO` | `materialId`，`courseId`，`chapterId?`，`lessonId?`，`name`，`materialType: CodeLabel`，`fileKey?`，`fileUrl?`，`fileSize?`，`mimeType?`，`visibility: CodeLabel`，`status: CodeLabel`，`sortOrder`，`version` |
| `EnrollmentVO` | `enrollmentId`，`courseId`，`studentId`，`status: CodeLabel`，`enrolledAt?`，`withdrawnAt?`，`version` |
| `CourseOutlineVO` | `courseId`，`courseName`，`status: CodeLabel`，`chapters: ChapterOutlineVO[]` |
| `ChapterOutlineVO` | `chapterId`，`title`，`sortOrder`，`lessons: LessonOutlineVO[]` |
| `LessonOutlineVO` | `lessonId`，`title`，`sortOrder`，`contentType: CodeLabel`，`estimatedMinutes?`，`unlocked: boolean`，`completed: boolean`，`learningStatus: CodeLabel` |
| `StudentLessonDetailVO` | `lessonId`，`courseId`，`chapterId`，`title`，`contentType: CodeLabel`，`content?`，`videoUrl?`，`estimatedMinutes?`，`status: CodeLabel`，`unlockAt?`，`learningRecord: LearningRecordVO?` |
| `LearningRecordVO` | `recordId`，`courseId`，`chapterId`，`lessonId`，`studentId`，`status: CodeLabel`，`startedAt?`，`completedAt?`，`lastStudiedAt?` |
| `CourseProgressVO` | `courseId`，`totalLessons`，`availableLessons`，`completedLessons`，`progressPercent`，`lastLessonId?`，`nextLessonId?` |
| `MaterialAccessVO` | `materialId`，`name`，`materialType: CodeLabel`，`fileSize?`，`mimeType?`，`accessMode`，`accessUrl` |
| `CourseReviewListItemVO` | `courseId`，`courseCode`，`name`，`ownerTeacherId`，`ownerTeacherName`，`term?`，`courseStatus: CodeLabel`，`reviewStatus: CodeLabel`，`updatedAt` |
| `CourseReviewDetailVO` | `course: CourseDetailVO`，`history: CourseReviewVO[]` |
| `CourseReviewVO` | `reviewId`，`courseId`，`reviewStatus: CodeLabel`，`reviewerId`，`reviewerName`，`reason?`，`remark?`，`reviewedAt` |

### 3.7 测试接口

`GET /api/v1/test/student`、`/teacher`、`/admin` 仅用于后端权限验证，前端不应接入、菜单不应展示。

## 4. 未实现公开接口

以下接口已完成路径、角色和业务规则设计，但当前没有对应 Controller。前端只能使用 Mock；后端完成后将本表状态改为“已实现”。

### 4.1 作业、提交与成绩

| 方法 | 路径 | 角色 | 请求体或查询 | 成功响应 `data` | 状态 |
|---|---|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | `page/size/status/sort` | `PageResponse<AssignmentDetailVO>` | 未实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | `AssignmentCreateRequest` | `AssignmentDetailVO` | 未实现 |
| `PUT` | `/api/v1/teacher/assignments/{assignmentId}` | 教师 | `AssignmentUpdateRequest` | `AssignmentDetailVO` | 未实现 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/publish` | 教师 | 无 | `AssignmentDetailVO` | 未实现 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/close` | 教师 | 无 | `AssignmentDetailVO` | 未实现 |
| `GET` | `/api/v1/student/courses/{courseId}/assignments` | 学生 | `page/size/status/sort` | `PageResponse<StudentAssignmentListItemVO>` | 未实现 |
| `GET` | `/api/v1/student/assignments/{assignmentId}` | 学生 | 无 | `StudentAssignmentDetailVO` | 未实现 |
| `PUT` | `/api/v1/student/assignments/{assignmentId}/submission-draft` | 学生 | `SubmissionSaveRequest` | `SubmissionDetailVO` | 未实现 |
| `POST` | `/api/v1/student/assignments/{assignmentId}/submissions` | 学生 | `SubmissionSubmitRequest` | `SubmissionDetailVO` | 未实现 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/submissions` | 教师 | `page/size/status/sort` | `PageResponse<TeacherSubmissionGradeVO>` | 未实现 |
| `POST` | `/api/v1/teacher/submissions/{submissionId}/grade` | 教师 | `GradeSubmissionRequest` | `TeacherSubmissionGradeVO` | 未实现 |
| `POST` | `/api/v1/teacher/grades/{gradeId}/publication` | 教师 | `PublishGradeRequest` | `TeacherSubmissionGradeVO` | 未实现 |
| `GET` | `/api/v1/student/grades` | 学生 | `GradeListQuery` | `PageResponse<StudentGradeVO>` | 未实现 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/statistics` | 教师 | 无 | `AssignmentStatisticsVO` | 未实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/grade-statistics` | 教师 | 无 | `CourseGradeStatisticsVO`（字段待定） | 未实现 |

作业状态为 `DRAFT/PUBLISHED/CLOSED`；提交状态为 `DRAFT/SUBMITTED/RETURNED/GRADED`；成绩发布状态为 `DRAFT/PUBLISHED/REVOKED`。正式提交必须有文本内容或附件，逾期、重复提交和版本冲突返回 409。

### 4.2 论坛与学习预警

| 方法 | 路径 | 角色 | 请求体或查询 | 成功响应 `data` | 状态 |
|---|---|---|---|---|---|
| `GET` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | `page/size/status/keyword` | `PageResponse<ForumTopicListItemVO>` | 未实现 |
| `POST` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | `ForumTopicCreateRequest` | `ForumTopicDetailVO` | 未实现 |
| `GET` | `/api/v1/student/forum/topics/{topicId}` | 学生 | 无 | `ForumTopicDetailVO` | 未实现 |
| `GET` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | `page/size/status` | `PageResponse<ForumReplyVO>` | 未实现 |
| `POST` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | `ForumReplyCreateRequest` | `ForumReplyVO` | 未实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/forum/topics` | 教师 | `page/size/status/keyword` | `PageResponse<ForumTopicListItemVO>` | 未实现 |
| `PATCH` | `/api/v1/teacher/forum/topics/{topicId}/visibility` | 教师 | `ForumVisibilityRequest` | `ForumTopicDetailVO` | 未实现 |
| `PATCH` | `/api/v1/teacher/forum/replies/{replyId}/visibility` | 教师 | `ForumVisibilityRequest` | `ForumReplyVO` | 未实现 |
| `PATCH` | `/api/v1/admin/forum/topics/{topicId}/visibility` | 管理员 | `ForumVisibilityRequest` | `ForumTopicDetailVO` | 未实现 |
| `PATCH` | `/api/v1/admin/forum/replies/{replyId}/visibility` | 管理员 | `ForumVisibilityRequest` | `ForumReplyVO` | 未实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/warnings/generation` | 教师 | `GenerateCourseWarningsRequest` | `WarningGenerationResultVO` | 未实现 |
| `GET` | `/api/v1/student/warnings` | 学生 | `WarningListQuery` | `PageResponse<LearningWarningVO>` | 未实现 |
| `GET` | `/api/v1/student/warnings/{warningId}` | 学生 | 无 | `LearningWarningVO` | 未实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/warnings` | 教师 | `WarningListQuery` | `PageResponse<LearningWarningVO>` | 未实现 |
| `GET` | `/api/v1/teacher/warnings/{warningId}` | 教师 | 无 | `LearningWarningVO` | 未实现 |
| `POST` | `/api/v1/teacher/warnings/{warningId}/handle` | 教师 | `WarningHandleRequest` | `LearningWarningVO` | 未实现 |

论坛可见性使用 `VISIBLE/HIDDEN`；预警状态使用 `OPEN/HANDLED/IGNORED`，预警由规则生成，AI 不得直接关闭预警。

### 4.3 考试、题库、公告与管理员治理

| 方法 | 路径 | 角色 | 请求 / 成功响应 `data` | 状态 |
|---|---|---|---|---|
| `GET/POST` | `/api/v1/teacher/courses/{courseId}/question-banks` | 教师 | `QuestionBankListQuery`、`CreateQuestionBankRequest` / `PageResponse<QuestionBankVO>`、`QuestionBankVO` | 已实现 |
| `GET/PUT/DELETE` | `/api/v1/teacher/question-banks/{bankId}` | 教师 | `UpdateQuestionBankRequest` / `QuestionBankVO`、空数据 | 已实现 |
| `GET/POST` | `/api/v1/teacher/question-banks/{bankId}/questions` | 教师 | `QuestionListQuery`、`CreateQuestionRequest` / `PageResponse<QuestionVO>`、`QuestionVO` | 已实现 |
| `GET/PUT/DELETE` | `/api/v1/teacher/questions/{questionId}` | 教师 | `UpdateQuestionRequest` / `QuestionVO`、空数据 | 已实现 |
| `GET/POST` | `/api/v1/teacher/courses/{courseId}/exams` | 教师 | `ExamListQuery`、`CreateExamRequest` / `PageResponse<ExamVO>`、`ExamVO` | 已实现 |
| `GET/PUT/DELETE` | `/api/v1/teacher/exams/{examId}` | 教师 | `UpdateExamRequest` / `ExamVO`、空数据 | 已实现 |
| `POST` | `/api/v1/teacher/exams/{examId}/papers` | 教师 | `CreateExamPaperRequest` / `ExamPaperVO` | 已实现 |
| `GET/PUT/DELETE` | `/api/v1/teacher/exam-papers/{paperId}` | 教师 | `UpdateExamPaperRequest` / `ExamPaperVO`、空数据 | 已实现 |
| `POST` | `/api/v1/teacher/exam-papers/{paperId}/publish` | 教师 | 无 / `ExamPaperVO` | 已实现 |
| `GET` | `/api/v1/student/courses/{courseId}/exams` | 学生 | `page`、`size`、`keyword` / `PageResponse<StudentExamListItemVO>` | 已实现，仅返回已发布考试 |
| `POST` | `/api/v1/student/exams/{examId}/attempts` | 学生 | 开始考试 / 字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/student/exam-attempts/{attemptId}/submit` | 学生 | 提交答卷 / 字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/teacher/courses/{courseId}/announcements` | 教师 | 发布课程公告 / 字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/admin/announcements` | 管理员 | 发布系统公告 / 字段待设计 | 未实现，字段待设计 |
| `GET/POST` | `/api/v1/admin/users` | 管理员 | 用户查询和管理 / 字段待设计 | 未实现，字段待设计 |
| `GET/POST` | `/api/v1/admin/course-categories` | 管理员 | 课程分类查询和管理 / 字段待设计 | 未实现，字段待设计 |
| `GET` | `/api/v1/admin/statistics` | 管理员 | 统计看板数据 / 字段待设计 | 未实现，字段待设计 |
| `GET` | `/api/v1/admin/ai-management/status` | 管理员 | AI 运行状态 / 字段待设计 | 未实现，字段待设计 |

题库和题目字段：

| DTO / VO | 字段 |
|---|---|
| `QuestionBankListQuery` | `page`、`size`、`keyword`、`status` |
| `CreateQuestionBankRequest` | `name`、`description`；新题库固定为 `ACTIVE` |
| `UpdateQuestionBankRequest` | `name`、`description`、`status`（`ACTIVE/ARCHIVED`）、`version` |
| `QuestionBankVO` | `bankId`、`courseId`、`name`、`description`、`status`、`version` |
| `QuestionListQuery` | `page`、`size`、`keyword`、`questionType`、`difficulty`、`status` |
| `CreateQuestionRequest` | `questionType`、`stem`、`analysis`、`difficulty`、`score`、`options`；新题目固定为 `ACTIVE` |
| `UpdateQuestionRequest` | `questionType`、`stem`、`analysis`、`difficulty`、`score`、`options`、`version`；`status` 可选，未传时保留原状态 |
| `QuestionOptionRequest` | `label`、`content`、`correct`、`sortOrder` |
| `QuestionVO` | `questionId`、`bankId`、`courseId`、`questionType`、`stem`、`analysis`、`difficulty`、`score`、`status`、`options`、`version` |

考试和试卷字段：

| DTO / VO | 字段 |
|---|---|
| `ExamListQuery` | `page`、`size`、`keyword`、`status`；学生列表忽略 `status`，仅查询已发布考试 |
| `CreateExamRequest` | `title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore`；新考试固定为 `DRAFT` |
| `UpdateExamRequest` | `title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore`、`version` |
| `ExamVO` | `examId`、`courseId`、`title`、`description`、`status`、`startAt`、`endAt`、`durationMinutes`、`totalScore`、`version` |
| `CreateExamPaperRequest`、`UpdateExamPaperRequest` | `title`、`questions`；更新时额外携带 `version` |
| `ExamPaperQuestionRequest` | `questionId`、`questionOrder`、`score` |
| `ExamPaperVO` | `paperId`、`examId`、`courseId`、`title`、`totalScore`、`status`、`questions`、`version` |
| `StudentExamListItemVO` | `examId`、`courseId`、`title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore` |

规则：教师必须属于课程教师团队，学生必须已选课且课程可学习。选择题至少有两个选项，单选/判断题恰有一个正确项，多选题至少两个正确项，简答题不得携带选项。题目只能用于同一课程的试卷；一份试卷的题目和题号不能重复；发布时试卷总分必须等于考试总分，且一个考试只能发布一份试卷。已发布试卷中的题目不可修改，任何已被试卷引用的题目不可删除。所有更新使用 `version` 做乐观锁校验，冲突返回 `409 RESOURCE_CONFLICT`。

考试和试卷状态使用 `DRAFT/PUBLISHED/CLOSED`。首版以考试安排、题库和试卷草稿为边界；完整在线考试会话和自动阅卷后续实现。

### 4.4 AI 接口

| 方法 | 路径 | 角色 | 请求 / 响应 | 状态 |
|---|---|---|---|---|
| `POST` | `/api/v1/ai/courses/{courseId}/qa/stream` | 学生/教师 | 请求字段待设计 / `text/event-stream` | 未实现，字段待设计 |
| `POST` | `/api/v1/ai/lessons/{lessonId}/summary-draft` | 教师 | 请求字段待设计 / JSON 草稿字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/ai/submissions/{submissionId}/comment-draft` | 教师 | 请求字段待设计 / JSON 草稿字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/ai/warnings/{warningId}/explanation` | 教师 | 请求字段待设计 / JSON 草稿字段待设计 | 未实现，字段待设计 |
| `POST` | `/api/v1/ai/exams/paper-suggestions` | 教师 | 请求字段待设计 / JSON 建议字段待设计 | 未实现，字段待设计 |

SSE 事件固定为 `meta`、`delta`、`citation`、`done`、`error`。AI 只能返回回答、建议、草稿和引用，正式业务数据必须由 Biz 服务在人工确认后写入。

### 4.5 作业、成绩、论坛与预警的数据格式

本节是未实现接口的**设计契约**，不是当前 Java 代码；实现接口时，后端必须创建同名 DTO/VO，并让字段与本节保持一致。`datetime` 为 RFC 3339 字符串，`decimal` 为 JSON 数字，`?` 表示可为 `null`。

| 请求体类型 | JSON 字段 |
|---|---|
| `AssignmentCreateRequest` | `lessonId?: string`，`title*: string(1-160)`，`description?`，`maxScore*: decimal(>0)`，`openAt?`，`dueAt*: datetime`，`attachments?: AssignmentAttachmentDTO[]` |
| `AssignmentUpdateRequest` | 创建作业全部字段（不含 `lessonId` 可按需保留），另有 `version*: integer` |
| `AssignmentAttachmentDTO` | `name*: string`，`fileKey?`，`fileUrl?`，`fileSize?`，`mimeType?`，`sortOrder*: integer` |
| `SubmissionSaveRequest` | `content?`，`fileKey?`，`fileUrl?`，`version?`；保存草稿时可为空 |
| `SubmissionSubmitRequest` | `content?`，`fileKey?`，`fileUrl?`，`version?`；`content` 或附件地址必须至少有一项 |
| `GradeSubmissionRequest` | `score*: decimal`，`maxScore*: decimal`，`teacherComment?: string(<=1000)`，`aiCommentDraftId?`，`publishNow?: boolean`，`version*: integer` |
| `PublishGradeRequest` | `version*: integer` |
| `ForumTopicCreateRequest` | `title*: string(1-160)`，`content*: string(1-5000)` |
| `ForumReplyCreateRequest` | `parentReplyId?: string`，`content*: string(1-3000)` |
| `ForumVisibilityRequest` | `visible*: boolean`，`reason?`，`version*: integer` |
| `GenerateCourseWarningsRequest` | `warningTypes?: string[]`（`PROGRESS_LAG/MISSING_ASSIGNMENT/LOW_SCORE`），`studentId?`，`dryRun?: boolean`（默认 `false`） |
| `WarningHandleRequest` | `action*: HANDLED | IGNORED`，`remark?`，`version*: integer` |
| `GradeListQuery` | `courseId?`，`sourceType?`（MVP 为 `ASSIGNMENT`），`status?`，`page=1`，`size=20`（最大 100） |
| `WarningListQuery` | `courseId?`，`studentId?`，`warningType?`，`warningLevel?`，`warningStatus?`，`page=1`，`size=20`（最大 100） |

| 响应 `data` 类型 | 字段 |
|---|---|
| `AssignmentDetailVO` | `assignmentId`，`courseId`，`lessonId?`，`title`，`description?`，`maxScore`，`assignmentStatus: DRAFT|PUBLISHED|CLOSED`，`availabilityStatus: NOT_OPEN|OPEN|OVERDUE|CLOSED`，`openAt?`，`dueAt`，`publishedAt?`，`attachments: AssignmentAttachmentDTO[]`，`version` |
| `StudentAssignmentListItemVO` | `assignmentId`，`courseId`，`title`，`maxScore`，`availabilityStatus`，`dueAt`，`submissionStatus?`，`submittedAt?`，`graded: boolean` |
| `StudentAssignmentDetailVO` | `assignment: AssignmentDetailVO`，`submission?: SubmissionDetailVO` |
| `SubmissionDetailVO` | `submissionId`，`assignmentId`，`courseId`，`studentId`，`attemptNo`，`content?`，`fileKey?`，`fileUrl?`，`submissionStatus`，`submittedAt?`，`score?`，`gradedAt?`，`publishedAt?`，`version` |
| `TeacherSubmissionGradeVO` | `submissionId`，`assignmentId`，`courseId`，`studentId`，`studentName?`，`submissionStatus`，`submittedAt?`，`score?`，`maxScore`，`teacherComment?`，`gradeStatus?`，`publishedAt?`，`version` |
| `StudentGradeVO` | `gradeId`，`courseId`，`assignmentId`，`assignmentTitle`，`score`，`maxScore`，`scoreRate`，`teacherComment?`，`publishedAt` |
| `AssignmentStatisticsVO` | `assignmentId`，`courseId`，`totalStudentCount`，`submittedCount`，`missingCount`，`gradedCount`，`publishedGradeCount`，`averageScore?`，`lowScoreCount` |
| `ForumTopicListItemVO` | `topicId`，`courseId`，`title`，`authorId`，`authorName?`，`status: VISIBLE|HIDDEN`，`pinned: boolean`，`replyCount`，`lastRepliedAt?`，`createdAt`，`version` |
| `ForumTopicDetailVO` | `topicId`，`courseId`，`title`，`content`，`authorId`，`authorName?`，`status`，`createdAt`，`version` |
| `ForumReplyVO` | `replyId`，`topicId`，`courseId`，`authorId`，`authorName?`，`parentReplyId?`，`content`，`status: VISIBLE|HIDDEN`，`createdAt`，`version` |
| `LearningWarningVO` | `warningId`，`courseId`，`studentId`，`warningType`，`warningLevel: LOW|MEDIUM|HIGH`，`warningStatus: OPEN|HANDLED|IGNORED`，`summary`，`suggestion?`，`aiExplanationDraftId?`，`generatedAt`，`handledBy?`，`handledAt?`，`evidences: WarningEvidenceVO[]`，`version` |
| `WarningEvidenceVO` | `evidenceId`，`evidenceType`，`sourceId?`，`metricCode?`，`metricValue?`，`description` |
| `WarningGenerationResultVO` | `createdCount`，`skippedCount`，`warnings: LearningWarningVO[]`（`dryRun=true` 时为预览） |

完整示例：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "assignmentId": "31001",
    "courseId": "21001",
    "title": "第一章课后练习",
    "maxScore": 100,
    "assignmentStatus": "PUBLISHED",
    "availabilityStatus": "OPEN",
    "dueAt": "2026-09-15T23:59:59+08:00",
    "attachments": [],
    "version": 0
  },
  "errors": [],
  "traceId": "a6d8e30f",
  "timestamp": "2026-07-10T11:00:00Z"
}
```

## 5. 内部接口

| 方法 | 路径 | 调用方 | 提供方 | 状态 |
|---|---|---|---|---|
| `POST` | `/_internal/v1/ai-context/course` | `edu-ai-service` | `edu-biz-service` | 已实现，内部接口 |

该接口由 `edu-feign-api` 的 `BizAiContextFeignClient` 调用，Gateway 和前端都不得暴露或调用它。

## 6. 错误码与维护规则

| HTTP 状态 | 错误码示例 | 前端处理 |
|---:|---|---|
| 400 | `PARAM_VALIDATION_ERROR` | 展示字段错误，不回显密码、Token、作业全文或考试答案 |
| 401 | `UNAUTHORIZED`、`TOKEN_EXPIRED` | 清理登录态并跳转登录页 |
| 403 | `FORBIDDEN` | 显示无权限，不泄露他人资源内容 |
| 404 | `RESOURCE_NOT_FOUND` | 显示资源不存在或已不可访问 |
| 409 | `RESOURCE_CONFLICT`、`OPERATION_NOT_ALLOWED` | 刷新数据或提示状态冲突 |
| 429 | `AI_RATE_LIMITED` | 保留用户输入，按 `Retry-After` 重试 |
| 503 | `AI_SERVICE_UNAVAILABLE` | 保留用户输入，提示稍后重试 |

本文是项目唯一 API 文档。已实现接口以当前 Controller、DTO、VO 为最终事实来源；未实现接口以本文的路径、角色和状态规则为准。新增、删除或修改任何接口时，必须在同一提交中更新本文的状态和字段说明。
