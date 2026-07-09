# 学习预警 API 契约

> Owner：后端 A
> 当前状态：契约草案，先落在本地 `backend-2`，不合入 `dev`。
> 实现边界：本文只定义契约，不实现 Controller、Service、Mapper、Entity 或 Flyway SQL。

## 1. 业务范围

本契约覆盖学习预警最小闭环：

```text
系统基于课程进度、作业缺交、低分生成预警
-> 学生查看本人预警
-> 教师查看自己课程下学生预警
-> 教师处理预警
-> AI 可解释预警和给建议，但不能创建或修改正式预警
```

MVP 不做复杂风险模型、消息中心、家长通知、自动干预流或跨课程综合画像。

## 2. 状态机

当前表 `edu_learning_warning.warning_status` 可承载 MVP 状态：

| code | 含义 | 允许流转 | 操作者 |
|---|---|---|---|
| `OPEN` | 已生成，待处理 | `OPEN -> HANDLED` 或 `OPEN -> IGNORED` | 课程教师 |
| `HANDLED` | 教师已处理 | 终态 | 课程教师 |
| `IGNORED` | 教师确认暂不处理 | 终态 | 课程教师 |

若后续要细分 `ACKNOWLEDGED/IN_PROGRESS/RESOLVED/CLOSED`，需要新增契约并评估是否增加 `edu_warning_action` 或处理备注字段。

## 3. 预警类型与依据

| warningType | 生成依据 | evidenceType |
|---|---|---|
| `PROGRESS_LAG` | 学生课程进度低于课程节奏或阈值 | `LESSON_PROGRESS` |
| `MISSING_ASSIGNMENT` | 已过截止时间但没有正式提交 | `ASSIGNMENT_SUBMISSION` |
| `LOW_SCORE` | 已发布作业成绩低于阈值 | `GRADE_RECORD` |

MVP 默认阈值：

| 指标 | 默认 |
|---|---|
| 低进度 | 完成率低于 50% |
| 低分 | 得分率低于 60% |
| 缺交 | `dueAt` 已过且无 `SUBMITTED/GRADED` 提交 |

阈值先写在服务配置或常量中，后续如要可配置再单独设计。

## 4. DTO

### 4.1 `GenerateCourseWarningsRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `warningTypes` | array | 否 | 不填表示运行全部 MVP 类型 |
| `studentId` | string | 否 | 可选只生成某个学生 |
| `dryRun` | boolean | 否 | 默认 `false`；`true` 只预览不写正式表 |

生成接口可以先作为教师手动触发的同步命令；后续定时任务仍由同一 application use case 执行。

### 4.2 `WarningHandleRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `action` | string | 是 | `HANDLED` 或 `IGNORED` |
| `remark` | string | 否 | MVP 如无字段保存，只用于日志；若要持久化需增量迁移 |
| `version` | integer | 是 | 乐观锁版本 |

### 4.3 `WarningListQuery`

| 字段 | 类型 | 说明 |
|---|---|---|
| `courseId` | string | 教师端必填，学生端可选 |
| `studentId` | string | 教师端可选 |
| `warningType` | string | 可选 |
| `warningLevel` | string | 可选 |
| `warningStatus` | string | 可选 |
| `page` | integer | 从 1 开始 |
| `size` | integer | 最大 100 |

## 5. VO

### 5.1 `LearningWarningVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `warningId` | string | 预警 ID |
| `courseId` | string | 课程 ID |
| `studentId` | string | 学生 ID |
| `warningType` | string | 预警类型 |
| `warningLevel` | string | `LOW/MEDIUM/HIGH` |
| `warningStatus` | string | `OPEN/HANDLED/IGNORED` |
| `summary` | string | 摘要 |
| `suggestion` | string/null | 规则建议或教师建议 |
| `aiExplanationDraftId` | string/null | AI 解释草稿 ID |
| `generatedAt` | datetime | 生成时间 |
| `handledBy` | string/null | 处理教师 ID |
| `handledAt` | datetime/null | 处理时间 |
| `evidences` | array | 预警依据 |
| `version` | integer | 乐观锁版本 |

### 5.2 `WarningEvidenceVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `evidenceId` | string | 依据 ID |
| `evidenceType` | string | `LESSON_PROGRESS/ASSIGNMENT_SUBMISSION/GRADE_RECORD` |
| `sourceId` | string/null | 来源业务 ID |
| `metricCode` | string/null | 指标 code |
| `metricValue` | string/null | 指标值快照 |
| `description` | string | 解释说明 |

### 5.3 `WarningGenerationResultVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `createdCount` | integer | 新增预警数 |
| `skippedCount` | integer | 已存在或不满足规则而跳过 |
| `warnings` | array | `dryRun=true` 时返回预览，正式写入时可返回简表 |

## 6. RESTful API

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `POST` | `/api/v1/teacher/courses/{courseId}/warnings/generation` | 教师 | 为课程生成规则型预警 |
| `GET` | `/api/v1/student/warnings` | 学生 | 查看本人预警，分页 |
| `GET` | `/api/v1/student/warnings/{warningId}` | 学生 | 查看本人预警详情 |
| `GET` | `/api/v1/teacher/courses/{courseId}/warnings` | 教师 | 查看课程预警，分页 |
| `GET` | `/api/v1/teacher/warnings/{warningId}` | 教师 | 查看课程下某条预警详情 |
| `POST` | `/api/v1/teacher/warnings/{warningId}/handle` | 教师 | 处理或忽略预警 |

## 7. 权限规则

- 学生只能查看本人预警和依据。
- 教师只能生成、查看、处理自己负责或协作课程下学生预警。
- 管理员不直接生成或处理教学预警；如后续要治理视图，必须另立管理员契约。
- AI 不能创建、更新、关闭或删除正式预警数据。
- 缺交和低分预警必须基于后端 A 的作业/成绩事实；不能由前端或 AI 传入结论。
- 生成预警应具备幂等性：同一课程、学生、类型、来源对象已有 `OPEN` 预警时默认跳过。

## 8. 错误码

| code | HTTP | 场景 |
|---|---:|---|
| `WARNING_NOT_FOUND` | 404 | 预警不存在或不可访问 |
| `WARNING_FORBIDDEN` | 403 | 当前用户无权查看或处理 |
| `WARNING_STATE_CONFLICT` | 409 | 当前预警状态不允许处理 |
| `WARNING_GENERATION_CONFLICT` | 409 | 生成任务与现有规则冲突 |
| `WARNING_RULE_UNSUPPORTED` | 400 | 不支持的预警类型 |
| `WARNING_EVIDENCE_MISSING` | 409 | 缺少生成预警所需依据 |
| `RESOURCE_CONFLICT` | 409 | 乐观锁冲突 |

## 9. 分页规则

学生端默认按 `generatedAt desc, id desc`。教师端默认按 `warningStatus asc, warningLevel desc, generatedAt desc, id desc`。支持筛选 `warningType`、`warningLevel`、`warningStatus`、`studentId`。

## 10. 示例

### 10.1 教师生成课程预警

```http
POST /api/v1/teacher/courses/21001/warnings/generation
Content-Type: application/json
Authorization: Bearer <teacher-token>
```

```json
{
  "warningTypes": ["PROGRESS_LAG", "MISSING_ASSIGNMENT", "LOW_SCORE"],
  "dryRun": false
}
```

### 10.2 学生查看预警

```json
{
  "warningId": "36001",
  "courseId": "21001",
  "studentId": "1001",
  "warningType": "PROGRESS_LAG",
  "warningLevel": "MEDIUM",
  "warningStatus": "OPEN",
  "summary": "学习进度低于课程节奏",
  "suggestion": "建议完成第一章补学并提交学习反馈。",
  "aiExplanationDraftId": "39002",
  "generatedAt": "2026-09-16T09:00:00+08:00",
  "handledBy": null,
  "handledAt": null,
  "evidences": [
    {
      "evidenceId": "36101",
      "evidenceType": "LESSON_PROGRESS",
      "sourceId": "23001",
      "metricCode": "completedLessonRate",
      "metricValue": "0.25",
      "description": "课程学习完成率低于 50%。"
    }
  ],
  "version": 0
}
```

### 10.3 教师处理预警

```http
POST /api/v1/teacher/warnings/36001/handle
Content-Type: application/json
Authorization: Bearer <teacher-token>
```

```json
{
  "action": "HANDLED",
  "remark": "已线下提醒学生补学第一章。",
  "version": 0
}
```

## 11. 前端 C 关注字段

- 学生端重点展示 `warningLevel`、`summary`、`suggestion`、`generatedAt`、`warningStatus`。
- 教师端需要 `studentId/studentName`、`evidences`、`version` 和处理按钮。
- `aiExplanationDraftId` 只能作为“查看 AI 解释”的可选入口，不代表预警由 AI 创建。
- 处理备注在 MVP 中可能只进入日志；若页面必须展示历史备注，需要先增量设计。

## 12. 后端 B 上下文

- `POST /api/v1/ai/warnings/{warningId}/explanation` 由后端 B 实现，返回解释草稿或建议。
- 后端 B 只能基于 Biz 授权后的预警摘要和依据生成解释。
- AI 解释失败不得影响预警生成、查看和教师处理。
