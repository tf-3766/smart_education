# 作业与提交 API 契约

> Owner：后端 A
> 当前状态：契约草案，先落在本地 `backend-2`，不合入 `dev`。
> 实现边界：本文只定义契约，不实现 Controller、Service、Mapper、Entity 或 Flyway SQL。

## 1. 业务范围

本契约覆盖“作业与提交最小闭环”：

```text
教师创建作业
-> 教师发布作业
-> 学生查看课程作业
-> 学生保存草稿或正式提交
-> 教师查看提交列表
```

不覆盖教师评分、成绩发布和统计；这些见 `docs/api-contract-grade.md`。文件上传仍按 `docs/api-style.md` 的统一上传能力处理，作业和提交只保存附件元数据或已上传文件引用。

## 2. 状态机

### 2.1 作业状态

| code | 含义 | 允许流转 | 操作者 |
|---|---|---|---|
| `DRAFT` | 教师编辑中，学生不可见 | `DRAFT -> PUBLISHED` | 课程教师 |
| `PUBLISHED` | 已发布，已选课学生可见 | `PUBLISHED -> CLOSED` | 课程教师 |
| `CLOSED` | 已关闭，不再允许提交 | 终态，MVP 不恢复 | 课程教师 |

时间派生展示：

| 展示状态 | 规则 |
|---|---|
| `NOT_OPEN` | 作业已发布且服务端当前时间早于 `openAt` |
| `OPEN` | 作业已发布，已到开放时间且未超过 `dueAt` |
| `OVERDUE` | 作业已发布且服务端当前时间晚于 `dueAt` |

`NOT_OPEN/OPEN/OVERDUE` 不落库，由服务端根据 `openAt/dueAt` 和 `status` 计算。

### 2.2 提交状态

| code | 含义 | 允许流转 | 操作者 |
|---|---|---|---|
| `DRAFT` | 学生草稿，教师默认不作为正式提交批改 | `DRAFT -> SUBMITTED` | 学生本人 |
| `SUBMITTED` | 学生已正式提交，等待批改 | `SUBMITTED -> GRADED` 或 `SUBMITTED -> RETURNED` | 课程教师 |
| `RETURNED` | 教师退回，允许后续重交时使用 | `RETURNED -> SUBMITTED` | 学生本人 |
| `GRADED` | 已批改，但成绩不一定发布 | 终态，除非后续定义更正流程 | 课程教师 |

MVP 默认：同一学生对同一作业只允许一个有效提交尝试；正式提交后再次正式提交返回 409。若后续要求重交，必须先在契约中明确 `RETURNED` 的重交流程和 `attemptNo` 策略。

## 3. DTO

### 3.1 `AssignmentCreateRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `lessonId` | string | 否 | 可选绑定课时，必须属于 `courseId` |
| `title` | string | 是 | 1 到 160 字符 |
| `description` | string | 否 | 作业说明 |
| `maxScore` | decimal | 是 | 大于 0，最多两位小数 |
| `openAt` | datetime | 否 | 不填表示发布后立即开放 |
| `dueAt` | datetime | 是 | 必须晚于 `openAt` 或发布时间 |
| `attachments` | array | 否 | 作业附件元数据，MVP 可为空 |

### 3.2 `AssignmentUpdateRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `title` | string | 是 | 1 到 160 字符 |
| `description` | string | 否 | 作业说明 |
| `maxScore` | decimal | 是 | 已有正式提交后修改满分需返回影响提示 |
| `openAt` | datetime | 否 | 不填表示发布后立即开放 |
| `dueAt` | datetime | 是 | 必须晚于 `openAt` |
| `attachments` | array | 否 | 替换作业附件元数据 |
| `version` | integer | 是 | 乐观锁版本 |

### 3.3 `AssignmentAttachmentDTO`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `name` | string | 是 | 展示文件名 |
| `fileKey` | string | 否 | 已上传文件 object key 或 mock key |
| `fileUrl` | string | 否 | MVP 演示 URL，可为空 |
| `fileSize` | long | 否 | 字节数 |
| `mimeType` | string | 否 | MIME 类型 |
| `sortOrder` | integer | 是 | 展示顺序 |

### 3.4 `SubmissionSaveRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `content` | string | 否 | 学生作答文本 |
| `fileKey` | string | 否 | 已上传文件 key，MVP 单附件 |
| `fileUrl` | string | 否 | 演示 URL，可为空 |
| `version` | integer | 否 | 更新已有草稿时必填 |

保存草稿使用 `PUT /api/v1/student/assignments/{assignmentId}/submission-draft`。

### 3.5 `SubmissionSubmitRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `content` | string | 否 | 学生作答文本 |
| `fileKey` | string | 否 | 已上传文件 key，MVP 单附件 |
| `fileUrl` | string | 否 | 演示 URL，可为空 |
| `version` | integer | 否 | 从草稿正式提交时必填 |

正式提交必须满足 `content` 或 `fileKey/fileUrl` 至少有一项。

## 4. VO

### 4.1 `AssignmentDetailVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `assignmentId` | string | 作业 ID |
| `courseId` | string | 课程 ID |
| `lessonId` | string/null | 绑定课时 |
| `title` | string | 标题 |
| `description` | string/null | 说明 |
| `maxScore` | decimal | 满分 |
| `assignmentStatus` | string | `DRAFT/PUBLISHED/CLOSED` |
| `availabilityStatus` | string | `NOT_OPEN/OPEN/OVERDUE/CLOSED` |
| `openAt` | datetime/null | 开放时间 |
| `dueAt` | datetime | 截止时间 |
| `publishedAt` | datetime/null | 发布时间 |
| `attachments` | array | 作业附件 |
| `version` | integer | 乐观锁版本 |

### 4.2 `StudentAssignmentListItemVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `assignmentId` | string | 作业 ID |
| `courseId` | string | 课程 ID |
| `title` | string | 标题 |
| `maxScore` | decimal | 满分 |
| `availabilityStatus` | string | 学生端展示状态 |
| `dueAt` | datetime | 截止时间 |
| `submissionStatus` | string/null | 本人提交状态 |
| `submittedAt` | datetime/null | 正式提交时间 |
| `graded` | boolean | 是否已批改 |

### 4.3 `SubmissionDetailVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `submissionId` | string | 提交 ID |
| `assignmentId` | string | 作业 ID |
| `courseId` | string | 课程 ID |
| `studentId` | string | 学生 ID |
| `attemptNo` | integer | 提交次数 |
| `content` | string/null | 作答文本 |
| `fileKey` | string/null | 文件 key |
| `fileUrl` | string/null | 文件 URL |
| `submissionStatus` | string | 提交状态 |
| `submittedAt` | datetime/null | 正式提交时间 |
| `score` | decimal/null | 教师端可见分数 |
| `gradedAt` | datetime/null | 批改时间 |
| `publishedAt` | datetime/null | 成绩发布时间 |
| `version` | integer | 乐观锁版本 |

## 5. RESTful API

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | 查看课程作业，分页 |
| `POST` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | 创建作业草稿 |
| `PUT` | `/api/v1/teacher/assignments/{assignmentId}` | 教师 | 修改作业 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/publish` | 教师 | 发布作业 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/close` | 教师 | 关闭作业 |
| `GET` | `/api/v1/student/courses/{courseId}/assignments` | 学生 | 查看课程作业，分页 |
| `GET` | `/api/v1/student/assignments/{assignmentId}` | 学生 | 查看作业详情和本人提交状态 |
| `PUT` | `/api/v1/student/assignments/{assignmentId}/submission-draft` | 学生 | 保存或更新草稿 |
| `POST` | `/api/v1/student/assignments/{assignmentId}/submissions` | 学生 | 正式提交 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/submissions` | 教师 | 查看提交列表，分页 |

## 6. 权限规则

- 教师只能操作自己负责或协作课程下的作业。
- 学生只能查看已选课程中 `PUBLISHED` 的作业。
- 学生只能保存和提交自己的作业提交。
- 未选课学生访问作业列表、详情、草稿或提交接口返回 403，敏感场景可返回 404。
- 作业未开放时，学生可查看详情但不能正式提交；是否允许保存草稿由前端交互确认，默认允许。
- 作业逾期后，正式提交返回 409；草稿更新默认也返回 409，避免误以为仍可提交。
- 重复正式提交返回 409；同一 `Idempotency-Key` 的同请求重试返回首次结果。

## 7. 错误码

| code | HTTP | 场景 |
|---|---:|---|
| `ASSIGNMENT_NOT_FOUND` | 404 | 作业不存在或不可访问 |
| `ASSIGNMENT_FORBIDDEN` | 403 | 当前用户无权访问该作业 |
| `ASSIGNMENT_STATE_CONFLICT` | 409 | 当前作业状态不允许该操作 |
| `ASSIGNMENT_NOT_OPEN` | 409 | 作业尚未开放，不允许正式提交 |
| `ASSIGNMENT_OVERDUE` | 409 | 作业已截止，不允许正式提交 |
| `SUBMISSION_ALREADY_SUBMITTED` | 409 | 已正式提交，不能重复提交 |
| `SUBMISSION_CONTENT_REQUIRED` | 400 | 正式提交缺少文本和附件 |
| `RESOURCE_CONFLICT` | 409 | 乐观锁或幂等键冲突 |
| `PARAM_VALIDATION_ERROR` | 400 | 参数校验失败 |

## 8. 分页规则

列表统一使用：

| 参数 | 默认 | 说明 |
|---|---:|---|
| `page` | 1 | 从 1 开始 |
| `size` | 20 | 最大 100 |
| `status` | 空 | 作业或提交状态 |
| `sort` | `createdAt,desc` | 允许 `createdAt,dueAt,submittedAt` 白名单 |

返回 `PageResponse` 风格结构：`records/page/size/total/totalPages`。

## 9. 示例

### 9.1 教师创建作业

```http
POST /api/v1/teacher/courses/21001/assignments
Content-Type: application/json
Authorization: Bearer <teacher-token>
```

```json
{
  "title": "第一章课后练习",
  "description": "完成基础概念说明与案例分析。",
  "maxScore": 100,
  "openAt": "2026-09-01T08:00:00+08:00",
  "dueAt": "2026-09-15T23:59:59+08:00",
  "attachments": [
    {
      "name": "作业说明.pdf",
      "fileKey": "mock/assignment-31001/guide.pdf",
      "fileSize": 2048,
      "mimeType": "application/pdf",
      "sortOrder": 10
    }
  ]
}
```

### 9.2 学生正式提交

```http
POST /api/v1/student/assignments/31001/submissions
Idempotency-Key: assignment-31001-student-1001-v1
Content-Type: application/json
Authorization: Bearer <student-token>
```

```json
{
  "content": "学生提交的第一章练习内容。",
  "fileKey": "mock/submission-32001/report.docx"
}
```

### 9.3 逾期提交错误

```json
{
  "code": "ASSIGNMENT_OVERDUE",
  "message": "该作业已截止，当前不允许提交",
  "data": {
    "assignmentId": "31001",
    "deadlineAt": "2026-09-15T23:59:59+08:00"
  },
  "errors": [],
  "traceId": "01J...",
  "timestamp": "2026-09-16T00:05:00+08:00"
}
```

## 10. 前端 C 关注字段

- 学生列表需要 `availabilityStatus`、`submissionStatus`、`dueAt`、`graded`。
- 草稿页需要回显 `content/fileKey/fileUrl/version`。
- 正式提交按钮应在 `NOT_OPEN/OVERDUE/CLOSED` 时禁用，但服务端仍必须校验。
- 重复提交、逾期提交、未选课越权要展示稳定 `code`，不要解析中文 `message`。

## 11. 后端 B 上下文

- AI 评语草稿接口会以 `submissionId` 为上下文，但不能直接读取 Biz Mapper。
- Biz 需要向 AI 提供授权后的提交摘要时，应通过内部 contract 或 A 暴露的 application interface。
- AI 返回的 `aiCommentDraftId` 只有教师确认后，才能写入正式提交或成绩记录。
