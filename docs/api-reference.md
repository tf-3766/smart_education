# 前后端 API 参考

> 状态：2026-07-11
> 网关地址：`http://localhost:18080`
> 本文用于前后端联调；已实现接口的字段以当前 Controller DTO/VO 为准，未实现接口的字段必须在实现前补充到本文。

## 1. 使用规则

所有公开接口通过 Gateway 的 `/api/v1` 访问。除登录和注册外，携带：

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

公开 API 响应中的业务 ID 一律按字符串处理；路径、查询和请求体中的 `Long` ID 可使用十进制字符串，JSON 请求体也可使用整数。时间使用 RFC 3339；分页查询使用 `page`、`size`、`keyword`、`status` 等参数。内部 Feign 契约的 ID 类型以第 5 节为准。

## 2. 接口状态

| 标记 | 含义 | 前端处理方式 |
|---|---|---|
| `已实现` | 当前后端 Controller 已提供 | 可直接联调 |
| `契约待实现` | 文档/OpenAPI 已定义，但没有 Controller | 只能用 Mock，不能请求真实后端 |
| `内部接口` | 仅供服务间 Feign 调用 | 前端不得调用 |

当前公开可联调的是登录注册、教师审核、超级管理员授权、文件与头像、课程、课程内容、选课、学习进度、课程审核、作业、成绩、论坛、预警、考试题库与答题、公告、课程分类和管理统计。AI 课程问答与课时摘要雏形可联调；未配置模型时返回框架模式，不生成伪答案。

## 3. 已实现公开接口

### 3.1 认证

| 方法 | 路径 | 角色 | 请求 | 响应 |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | 匿名 | `LoginRequest` | `LoginVO` |
| `POST` | `/api/v1/auth/register` | 匿名 | `RegisterRequest` | `RegistrationVO`；学生 201，教师待审核 202 |
| `GET` | `/api/v1/auth/me` | 已登录 | 无 | `CurrentUserVO` |
| `POST` | `/api/v1/auth/logout` | 已登录 | 无 | `LogoutVO` |
| `PUT` | `/api/v1/auth/me/avatar` | 已登录 | `UpdateAvatarRequest` | `CurrentUserVO` |

登录示例：

```json
{"username":"student","password":"123456"}
```

注册示例：

```json
{
  "username": "new.student",
  "password": "Student2026",
  "displayName": "新学生",
  "role": "STUDENT"
}
```

公开注册的 `role` 只接受 `STUDENT` 或 `TEACHER`。用户名保存前会去除首尾空白并转为小写。学生账号以 `ENABLED` 创建，`RegistrationVO.login` 返回登录令牌；教师账号以 `PENDING` 创建，`login=null`，必须由超级管理员审核后才能登录。普通注册密码必须为 8～128 个字符并同时包含字母和数字；Bootstrap 演示账号的固定密码不经过注册接口。本地 Bootstrap 账号为 `student/123456`、`teacher/t123456`、`teacher2/t123456`、`admin/admin123`，数据库仅保存 BCrypt 哈希；这些固定凭据只用于本地开发和验收，生产环境不得保留。

### 3.2 文件上传与访问

| 方法 | 路径 | 角色 | 请求 | 响应 |
|---|---|---|---|---|
| `POST` | `/api/v1/files` | 已登录 | `multipart/form-data`：`file*`、`purpose?` | `StoredFileVO`，201 |
| `GET` | `/api/v1/files/{fileId}` | 文件所有者、管理员或有业务资源访问权的用户 | 无 | `StoredFileVO` |
| `GET` | `/api/v1/files/{fileId}/content` | 同上 | 无 | 文件流，不使用 `ApiResponse` 包裹 |
| `DELETE` | `/api/v1/files/{fileId}` | 文件所有者或管理员 | 无 | `void`；被业务数据引用时返回 409 |

`purpose` 可取 `AVATAR/COURSE_MATERIAL/ASSIGNMENT_ATTACHMENT/SUBMISSION/GENERAL`。默认单文件上限为 50MB，可通过 `FILE_MAX_SIZE` 配置；头像仅接受 JPEG、PNG、WebP 和 GIF。数据库只保存文件元数据和对象键，文件内容由可配置的本地目录保存。业务请求使用托管文件时只传 `fileId`；使用外部文件时传 `fileKey` 和/或 `fileUrl`，两种模式同时提交返回 `400 PARAM_VALIDATION_ERROR`。

### 3.3 教师课程管理

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

### 3.4 教师课程内容管理

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

### 3.5 学生课程与学习

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

### 3.6 管理员课程审核

| 方法 | 路径 | 请求 | 响应 |
|---|---|---|---|
| `GET` | `/api/v1/admin/course-reviews` | `CourseListQuery` | `PageResponse<CourseReviewListItemVO>` |
| `GET` | `/api/v1/admin/course-reviews/{courseId}` | 路径参数 | `CourseReviewDetailVO` |
| `POST` | `/api/v1/admin/course-reviews/{courseId}/approve` | `ReviewCourseRequest` | `CourseReviewVO` |
| `POST` | `/api/v1/admin/course-reviews/{courseId}/reject` | `RejectCourseRequest` | `CourseReviewVO` |

### 3.7 超级管理员用户授权

| 方法 | 路径 | 角色 | 请求 | 响应 |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/users` | 超级管理员 | `AdminUserQuery` | `PageResponse<AdminUserVO>` |
| `PUT` | `/api/v1/admin/users/{userId}/administrator` | 超级管理员 | 无 | `AdminUserVO` |
| `DELETE` | `/api/v1/admin/users/{userId}/administrator` | 超级管理员 | 无 | `AdminUserVO` |
| `PUT` | `/api/v1/admin/users/{userId}/teacher-approval` | 超级管理员 | 无 | `AdminUserVO`，通过教师申请 |
| `DELETE` | `/api/v1/admin/users/{userId}/teacher-approval` | 超级管理员 | 无 | `AdminUserVO`，拒绝教师申请 |

Bootstrap SQL 默认创建一个超级管理员账号 `admin`。该账号同时拥有 `ADMIN` 和 `SUPER_ADMIN`，其中 `SUPER_ADMIN` 仅由初始化脚本授予，没有公开授予接口。超级管理员只能把已启用的学生或教师设为普通管理员；普通管理员保留原有角色，但不能继续授予管理员。超级管理员自身的 `ADMIN` 身份不可撤销。

教师申请被拒绝后状态为 `REJECTED`，不能登录，也不能再次执行批准或拒绝；如需重新申请，应使用新账号或由后续用户治理流程处理。审核接口仅允许超级管理员调用。

JWT 是无状态令牌。授予或撤销管理员后，被操作用户需要重新登录，新的角色和权限才会进入新 Token；旧 Token 最迟在当前 JWT 到期后失效。

### 3.8 已实现接口的数据格式

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
| `RegisterRequest` | `username*: string(3-64, A-Z/a-z/0-9/._-)`，`password*: string(8-128，必须同时包含字母和数字)`，`displayName*: string(1-128)`，`role*: STUDENT | TEACHER` |
| `CreateCourseRequest` | `courseCode*: string(1-64, A-Z/a-z/0-9/._-)`，`name*: string(1-160)`，`summary?: string(<=4000)`，`coverUrl?: string(<=1024)`，`categoryId?: number`，`term?: string(<=32)`，`department?: string(<=128)`，`credit?: number(0-99.99)`，`enrollmentOpenAt?`，`enrollmentCloseAt?`，`startAt?`，`endAt?` |
| `UpdateCourseRequest` | `name*: string(1-160)`，`summary?: string(<=4000)`，`coverUrl?: string(<=1024)`，`categoryId?: number`，`term?: string(<=32)`，`department?: string(<=128)`，`credit?: number(0-99.99)`，`enrollmentOpenAt?`，`enrollmentCloseAt?`，`startAt?`，`endAt?`，`version*: number(>=0)` |
| `AddCourseTeacherRequest` | `teacherId*: number(>0)` |
| `CreateChapterRequest` | `title*: string(1-120)`，`description?: string(<=2000)`，`sortOrder*: number(0-100000)` |
| `UpdateChapterRequest` | `title*: string(1-120)`，`description?: string(<=2000)`，`sortOrder*: number(0-100000)`，`version*: number(>=0)` |
| `CreateLessonRequest` | `courseId?: number(>0)`，`title*: string(1-160)`，`contentType*: string`，`content?: string(<=60000)`，`videoUrl?: string(<=1024)`，`estimatedMinutes?: number(1-10000)`，`sortOrder*: number(0-100000)`，`unlockType*: string`，`unlockAt?: RFC3339` |
| `UpdateLessonRequest` | `courseId?: number(>0)`，`title*: string(1-160)`，`contentType*: string`，`content?: string(<=60000)`，`videoUrl?: string(<=1024)`，`estimatedMinutes?: number(1-10000)`，`sortOrder*: number(0-100000)`，`unlockType*: string`，`unlockAt?: RFC3339`，`version*: number(>=0)` |
| `CreateCourseMaterialRequest` | `chapterId?: number`，`lessonId?: number`，`name*: string(1-160)`，`materialType*: string`，`fileId?: number(>0)`，`fileKey?: string(<=512)`，`fileUrl?: string(<=1024)`，`fileSize?: number(>=0)`，`mimeType?: string(<=128)`，`visibility*: string`，`status?: string`，`sortOrder*: number(0-100000)`；托管模式只传 `fileId`，外部模式传 `fileKey/fileUrl`，两种模式不可混用 |
| `UpdateCourseMaterialRequest` | `chapterId?: number`，`lessonId?: number`，`name*: string(1-160)`，`materialType*: string`，`fileId?: number(>0)`，`fileKey?: string(<=512)`，`fileUrl?: string(<=1024)`，`fileSize?: number(>=0)`，`mimeType?: string(<=128)`，`visibility*: string`，`status*: string`，`sortOrder*: number(0-100000)`，`version*: number(>=0)`；托管与外部文件模式不可混用 |
| `ReviewCourseRequest` | `remark?: string(<=500)`；审批接口允许不传请求体 |
| `RejectCourseRequest` | `reason*: string(1-500)` |

时间字段应传带时区的 ISO 8601/RFC 3339 值，例如 `"2026-09-01T08:00:00+08:00"`。
`version` 是乐观锁版本号，前端必须使用详情或列表最新返回值回传。枚举字段必须传后端枚举的 `code`，不要传中文 `label`。

`GET` 列表接口使用查询参数，不传时使用默认值：

| 查询类型 | 可用参数 |
|---|---|
| `AdminUserQuery` | `page=1`，`size=20`（1-100），`keyword?`（<=100，匹配用户名或显示名称），`status?: PENDING/ENABLED/DISABLED/REJECTED` |
| `CourseListQuery` | `page=1`，`size=20`（1-100），`keyword?`（<=100），`status?`，`reviewStatus?`，`enrollmentStatus?`，`term?`（<=32），`categoryId?`，`sort=createdAt,desc`（<=32） |
| `CourseMaterialListQuery` | `page=1`，`size=20`（1-100），`keyword?`，`status?`，`visibility?` |

`CourseListQuery` 是共享绑定类型，但各端点实际生效字段不同：教师课程列表使用 `keyword/status/reviewStatus/term/categoryId/sort`；学生课程目录使用 `keyword/status/term/categoryId`，并始终限制为审核通过、已发布或进行中且处于选课时间窗的课程；学生“我的课程”另支持 `enrollmentStatus`；管理员审核列表使用 `keyword/reviewStatus/term`。未列出的共享字段不会改变该端点结果，前端不应依赖它们。

`PageResponse<T>` 的 `data` 格式固定为：

```json
{"records": [], "page": 1, "size": 20, "total": 0, "totalPages": 0}
```

下表是已实现接口中 `data` 的字段结构。`CodeLabel` 固定为
`{"code":"PUBLISHED","label":"已发布"}`；`[]` 表示数组，`?` 表示字段可为 `null`。

| `data` 类型 | 字段 |
|---|---|
| `LoginVO` | `accessToken`，`tokenType`，`expiresIn`，`expiresAt`，`user: CurrentUserVO`，`roles: string[]`，`permissions: string[]` |
| `RegistrationVO` | `userId`，`username`，`displayName`，`role`，`userStatus: ENABLED|PENDING`，`approvalRequired: boolean`，`login?: LoginVO` |
| `CurrentUserVO` | `userId`，`username`，`displayName`，`avatarFileId?`，`avatarUrl?`，`activeRole`，`roles: string[]`，`permissions: string[]`，`version` |
| `AdminUserVO` | `userId`，`username`，`displayName`，`userStatus`，`roles: string[]`，`superAdministrator: boolean`，`createdAt`，`version` |
| `UpdateAvatarRequest` | `fileId?: number(>0)`，`version*: number(>=0)`；`fileId=null` 表示移除头像 |
| `StoredFileVO` | `fileId`，`originalName`，`objectKey`，`accessUrl`，`fileSize`，`mimeType`，`sha256`，`purpose`，`uploadedAt`，`version` |
| `LogoutVO` | `mode`，`serverSideRevoked: boolean` |
| `CourseDetailVO` | `courseId`，`courseCode`，`name`，`summary?`，`coverUrl?`，`categoryId?`，`term?`，`department?`，`credit?`，`ownerTeacherId`，`ownerTeacherName`，`status: CodeLabel`，`reviewStatus: CodeLabel`，`enrollmentOpenAt?`，`enrollmentCloseAt?`，`startAt?`，`endAt?`，`latestReviewReason?`，`version` |
| `TeacherCourseListItemVO` | `courseId`，`courseCode`，`name`，`term?`，`ownerTeacherId`，`ownerTeacherName`，`status: CodeLabel`，`reviewStatus: CodeLabel`，`startAt?`，`endAt?`，`updatedAt` |
| `StudentCourseListItemVO` | `courseId`，`courseCode`，`name`，`summary?`，`coverUrl?`，`term?`，`credit?`，`ownerTeacherName`，`status: CodeLabel`，`enrollmentStatus: CodeLabel`，`enrollable: boolean`，`startAt?`，`endAt?` |
| `CourseTeacherVO` | `relationId`，`courseId`，`teacherId`，`teacherName`，`role: CodeLabel`，`version` |
| `ChapterDetailVO` | `chapterId`，`courseId`，`title`，`description?`，`sortOrder`，`status: CodeLabel`，`publishedAt?`，`version` |
| `LessonDetailVO` | `lessonId`，`courseId`，`chapterId`，`title`，`contentType: CodeLabel`，`content?`，`videoUrl?`，`estimatedMinutes?`，`sortOrder`，`status: CodeLabel`，`unlockType: CodeLabel`，`unlockAt?`，`publishedAt?`，`version` |
| `CourseMaterialVO` | `materialId`，`courseId`，`chapterId?`，`lessonId?`，`name`，`materialType: CodeLabel`，`fileId?`，`fileKey?`，`fileUrl?`，`fileSize?`，`mimeType?`，`visibility: CodeLabel`，`status: CodeLabel`，`sortOrder`，`version` |
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

## 4. 扩展业务接口

本章同时记录已完成的 Backend A/B 扩展业务接口和字段尚未冻结的后续接口。前端可直接联调标为“已实现”的接口，其他接口继续使用 Mock。

### 4.1 作业、提交与成绩（已实现）

| 方法 | 路径 | 角色 | 请求体或查询 | 成功响应 `data` | 状态 |
|---|---|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | `page/size/keyword/status/sort` | `PageResponse<AssignmentDetailVO>` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | `AssignmentCreateRequest` | `AssignmentDetailVO`，HTTP 201 | 已实现 |
| `PUT` | `/api/v1/teacher/assignments/{assignmentId}` | 教师 | `AssignmentUpdateRequest` | `AssignmentDetailVO` | 已实现 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/publish` | 教师 | 无 | `AssignmentDetailVO` | 已实现 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/close` | 教师 | 无 | `AssignmentDetailVO` | 已实现 |
| `GET` | `/api/v1/student/courses/{courseId}/assignments` | 学生 | `page/size/keyword/status/sort` | `PageResponse<StudentAssignmentListItemVO>` | 已实现 |
| `GET` | `/api/v1/student/assignments/{assignmentId}` | 学生 | 无 | `StudentAssignmentDetailVO` | 已实现 |
| `PUT` | `/api/v1/student/assignments/{assignmentId}/submission-draft` | 学生 | `SubmissionSaveRequest` | `SubmissionDetailVO` | 已实现 |
| `POST` | `/api/v1/student/assignments/{assignmentId}/submissions` | 学生 | `SubmissionSubmitRequest` | `SubmissionDetailVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/submissions` | 教师 | `page/size/submissionStatus` | `PageResponse<TeacherSubmissionGradeVO>` | 已实现 |
| `POST` | `/api/v1/teacher/submissions/{submissionId}/grade` | 教师 | `GradeSubmissionRequest` | `TeacherSubmissionGradeVO` | 已实现 |
| `POST` | `/api/v1/teacher/grades/{gradeId}/publication` | 教师 | `PublishGradeRequest` | `TeacherSubmissionGradeVO` | 已实现 |
| `GET` | `/api/v1/student/grades` | 学生 | `GradeListQuery` | `PageResponse<StudentGradeVO>` | 已实现 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/statistics` | 教师 | 无 | `AssignmentStatisticsVO` | 已实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/grade-statistics` | 教师 | 无 | `CourseGradeStatisticsVO` | 已实现 |

作业状态为 `DRAFT/PUBLISHED/CLOSED`；提交状态为 `DRAFT/SUBMITTED/RETURNED/GRADED`；成绩发布状态首版为 `DRAFT/PUBLISHED`。正式提交必须有文本内容或附件，逾期、重复提交和版本冲突返回 409。已发布作业一旦存在提交，`lessonId/maxScore/openAt/dueAt` 即冻结，修改这些成绩口径字段返回 `409 OPERATION_NOT_ALLOWED`；标题、描述和附件仍可在正确 `version` 下更新。

### 4.2 论坛与学习预警（已实现）

| 方法 | 路径 | 角色 | 请求体或查询 | 成功响应 `data` | 状态 |
|---|---|---|---|---|---|
| `GET` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | `page/size/status/keyword` | `PageResponse<ForumTopicListItemVO>` | 已实现 |
| `POST` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | `ForumTopicCreateRequest` | `ForumTopicDetailVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/student/forum/topics/{topicId}` | 学生 | 无 | `ForumTopicDetailVO` | 已实现 |
| `GET` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | `page/size/status` | `PageResponse<ForumReplyVO>` | 已实现 |
| `POST` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | `ForumReplyCreateRequest` | `ForumReplyVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/forum/topics` | 教师 | `page/size/status/keyword` | `PageResponse<ForumTopicListItemVO>` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/forum/topics` | 教师 | `ForumTopicCreateRequest` | `ForumTopicDetailVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/forum/topics/{topicId}` | 教师 | 无 | `ForumTopicDetailVO` | 已实现 |
| `GET` | `/api/v1/teacher/forum/topics/{topicId}/replies` | 教师 | `ForumListQuery` | `PageResponse<ForumReplyVO>` | 已实现 |
| `POST` | `/api/v1/teacher/forum/topics/{topicId}/replies` | 教师 | `ForumReplyCreateRequest` | `ForumReplyVO`，HTTP 201 | 已实现 |
| `PATCH` | `/api/v1/teacher/forum/topics/{topicId}/visibility` | 教师 | `ForumVisibilityRequest` | `ForumTopicDetailVO` | 已实现 |
| `PATCH` | `/api/v1/teacher/forum/replies/{replyId}/visibility` | 教师 | `ForumVisibilityRequest` | `ForumReplyVO` | 已实现 |
| `PATCH` | `/api/v1/admin/forum/topics/{topicId}/visibility` | 管理员 | `ForumVisibilityRequest` | `ForumTopicDetailVO` | 已实现 |
| `PATCH` | `/api/v1/admin/forum/replies/{replyId}/visibility` | 管理员 | `ForumVisibilityRequest` | `ForumReplyVO` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/warnings/generation` | 教师 | `GenerateCourseWarningsRequest` | `WarningGenerationResultVO` | 已实现 |
| `GET` | `/api/v1/student/warnings` | 学生 | `WarningListQuery` | `PageResponse<LearningWarningVO>` | 已实现 |
| `GET` | `/api/v1/student/warnings/{warningId}` | 学生 | 无 | `LearningWarningVO` | 已实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/warnings` | 教师 | `WarningListQuery` | `PageResponse<LearningWarningVO>` | 已实现 |
| `GET` | `/api/v1/teacher/warnings/{warningId}` | 教师 | 无 | `LearningWarningVO` | 已实现 |
| `POST` | `/api/v1/teacher/warnings/{warningId}/handle` | 教师 | `WarningHandleRequest` | `LearningWarningVO` | 已实现 |

论坛可见性使用 `VISIBLE/HIDDEN`；学生列表的基础范围始终为 `VISIBLE`，因此学生传 `status=HIDDEN` 返回空列表而不会暴露隐藏内容。预警状态使用 `OPEN/HANDLED/IGNORED`，预警由规则生成，进度预警只统计已发布章节中已发布且当前已解锁的课时，AI 不得直接关闭预警。

### 4.3 考试、题库、公告与管理员治理

| 方法 | 路径 | 角色 | 请求 / 成功响应 `data` | 状态 |
|---|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/question-banks` | 教师 | `QuestionBankListQuery` / `PageResponse<QuestionBankVO>` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/question-banks` | 教师 | `CreateQuestionBankRequest` / `QuestionBankVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/question-banks/{bankId}` | 教师 | 无 / `QuestionBankVO` | 已实现 |
| `PUT` | `/api/v1/teacher/question-banks/{bankId}` | 教师 | `UpdateQuestionBankRequest` / `QuestionBankVO` | 已实现 |
| `DELETE` | `/api/v1/teacher/question-banks/{bankId}` | 教师 | 无 / `void` | 已实现 |
| `GET` | `/api/v1/teacher/question-banks/{bankId}/questions` | 教师 | `QuestionListQuery` / `PageResponse<QuestionVO>` | 已实现 |
| `POST` | `/api/v1/teacher/question-banks/{bankId}/questions` | 教师 | `CreateQuestionRequest` / `QuestionVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/questions/{questionId}` | 教师 | 无 / `QuestionVO` | 已实现 |
| `PUT` | `/api/v1/teacher/questions/{questionId}` | 教师 | `UpdateQuestionRequest` / `QuestionVO` | 已实现 |
| `DELETE` | `/api/v1/teacher/questions/{questionId}` | 教师 | 无 / `void` | 已实现 |
| `GET` | `/api/v1/teacher/courses/{courseId}/exams` | 教师 | `ExamListQuery` / `PageResponse<ExamVO>` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/exams` | 教师 | `CreateExamRequest` / `ExamVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/exams/{examId}` | 教师 | 无 / `ExamVO` | 已实现 |
| `PUT` | `/api/v1/teacher/exams/{examId}` | 教师 | `UpdateExamRequest` / `ExamVO` | 已实现 |
| `DELETE` | `/api/v1/teacher/exams/{examId}` | 教师 | 无 / `void` | 已实现 |
| `POST` | `/api/v1/teacher/exams/{examId}/papers` | 教师 | `CreateExamPaperRequest` / `ExamPaperVO`，HTTP 201 | 已实现 |
| `GET` | `/api/v1/teacher/exam-papers/{paperId}` | 教师 | 无 / `ExamPaperVO` | 已实现 |
| `PUT` | `/api/v1/teacher/exam-papers/{paperId}` | 教师 | `UpdateExamPaperRequest` / `ExamPaperVO` | 已实现 |
| `DELETE` | `/api/v1/teacher/exam-papers/{paperId}` | 教师 | 无 / `void` | 已实现 |
| `POST` | `/api/v1/teacher/exam-papers/{paperId}/publish` | 教师 | 无 / `ExamPaperVO` | 已实现 |
| `GET` | `/api/v1/student/courses/{courseId}/exams` | 学生 | `page`、`size`、`keyword` / `PageResponse<StudentExamListItemVO>` | 已实现，仅返回已发布考试 |
| `POST` | `/api/v1/student/exams/{examId}/attempts` | 学生 | 无 / `ExamAttemptVO` | 已实现，重试开始未提交考试时幂等 |
| `GET` | `/api/v1/student/exam-attempts/{attemptId}` | 学生 | 无 / `ExamAttemptVO` | 已实现，仅本人可读 |
| `POST` | `/api/v1/student/exam-attempts/{attemptId}/submit` | 学生 | `SubmitExamAttemptRequest` / `ExamAttemptVO` | 已实现 |
| `GET` | `/api/v1/teacher/exams/{examId}/attempts` | 教师 | `ExamAttemptListQuery` / `PageResponse<ExamAttemptVO>` | 已实现 |
| `POST` | `/api/v1/teacher/exam-attempts/{attemptId}/grade` | 教师 | `GradeExamAttemptRequest` / `ExamAttemptVO` | 已实现，用于简答题人工评分 |
| `GET` | `/api/v1/teacher/courses/{courseId}/announcements` | 教师 | `AnnouncementListQuery` / `PageResponse<AnnouncementVO>` | 已实现 |
| `POST` | `/api/v1/teacher/courses/{courseId}/announcements` | 教师 | `CreateAnnouncementRequest` / `AnnouncementVO`，HTTP 201 | 已实现 |
| `POST` | `/api/v1/teacher/announcements/{announcementId}/withdrawal` | 教师 | `WithdrawAnnouncementRequest` / `AnnouncementVO` | 已实现 |
| `GET` | `/api/v1/teacher/announcements` | 教师 | `AnnouncementListQuery` / `PageResponse<AnnouncementVO>` | 已实现，按负责课程与 `ALL/TEACHER` 受众过滤 |
| `GET` | `/api/v1/student/announcements` | 学生 | `AnnouncementListQuery` / `PageResponse<AnnouncementVO>` | 已实现，按选课与受众过滤 |
| `GET` | `/api/v1/admin/announcements` | 管理员 | `AnnouncementListQuery` / `PageResponse<AnnouncementVO>` | 已实现 |
| `POST` | `/api/v1/admin/announcements` | 管理员 | `CreateAnnouncementRequest` / `AnnouncementVO`，HTTP 201 | 已实现 |
| `POST` | `/api/v1/admin/announcements/{announcementId}/withdrawal` | 管理员 | `WithdrawAnnouncementRequest` / `AnnouncementVO` | 已实现 |
| `GET` | `/api/v1/course-categories` | 已登录 | 无 / `List<CourseCategoryVO>` | 已实现，仅启用分类 |
| `GET` | `/api/v1/admin/course-categories` | 管理员 | 无 / `List<CourseCategoryVO>` | 已实现 |
| `POST` | `/api/v1/admin/course-categories` | 管理员 | `CreateCourseCategoryRequest` / `CourseCategoryVO`，HTTP 201 | 已实现 |
| `PUT` | `/api/v1/admin/course-categories/{categoryId}` | 管理员 | `UpdateCourseCategoryRequest` / `CourseCategoryVO` | 已实现 |
| `DELETE` | `/api/v1/admin/course-categories/{categoryId}` | 管理员 | 无 / `void` | 已实现 |
| `GET` | `/api/v1/admin/statistics` | 管理员 | 无 / `AdminStatisticsVO` | 已实现 |

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
| `QuestionOptionVO` | `optionId`、`label`、`content`、`correct`、`sortOrder` |

考试和试卷字段：

| DTO / VO | 字段 |
|---|---|
| `ExamListQuery` | `page`、`size`、`keyword`、`status`；学生列表忽略 `status`，仅查询已发布考试 |
| `CreateExamRequest` | `title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore`；新考试固定为 `DRAFT` |
| `UpdateExamRequest` | `title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore`、`version` |
| `ExamVO` | `examId`、`courseId`、`title`、`description`、`status`、`startAt`、`endAt`、`durationMinutes`、`totalScore`、`version` |
| `CreateExamPaperRequest` | `title*: string(1-160)`，`questions*: ExamPaperQuestionRequest[]`（至少 1 项） |
| `UpdateExamPaperRequest` | `title*: string(1-160)`，`questions*: ExamPaperQuestionRequest[]`（至少 1 项），`version*: integer(>=0)` |
| `ExamPaperQuestionRequest` | `questionId`、`questionOrder`、`score` |
| `ExamPaperVO` | `paperId`、`examId`、`courseId`、`title`、`totalScore`、`status`、`questions`、`version` |
| `ExamPaperQuestionVO` | `questionId`、`questionOrder`、`score`、`questionType`、`stem` |
| `StudentExamListItemVO` | `examId`、`courseId`、`title`、`description`、`startAt`、`endAt`、`durationMinutes`、`totalScore` |
| `ExamAttemptListQuery` | `page`、`size`、`status?: IN_PROGRESS/SUBMITTED/GRADED` |
| `SubmitExamAttemptRequest` | `answers*: ExamAnswerSubmitRequest[]`，`version*: integer`；同一题不能重复 |
| `ExamAnswerSubmitRequest` | `questionId*: string`，`answerContent*: string(1-10000)` |
| `GradeExamAttemptRequest` | `answers*: GradeExamAnswerRequest[]`，`version*: integer` |
| `GradeExamAnswerRequest` | `questionId*: string`，`score*: decimal(>=0)`，`teacherComment?: string(<=1000)` |
| `ExamAttemptVO` | `attemptId`、`examId`、`paperId`、`studentId`、`status`、`startedAt`、`deadlineAt?`、`submittedAt?`、`gradedAt?`、`score?`、`questions: StudentExamQuestionVO[]`、`answers: ExamAnswerVO[]`、`version` |
| `StudentExamQuestionVO` | `questionId`、`questionOrder`、`score`、`questionType`、`stem`、`options: StudentExamOptionVO[]`；不返回正确选项和解析 |
| `StudentExamOptionVO` | `label`、`content`、`sortOrder` |
| `ExamAnswerVO` | `questionId`、`answerContent`、`score?`、`teacherComment?` |
| `CreateAnnouncementRequest` | `title*: string(1-200)`，`content*: string(1-10000)`，`audience*: ALL/STUDENT/TEACHER`；课程公告不接受 `TEACHER` |
| `WithdrawAnnouncementRequest` | `version*: integer` |
| `AnnouncementListQuery` | `page=1`，`size=20`（1-100） |
| `AnnouncementVO` | `announcementId`、`scopeType: COURSE/SYSTEM`、`courseId?`、`title`、`content`、`audience`、`status: PUBLISHED/WITHDRAWN`、`publishedAt`、`withdrawnAt?`、`publisherId`、`version` |
| `CreateCourseCategoryRequest` | `name*: string(1-128)`，`sortOrder*: integer(>=0)`，`enabled*: boolean` |
| `UpdateCourseCategoryRequest` | `name*: string(1-128)`，`sortOrder*: integer(>=0)`，`enabled*: boolean`，`version*: integer(>=0)` |
| `CourseCategoryVO` | `categoryId`、`name`、`sortOrder`、`enabled`、`version` |
| `AdminStatisticsVO` | `totalUsers`、`enabledUsers`、`students`、`teachers`、`administrators`、`totalCourses`、`publishedCourses`、`pendingCourseReviews`、`activeEnrollments`、`publishedAssignments`、`submittedAssignments`、`publishedExams`、`openWarnings`、`publishedAnnouncements` |

规则：教师必须属于课程教师团队，学生必须已选课且课程可学习。选择题至少有两个选项，单选/判断题恰有一个正确项，多选题至少两个正确项，简答题不得携带选项。题目只能用于同一课程的试卷；一份试卷的题目和题号不能重复；发布时试卷总分必须等于考试总分，且一个考试只能发布一份试卷。已发布试卷中的题目不可修改，任何已被试卷引用的题目不可删除。学生只能在考试时间窗内开始一次答题；客观题提交后自动判分，包含简答题时进入 `SUBMITTED` 待教师评分。所有更新使用 `version` 做乐观锁校验，冲突返回 `409 RESOURCE_CONFLICT`。

考试和试卷状态使用 `DRAFT/PUBLISHED/CLOSED`，答题状态使用 `IN_PROGRESS/SUBMITTED/GRADED`。当前实现基础答题、幂等开始、时间窗、客观题判分和简答题人工评分；断点续考、随机抽题、监考和防切屏仍属后续扩展。

### 4.4 AI 接口

| 方法 | 路径 | 角色 | 请求 / 响应 | 状态 |
|---|---|---|---|---|
| `POST` | `/api/v1/ai/courses/{courseId}/qa/stream` | 学生/教师 | `CourseQaRequest` / `text/event-stream` | 框架已实现，已校验 Biz 课程上下文 |
| `POST` | `/api/v1/ai/lessons/{lessonId}/summary-draft` | 教师 | `LessonSummaryRequest` / `AiDraftVO` | 框架已实现，仅返回草稿 |
| `GET` | `/api/v1/ai/admin/status` | 管理员/超级管理员 | 无 / `AiServiceStatusVO` | 已实现 |
| `POST` | `/api/v1/ai/submissions/{submissionId}/comment-draft` | 教师 | 契约待冻结 | 未实现 |
| `POST` | `/api/v1/ai/warnings/{warningId}/explanation` | 教师 | 契约待冻结 | 未实现 |
| `POST` | `/api/v1/ai/exams/paper-suggestions` | 教师 | 契约待冻结 | 未实现 |

| AI DTO / VO | JSON 字段 |
|---|---|
| `CourseQaRequest` | `question*: string(1-2000)`，`lessonId?: string` |
| `LessonSummaryRequest` | `courseId*: string` |
| `AiCitationVO` | `resourceType`，`resourceId`，`title`，`locator` |
| `AiDraftVO` | `requestId`，`draftType`，`businessId`，`content`，`provider`，`model`，`status: DRAFT/FRAMEWORK_ONLY`，`citations: AiCitationVO[]`，`createdAt` |
| `AiServiceStatusVO` | `serviceStatus`，`framework`，`frameworkVersion`，`provider`，`model`，`modelConfigured: boolean`，`vectorStoreConfigured: boolean`，`checkedAt` |
| `AiStreamEvent` | `type`，`requestId`，`data`，`timestamp` |

SSE 事件固定为 `meta`、`delta`、`citation`、`done`、`error`，每个事件的 JSON 数据为 `AiStreamEvent`：`type`、`requestId`、`data`、`timestamp`；同一次请求的全部事件共用一个 `requestId`。`error.data` 为 `{code,message}`，Biz 返回的 401/403/404/409 会保留对应统一错误码，模型或下游不可用才返回 `AI_SERVICE_UNAVAILABLE`。AI 服务基于 Spring AI 1.1.8；默认 `AI_CHAT_PROVIDER=none`，此时 `provider=fallback` 且仅说明模型未配置。配置 `AI_CHAT_PROVIDER=openai`、`OPENAI_API_KEY` 和可选 `OPENAI_BASE_URL/OPENAI_CHAT_MODEL` 后启用 Spring AI `ChatClient`。AI 只能返回回答、建议、草稿和引用，正式业务数据必须由 Biz 服务在人工确认后写入。

### 4.5 作业、成绩、论坛与预警的数据格式

本节是 Backend A 已实现接口的字段契约。`datetime` 为 RFC 3339 字符串，`decimal` 为 JSON 数字，`?` 表示可为 `null`。

| 请求体类型 | JSON 字段 |
|---|---|
| `AssignmentCreateRequest` | `lessonId?: string`，`title*: string(1-160)`，`description?`，`maxScore*: decimal(>0)`，`openAt?`，`dueAt*: datetime`，`attachments?: AssignmentAttachmentRequest[]` |
| `AssignmentUpdateRequest` | `lessonId?: string`，`title*: string(1-160)`，`description?: string(<=10000)`，`maxScore*: decimal(>0)`，`openAt?: datetime`，`dueAt*: datetime`，`attachments?: AssignmentAttachmentRequest[]`，`version*: integer(>=0)` |
| `AssignmentAttachmentRequest` | `name*: string`，`fileId?`，`fileKey?`，`fileUrl?`，`fileSize?`，`mimeType?`，`sortOrder*: integer`；托管模式只传 `fileId`，外部模式传 `fileKey/fileUrl`，两种模式不可混用 |
| `SubmissionSaveRequest` | `content?`，`fileId?`，`fileKey?`，`fileUrl?`，`version?`；保存草稿时可为空；托管与外部附件模式不可混用 |
| `SubmissionSubmitRequest` | `content?`，`fileId?`，`fileKey?`，`fileUrl?`，`version?`；`content` 或附件引用必须至少有一项；托管与外部附件模式不可混用 |
| `GradeSubmissionRequest` | `score*: decimal`，`maxScore*: decimal`，`teacherComment?: string(<=1000)`，`aiCommentDraftId?`，`publishNow?: boolean`，`version*: integer` |
| `PublishGradeRequest` | `version*: integer` |
| `ForumTopicCreateRequest` | `title*: string(1-160)`，`content*: string(1-5000)` |
| `ForumReplyCreateRequest` | `parentReplyId?: string`，`content*: string(1-3000)` |
| `ForumVisibilityRequest` | `visible*: boolean`，`reason?`，`version*: integer` |
| `GenerateCourseWarningsRequest` | `warningTypes?: string[]`（`PROGRESS_LAG/MISSING_ASSIGNMENT/LOW_SCORE`），`studentId?`，`dryRun?: boolean`（默认 `false`） |
| `WarningHandleRequest` | `action*: HANDLED | IGNORED`，`remark?`，`version*: integer` |
| `GradeListQuery` | `courseId?`，`assignmentId?`，`sourceType=ASSIGNMENT`，`status?: DRAFT/PUBLISHED`，`page=1`，`size=20`（最大 100）；学生基础范围只含本人已发布成绩，因此 `status=DRAFT` 返回空列表 |
| `ForumListQuery` | `page=1`，`size=20`（最大 100），`status?: VISIBLE/HIDDEN`，`keyword?: string(<=100)`；回复列表不使用 `keyword` |
| `WarningListQuery` | `courseId?`，`studentId?`，`warningType?`，`warningLevel?`，`warningStatus?`，`page=1`，`size=20`（最大 100）；学生端 `studentId` 与当前身份做交集，不能据此读取他人预警 |
| `AssignmentListQuery` | `page=1`，`size=20`（最大 100），`keyword?`（<=100），`status?: DRAFT/PUBLISHED/CLOSED`，`submissionStatus?: DRAFT/SUBMITTED/RETURNED/GRADED`，`sort=createdAt,desc` |

| 响应 `data` 类型 | 字段 |
|---|---|
| `AssignmentDetailVO` | `assignmentId`，`courseId`，`lessonId?`，`title`，`description?`，`maxScore`，`assignmentStatus: CodeLabel`，`availabilityStatus: CodeLabel`，`openAt?`，`dueAt`，`publishedAt?`，`attachments: AssignmentAttachmentVO[]`，`version` |
| `AssignmentAttachmentVO` | `attachmentId`，`name`，`fileId?`，`fileKey?`，`fileUrl?`，`fileSize?`，`mimeType?`，`sortOrder` |
| `StudentAssignmentListItemVO` | `assignmentId`，`courseId`，`lessonId?`，`title`，`maxScore`，`availabilityStatus: CodeLabel`，`dueAt`，`submissionStatus?: CodeLabel`，`submittedAt?`，`graded: boolean` |
| `StudentAssignmentDetailVO` | `assignment: AssignmentDetailVO`，`submission?: SubmissionDetailVO` |
| `SubmissionDetailVO` | `submissionId`，`assignmentId`，`courseId`，`studentId`，`attemptNo`，`content?`，`fileId?`，`fileKey?`，`fileUrl?`，`submissionStatus: CodeLabel`，`submittedAt?`，`score?`，`teacherComment?`，`aiCommentDraftId?`，`gradedBy?`，`gradedAt?`，`publishedAt?`，`version` |
| `TeacherSubmissionGradeVO` | `submissionId`，`assignmentId`，`courseId`，`studentId`，`studentName?`，`submissionStatus: CodeLabel`，`submittedAt?`，`content?`，`fileKey?`，`fileUrl?`，`score?`，`maxScore`，`teacherComment?`，`aiCommentDraftId?`，`gradedBy?`，`gradedAt?`，`gradeId?`，`gradeStatus?: CodeLabel`，`publishedAt?`，`version`，`gradeVersion?` |
| `StudentGradeVO` | `gradeId`，`courseId`，`assignmentId`，`assignmentTitle`，`score`，`maxScore`，`scoreRate`，`teacherComment?`，`publishedAt` |
| `AssignmentStatisticsVO` | `assignmentId`，`courseId`，`totalStudentCount`，`submittedCount`，`missingCount`，`gradedCount`，`publishedGradeCount`，`averageScore?`，`lowScoreCount` |
| `CourseGradeStatisticsVO` | `courseId`，`assignmentCount`，`publishedAssignmentCount`，`enrolledStudentCount`，`gradedRecordCount`，`publishedGradeCount`，`averageScoreRate?`，`passRate?`，`lowScoreCount`；聚合指标只统计已发布成绩，比率为 0-100 的百分数 |
| `ForumTopicListItemVO` | `topicId`，`courseId`，`title`，`authorId`，`authorName?`，`status: CodeLabel`（code 为 `VISIBLE/HIDDEN`），`pinned: boolean`，`replyCount`，`lastRepliedAt?`，`createdAt`，`version` |
| `ForumTopicDetailVO` | `topicId`，`courseId`，`title`，`content`，`authorId`，`authorName?`，`status: CodeLabel`，`moderationReason?`，`moderatedBy?`，`moderatedAt?`，`createdAt`，`version` |
| `ForumReplyVO` | `replyId`，`topicId`，`courseId`，`authorId`，`authorName?`，`parentReplyId?`，`content`，`status: CodeLabel`（code 为 `VISIBLE/HIDDEN`），`moderationReason?`，`moderatedBy?`，`moderatedAt?`，`createdAt`，`version` |
| `LearningWarningVO` | `warningId`，`courseId`，`studentId`，`studentName?`，`warningType: CodeLabel`，`warningLevel: CodeLabel`（code 为 `LOW/MEDIUM/HIGH`），`warningStatus: CodeLabel`（code 为 `OPEN/HANDLED/IGNORED`），`summary`，`suggestion?`，`aiExplanationDraftId?`，`generatedAt`，`handledBy?`，`handleRemark?`，`handledAt?`，`evidences: WarningEvidenceVO[]`，`version` |
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
    "assignmentStatus": {"code": "PUBLISHED", "label": "已发布"},
    "availabilityStatus": {"code": "OPEN", "label": "开放提交"},
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

该接口由 `edu-feign-api` 的 `BizAiContextFeignClient` 调用，AI 服务必须透传当前用户的 `Authorization` 头。Biz 以 JWT principal 为唯一身份来源，拒绝请求体伪造的 `userId/roleCode`，并重新验证选课/教师关系、课时解锁和资料可见范围。Gateway 和前端都不得暴露或调用它。

内部请求和响应使用 Java `Long`，因此 JSON 中的 ID 是整数，不适用公开 API 的“响应 ID 字符串化”约定：

| 内部类型 | JSON 字段 |
|---|---|
| `AiCourseContextRequest` | `userId*: number`，`roleCode*: string`，`courseId*: number`，`lessonId?: number`，`materialId?: number`，`purpose*: AiContextPurpose`，`traceId?: string` |
| `AiContextPurpose` | `COURSE_QA`、`LESSON_SUMMARY_DRAFT`、`GRADING_COMMENT_DRAFT`、`RISK_EXPLANATION`、`PAPER_SUGGESTION` |
| `AiCourseContextResponse` | `courseId`，`courseCode`，`courseName`，`courseStatus`，`reviewStatus`，`ownerTeacherId`，`teacherMember: boolean`，`enrolled: boolean`，`lessons: AiLessonRef[]`，`materials: AiMaterialRef[]` |
| `AiLessonRef` | `lessonId`，`chapterId`，`title`，`status`，`contentType`，`estimatedMinutes?` |
| `AiMaterialRef` | `materialId`，`chapterId?`，`lessonId?`，`name`，`materialType`，`fileKey?`，`fileUrl?`，`visibility`，`status` |

## 6. 错误码与维护规则

| HTTP 状态 | 错误码示例 | 前端处理 |
|---:|---|---|
| 400 | `PARAM_VALIDATION_ERROR` | 展示字段错误，不回显密码、Token、作业全文或考试答案 |
| 401 | `UNAUTHORIZED`、`TOKEN_EXPIRED` | 清理登录态并跳转登录页 |
| 403 | `FORBIDDEN` | 显示无权限，不泄露他人资源内容 |
| 404 | `RESOURCE_NOT_FOUND` | 显示资源不存在或已不可访问 |
| 409 | `RESOURCE_CONFLICT`、`OPERATION_NOT_ALLOWED` | 刷新数据或提示状态冲突 |
| 409 | `USERNAME_ALREADY_EXISTS` | 提示用户更换注册用户名 |
| 409 | `TEACHER_REGISTRATION_NOT_PENDING` | 刷新教师申请状态，不重复审核 |
| 409 | `SUPER_ADMIN_PROTECTED` | 不允许撤销系统超级管理员的管理员身份 |
| 409 | `FILE_IN_USE` | 文件仍被头像、课程资料或作业数据引用，解除引用后再删除 |
| 400/403/404 | `FILE_TYPE_NOT_ALLOWED`、`FILE_ACCESS_DENIED`、`FILE_NOT_FOUND` | 更换文件类型、检查资源权限或提示文件不存在 |
| 429 | `AI_RATE_LIMITED` | 保留用户输入，按 `Retry-After` 重试 |
| 503 | `AI_SERVICE_UNAVAILABLE` | 保留用户输入，提示稍后重试 |

本文是项目唯一 API 文档。已实现接口以当前 Controller、DTO、VO 为最终事实来源；未实现接口以本文的路径、角色和状态规则为准。新增、删除或修改任何接口时，必须在同一提交中更新本文的状态和字段说明。
