# 成绩与批改 API 契约

> Owner：后端 A
> 当前状态：契约草案，先落在本地 `backend-2`，不合入 `dev`。
> 实现边界：本文只定义契约，不实现 Controller、Service、Mapper、Entity 或 Flyway SQL。

## 1. 业务范围

本契约覆盖“教师批改、成绩发布、学生查看、教师统计”：

```text
教师查看提交
-> 教师评分并保存评语
-> 教师发布成绩
-> 学生查看本人已发布成绩
-> 教师查看课程作业统计
```

作业创建和学生提交见 `docs/api-contract-assignment.md`。考试成绩首版不进入课程总成绩，后续接入必须先由后端 A 定义成绩来源接口或事件。

## 2. 状态机

### 2.1 批改状态

批改状态落在提交上：

| submissionStatus | 含义 |
|---|---|
| `SUBMITTED` | 已提交，等待批改 |
| `GRADED` | 已批改，有分数和评语 |
| `RETURNED` | 退回重做，MVP 默认不启用 |

### 2.2 成绩发布状态

成绩发布状态落在 `edu_grade_record.grade_status`：

| gradeStatus | 含义 | 允许流转 | 学生可见 |
|---|---|---|---|
| `DRAFT` | 教师已批改但未发布 | `DRAFT -> PUBLISHED` | 否 |
| `PUBLISHED` | 已发布给学生 | MVP 终态 | 是 |

MVP 不做撤回和成绩版本；如需要 `WITHDRAWN` 或更正历史，必须新增契约和迁移计划。

## 3. DTO

### 3.1 `GradeSubmissionRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `score` | decimal | 是 | 0 到 `maxScore` |
| `maxScore` | decimal | 是 | 必须与作业满分一致，除非后续定义修订规则 |
| `teacherComment` | string | 否 | 1000 字以内 |
| `aiCommentDraftId` | string | 否 | AI 评语草稿 ID，教师确认后才保存 |
| `publishNow` | boolean | 否 | 是否批改后立即发布成绩，默认 `false` |
| `version` | integer | 是 | 提交记录乐观锁版本 |

### 3.2 `PublishGradeRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `version` | integer | 是 | 成绩记录乐观锁版本 |

### 3.3 `GradeListQuery`

| 字段 | 类型 | 说明 |
|---|---|---|
| `courseId` | string | 可选课程过滤 |
| `sourceType` | string | MVP 固定支持 `ASSIGNMENT` |
| `status` | string | 教师端可筛选 `DRAFT/PUBLISHED` |
| `page` | integer | 从 1 开始 |
| `size` | integer | 最大 100 |

## 4. VO

### 4.1 `TeacherSubmissionGradeVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `submissionId` | string | 提交 ID |
| `assignmentId` | string | 作业 ID |
| `courseId` | string | 课程 ID |
| `studentId` | string | 学生 ID |
| `studentName` | string/null | 学生展示名，按当前用户表能力返回 |
| `submissionStatus` | string | `SUBMITTED/GRADED/RETURNED` |
| `submittedAt` | datetime/null | 提交时间 |
| `score` | decimal/null | 分数 |
| `maxScore` | decimal | 满分 |
| `teacherComment` | string/null | 教师评语 |
| `gradeStatus` | string/null | `DRAFT/PUBLISHED` |
| `publishedAt` | datetime/null | 成绩发布时间 |
| `version` | integer | 提交或成绩版本，接口需明确使用对象 |

### 4.2 `StudentGradeVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `gradeId` | string | 成绩 ID |
| `courseId` | string | 课程 ID |
| `assignmentId` | string | 作业 ID |
| `assignmentTitle` | string | 作业标题 |
| `score` | decimal | 得分 |
| `maxScore` | decimal | 满分 |
| `scoreRate` | decimal | 得分率，0 到 1 |
| `teacherComment` | string/null | 已发布评语 |
| `publishedAt` | datetime | 发布时间 |

### 4.3 `AssignmentStatisticsVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `assignmentId` | string | 作业 ID |
| `courseId` | string | 课程 ID |
| `totalStudentCount` | integer | 当前有效选课学生数 |
| `submittedCount` | integer | 已正式提交数 |
| `missingCount` | integer | 未提交数 |
| `gradedCount` | integer | 已批改数 |
| `publishedGradeCount` | integer | 已发布成绩数 |
| `averageScore` | decimal/null | 已发布或已批改成绩平均分，契约需固定口径 |
| `lowScoreCount` | integer | 低分人数，MVP 默认低于 60% |

MVP 统计口径：教师端统计以已正式提交和有效选课学生为基准；未发布成绩也可纳入教师内部批改统计，但学生端不可见。

## 5. RESTful API

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/submissions` | 教师 | 查看提交和批改状态，分页 |
| `POST` | `/api/v1/teacher/submissions/{submissionId}/grade` | 教师 | 批改并保存成绩草稿，可立即发布 |
| `POST` | `/api/v1/teacher/grades/{gradeId}/publication` | 教师 | 发布成绩 |
| `GET` | `/api/v1/student/grades` | 学生 | 查看本人已发布成绩，分页 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/statistics` | 教师 | 查看课程作业统计 |
| `GET` | `/api/v1/teacher/courses/{courseId}/grade-statistics` | 教师 | 查看课程成绩概览，MVP 可延后 |

## 6. 权限规则

- 教师只能批改自己负责或协作课程下的提交。
- 无关教师访问提交或成绩返回 403，敏感场景可返回 404。
- 管理员不替教师评分，不调用批改和发布成绩接口。
- 学生只能查看本人 `PUBLISHED` 成绩；未发布成绩不能返回 0 或空分数暗示。
- 成绩来源 `sourceType=ASSIGNMENT` 时，`sourceId` 使用作业 ID；不得直接暴露考试内部表。
- AI 评语草稿只有教师提交批改请求时才可保存为正式评语。

## 7. 错误码

| code | HTTP | 场景 |
|---|---:|---|
| `SUBMISSION_NOT_FOUND` | 404 | 提交不存在或不可访问 |
| `GRADE_NOT_FOUND` | 404 | 成绩不存在或不可访问 |
| `GRADE_FORBIDDEN` | 403 | 当前用户无权批改或查看 |
| `SUBMISSION_STATE_CONFLICT` | 409 | 提交状态不允许批改 |
| `GRADE_STATE_CONFLICT` | 409 | 成绩状态不允许发布 |
| `GRADE_SCORE_OUT_OF_RANGE` | 400 | 分数小于 0 或大于满分 |
| `AI_DRAFT_NOT_ACCEPTABLE` | 409 | AI 草稿不存在、未完成或不属于该提交 |
| `RESOURCE_CONFLICT` | 409 | 乐观锁冲突 |

## 8. 分页规则

提交列表默认按 `submittedAt,desc`，无提交时间时按 `updatedAt,desc`。学生成绩默认按 `publishedAt,desc`。允许筛选 `status`、`courseId`、`assignmentId`，不允许前端传数据库列名。

## 9. 示例

### 9.1 教师批改并立即发布

```http
POST /api/v1/teacher/submissions/32001/grade
Content-Type: application/json
Authorization: Bearer <teacher-token>
```

```json
{
  "score": 88.5,
  "maxScore": 100,
  "teacherComment": "结构清楚，注意补充推导细节。",
  "aiCommentDraftId": "39001",
  "publishNow": true,
  "version": 0
}
```

### 9.2 学生查看已发布成绩

```http
GET /api/v1/student/grades?courseId=21001&page=1&size=20
Authorization: Bearer <student-token>
```

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "records": [
      {
        "gradeId": "33001",
        "courseId": "21001",
        "assignmentId": "31001",
        "assignmentTitle": "第一章课后练习",
        "score": 88.5,
        "maxScore": 100,
        "scoreRate": 0.885,
        "teacherComment": "结构清楚，注意补充推导细节。",
        "publishedAt": "2026-09-16T09:00:00+08:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1
  },
  "errors": [],
  "traceId": "01J...",
  "timestamp": "2026-09-16T09:01:00+08:00"
}
```

## 10. 前端 C 关注字段

- 教师批改页需要 `submissionStatus`、`score`、`maxScore`、`teacherComment`、`gradeStatus`、`version`。
- 学生成绩页只展示 `PUBLISHED` 记录，不出现未发布占位分。
- 统计页要区分 `submittedCount`、`gradedCount`、`publishedGradeCount`。
- AI 评语按钮由后端 B 提供，A 的批改接口只接收确认后的 `aiCommentDraftId`。

## 11. 后端 B 上下文

- `POST /api/v1/ai/submissions/{submissionId}/comment-draft` 由后端 B 实现，返回草稿而非正式评语。
- 后端 B 请求提交上下文时必须经过 Biz 授权；不得读取 `edu_assignment_submission` Mapper。
- AI 草稿失败不得阻断教师手动批改和成绩发布。
