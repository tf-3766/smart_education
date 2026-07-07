# 课程与学习 API 契约

> 所有路径使用 `/api/v1`，响应为 `ApiResponse<T>`；列表 `data` 为 `PageResponse<T>`。Long ID 对外序列化为字符串，时间使用带时区 RFC 3339。

## 1. 通用查询与状态字段

### 1.1 `CourseListQuery`

| 参数 | 类型 | 默认/限制 |
|---|---|---|
| `page` | integer | 默认 1，最小 1 |
| `size` | integer | 默认 20，1–100 |
| `keyword` | string | 可选，最长 100，匹配课程编号/名称 |
| `status` | `CourseStatus` | 可选 |
| `term` | string | 可选，最长 32 |
| `categoryId` | string(Long) | 可选 |
| `sort` | string | `createdAt,desc`、`name,asc`、`startAt,asc` 白名单 |

状态统一返回：

```json
{"code":"PUBLISHED","label":"已发布"}
```

状态 VO 统一为 `CodeLabelVO(code,label)`，禁止不同页面自行翻译同一 code。

### 1.2 通用错误

| 错误码 | HTTP | 场景 |
|---|---:|---|
| `PARAM_VALIDATION_ERROR` | 400 | DTO、查询或跨字段格式错误 |
| `UNAUTHORIZED` / `TOKEN_EXPIRED` | 401 | 未登录、非法或过期 Token |
| `FORBIDDEN` | 403 | 角色或资源范围不允许 |
| `RESOURCE_NOT_FOUND` | 404 | 资源不存在，或按安全策略不向当前用户暴露 |
| `RESOURCE_CONFLICT` | 409 | 唯一关系、重复绑定、乐观锁冲突 |
| `OPERATION_NOT_ALLOWED` | 409 | 当前状态、发布、解锁、选课窗口不允许操作 |

## 2. DTO 与 VO

### 2.1 课程 DTO

- `CreateCourseRequest`：`courseCode`、`name` 必填；`summary/coverUrl/categoryId/term/department/credit` 可选；四个选课/课程时间可选但必须顺序合法。
- `UpdateCourseRequest`：不允许改变 `courseCode/ownerTeacherId`；其余字段同创建请求，并携带 `version`。
- `ReviewCourseRequest`：批准时可选 `remark`，最长 500。
- `RejectCourseRequest`：`reason` 必填，1–500 字。
- `AddCourseTeacherRequest`：`teacherId` 必填且必须具有 TEACHER 角色。

### 2.2 内容与资料 DTO

- `CreateChapterRequest/UpdateChapterRequest`：`title` 1–120，`description` 最长 2000，`sortOrder` 0–100000；更新携带 `version`。
- `CreateLessonRequest/UpdateLessonRequest`：标题 1–160，`contentType`、Markdown `content`、`videoUrl`、`estimatedMinutes`、`sortOrder`、`unlockType/unlockAt`；`SCHEDULED` 必须有 `unlockAt`；更新携带 `version`。请求可选携带 `courseId` 作为前端上下文断言，Service 会与章节所属课程核对，不能用它改变归属。
- `CreateCourseMaterialRequest/UpdateCourseMaterialRequest`：名称、类型、`fileKey/fileUrl`、大小、MIME、visibility、chapterId/lessonId、sortOrder；Service 校验三层归属；更新携带 `version`。

### 2.3 主要 VO

- `TeacherCourseListItemVO`：课程 ID/编号/名称、运行/审核状态、term、owner、开始结束时间、更新时间。
- `StudentCourseListItemVO`：课程 ID/编号/名称、封面、摘要、term、credit、负责人显示名、运行状态、选课状态、是否可选；不含审核备注。
- `CourseDetailVO`：按教师/管理员/学生视图构造；学生视图不含审核内部原因、版本和后台字段。
- `CourseOutlineVO/ChapterOutlineVO/LessonOutlineVO`：课程与已授权章节课时树、解锁和本人完成状态。
- `EnrollmentVO`：courseId/studentId/status/enrolledAt/withdrawnAt。
- `CourseProgressVO`：`totalLessons/availableLessons/completedLessons/progressPercent/lastLessonId/nextLessonId`。
- `LearningRecordVO`：课时、本人的状态与时间；不接受客户端 studentId。
- `CourseMaterialVO/MaterialAccessVO`：授权元数据和访问模式，不暴露物理路径。
- `CourseReviewVO`：管理员/教师视图使用，学生接口永不返回审核原因。

## 3. 教师课程管理

| 方法与路径 | 请求/查询 | 响应 | 权限 | 主要错误 |
|---|---|---|---|---|
| `POST /teacher/courses` | `CreateCourseRequest` | `CourseDetailVO` | TEACHER；当前用户成为 OWNER | 参数错误、courseCode 冲突 |
| `GET /teacher/courses` | `CourseListQuery` | `PageResponse<TeacherCourseListItemVO>` | TEACHER；只列本人关系课程 | 参数错误 |
| `GET /teacher/courses/{courseId}` | path | `CourseDetailVO` | OWNER/COLLABORATOR | 404/403 |
| `PUT /teacher/courses/{courseId}` | `UpdateCourseRequest` | `CourseDetailVO` | OWNER；DRAFT 或 REJECTED | 403/409 |
| `POST /teacher/courses/{courseId}/submit-review` | 无 | `CourseDetailVO` | OWNER；DRAFT/REJECTED | 403/409 |
| `POST /teacher/courses/{courseId}/publish` | 无 | `CourseDetailVO` | OWNER；APPROVED | 403/409 |
| `POST /teacher/courses/{courseId}/offline` | 无 | `CourseDetailVO` | OWNER | 403/409 |
| `GET /teacher/courses/{courseId}/teachers` | path | `List<CourseTeacherVO>` | 课程教师 | 404/403 |
| `POST /teacher/courses/{courseId}/teachers` | `AddCourseTeacherRequest` | `CourseTeacherVO` | OWNER | 404/403/409 |
| `DELETE /teacher/courses/{courseId}/teachers/{teacherId}` | path | `ApiResponse<Void>` | OWNER；不能删除 OWNER | 404/403/409 |

协作者可通过章节、课时、资料接口编辑内容，但不能改课程关键元数据、提交审核、管理成员、发布或下线。

## 4. 管理员课程审核

| 方法与路径 | 请求/查询 | 响应 | 权限 | 主要错误 |
|---|---|---|---|---|
| `GET /admin/course-reviews` | page/size/keyword/reviewStatus/term/sort | `PageResponse<CourseReviewListItemVO>` | ADMIN | 参数错误 |
| `GET /admin/course-reviews/{courseId}` | path | `CourseReviewDetailVO` | ADMIN | 404 |
| `POST /admin/course-reviews/{courseId}/approve` | `ReviewCourseRequest` | `CourseReviewVO` | ADMIN；PENDING | 404/409 |
| `POST /admin/course-reviews/{courseId}/reject` | `RejectCourseRequest` | `CourseReviewVO` | ADMIN；PENDING | 400/404/409 |

批准/驳回记录 reviewerId、reviewedAt、结论、原因/备注。审核接口不接受课程正文、章节或课时字段。

## 5. 学生课程与选课

| 方法与路径 | 请求/查询 | 响应 | 权限 | 主要错误 |
|---|---|---|---|---|
| `GET /student/courses/catalog` | `CourseListQuery` | `PageResponse<StudentCourseListItemVO>` | STUDENT；只显示 APPROVED+PUBLISHED/ONGOING+选课窗口开放 | 参数错误 |
| `GET /student/courses` | page/size/keyword/status/term/categoryId/sort | `PageResponse<StudentCourseListItemVO>` | STUDENT；只列本人选课 | 参数错误 |
| `GET /student/courses/{courseId}` | path | `CourseDetailVO` | 有效选课；目录详情仅通过 catalog 字段展示 | 404/403 |
| `POST /student/courses/{courseId}/enroll` | 无 | `EnrollmentVO` | STUDENT；课程可选 | 404/409 |
| `POST /student/courses/{courseId}/withdraw` | 无 | `EnrollmentVO` | STUDENT；本人 ENROLLED | 404/409 |

“待开始/在学/已结束”由课程时间/状态与 enrollment status 映射，不新增数据库状态。

## 6. 教师章节与课时

| 方法与路径 | 请求 | 响应 | 权限/错误摘要 |
|---|---|---|---|
| `GET /teacher/courses/{courseId}/chapters` | path | `List<ChapterDetailVO>` | 课程教师 |
| `POST /teacher/courses/{courseId}/chapters` | `CreateChapterRequest` | `ChapterDetailVO` | OWNER/COLLABORATOR |
| `PUT /teacher/chapters/{chapterId}` | `UpdateChapterRequest` | `ChapterDetailVO` | OWNER/COLLABORATOR；版本校验 |
| `DELETE /teacher/chapters/{chapterId}` | path | `Void` | OWNER/COLLABORATOR；逻辑删除 |
| `POST /teacher/chapters/{chapterId}/publish` | path | `ChapterDetailVO` | 课程已发布/进行中，否则 409 |
| `POST /teacher/chapters/{chapterId}/offline` | path | `ChapterDetailVO` | OWNER/COLLABORATOR |
| `GET /teacher/chapters/{chapterId}/lessons` | path | `List<LessonDetailVO>` | 课程教师 |
| `POST /teacher/chapters/{chapterId}/lessons` | `CreateLessonRequest` | `LessonDetailVO` | OWNER/COLLABORATOR；courseId 以章节为准，可选请求值仅用于一致性断言 |
| `GET /teacher/lessons/{lessonId}` | path | `LessonDetailVO` | 课程教师 |
| `PUT /teacher/lessons/{lessonId}` | `UpdateLessonRequest` | `LessonDetailVO` | OWNER/COLLABORATOR |
| `DELETE /teacher/lessons/{lessonId}` | path | `Void` | OWNER/COLLABORATOR；保留学习记录 |
| `POST /teacher/lessons/{lessonId}/publish` | path | `LessonDetailVO` | 章节已发布，否则 409 |
| `POST /teacher/lessons/{lessonId}/offline` | path | `LessonDetailVO` | OWNER/COLLABORATOR |

首版只使用 `sortOrder`，不提供拖拽排序 API。

## 7. 教师课程资料

| 方法与路径 | 请求 | 响应 | 权限/错误摘要 |
|---|---|---|---|
| `GET /teacher/courses/{courseId}/materials` | page/size/status/visibility | `PageResponse<CourseMaterialVO>` | 课程教师 |
| `POST /teacher/courses/{courseId}/materials` | `CreateCourseMaterialRequest` | `CourseMaterialVO` | OWNER/COLLABORATOR；归属一致性 |
| `PUT /teacher/materials/{materialId}` | `UpdateCourseMaterialRequest` | `CourseMaterialVO` | OWNER/COLLABORATOR；归属一致性 |
| `DELETE /teacher/materials/{materialId}` | path | `Void` | OWNER/COLLABORATOR；逻辑删除 |

Mock 阶段暂缺：真实附件 ID、校验和、病毒扫描状态、签名 URL 过期时间。`fileKey/fileUrl` 只用于本地联调，学生接口不原样回传内部值。

## 8. 学生学习与进度

| 方法与路径 | 响应 | 权限/错误摘要 |
|---|---|---|
| `GET /student/courses/{courseId}/outline` | `CourseOutlineVO` | 已选且课程可学习；只含已发布章节和已发布课时 |
| `GET /student/lessons/{lessonId}` | `StudentLessonDetailVO` | 已选、课程/章节/课时发布、已解锁 |
| `POST /student/lessons/{lessonId}/start` | `LearningRecordVO` | 本人；幂等 |
| `POST /student/lessons/{lessonId}/complete` | `LearningRecordVO` | 本人；幂等 |
| `GET /student/courses/{courseId}/progress` | `CourseProgressVO` | 本人有效选课 |
| `GET /student/materials/{materialId}` | `MaterialAccessVO` | 资料及所属层级可访问 |

进度口径：

```text
progressPercent = completedLessons / availableLessons * 100
```

`totalLessons` 为当前课程未删除课时数；`availableLessons` 为当前已发布、所属章节已发布且已解锁课时数；`completedLessons` 只统计 available 集合内本人的完成记录。无可学习课时时百分比为 0。

## 9. 示例

创建课程：

```http
POST /api/v1/teacher/courses
Authorization: Bearer <teacher-token>
Content-Type: application/json

{
  "courseCode": "CS-DS-2026",
  "name": "数据结构",
  "summary": "面向本科生的数据结构基础课程",
  "term": "2026-FALL",
  "department": "计算机学院",
  "credit": 3.0,
  "enrollmentOpenAt": "2026-08-01T00:00:00+08:00",
  "enrollmentCloseAt": "2026-09-15T23:59:59+08:00",
  "startAt": "2026-09-01T00:00:00+08:00",
  "endAt": "2027-01-15T23:59:59+08:00"
}
```

课程进度响应：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "courseId": "21001",
    "totalLessons": 8,
    "availableLessons": 6,
    "completedLessons": 3,
    "progressPercent": 50.00,
    "lastLessonId": "23003",
    "nextLessonId": "23004"
  },
  "errors": [],
  "traceId": "trace-course-12345678",
  "timestamp": "2026-07-07T00:00:00Z"
}
```

## 10. 联调与兼容性

- OpenAPI/Postman 以本文路径为准；实现完成后提供可导入集合和环境变量 `baseUrl/teacherToken/studentToken/adminToken`。
- 当前前端 Mock 可能缺少 `version`、审核历史、`accessMode`、课程负责人 ID、课时解锁信息；接入真实 API 时不得用列表对象代替详情请求。
- 新增可选字段兼容；改变状态 code、ID 类型、时间语义、权限范围或必填性属于破坏性变更，必须更新本文并单独评审。
