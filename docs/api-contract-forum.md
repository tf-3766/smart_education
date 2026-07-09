# 课程论坛 API 契约

> Owner：后端 A
> 当前状态：契约草案，先落在本地 `backend-2`，不合入 `dev`。
> 实现边界：本文只定义契约，不实现 Controller、Service、Mapper、Entity 或 Flyway SQL。

## 1. 业务范围

本契约覆盖课程论坛最小闭环：

```text
课程成员查看主题
-> 学生发帖
-> 学生或教师回复
-> 教师或管理员隐藏违规内容
```

MVP 不做复杂审核流、举报流、敏感词系统、推荐流、消息中心或站内信。

## 2. 状态机

### 2.1 主题状态

| code | 含义 | 允许流转 | 操作者 |
|---|---|---|---|
| `VISIBLE` | 正常可见 | `VISIBLE -> HIDDEN` | 课程教师或管理员 |
| `HIDDEN` | 已隐藏，普通课程成员不可见 | `HIDDEN -> VISIBLE` | 课程教师或管理员 |

用户主动删除在 MVP 中采用逻辑删除或隐藏策略，不能物理删除历史。

### 2.2 回复状态

| code | 含义 | 允许流转 | 操作者 |
|---|---|---|---|
| `VISIBLE` | 正常可见 | `VISIBLE -> HIDDEN` | 课程教师或管理员 |
| `HIDDEN` | 已隐藏 | `HIDDEN -> VISIBLE` | 课程教师或管理员 |

回复必须归属于主题所在课程；`parentReplyId` 只能引用同主题下回复。

## 3. DTO

### 3.1 `ForumTopicCreateRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `title` | string | 是 | 1 到 160 字符 |
| `content` | string | 是 | 1 到 5000 字符 |

### 3.2 `ForumReplyCreateRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `parentReplyId` | string | 否 | 父回复 ID，必须属于同一主题 |
| `content` | string | 是 | 1 到 3000 字符 |

### 3.3 `ForumVisibilityRequest`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `visible` | boolean | 是 | `false` 表示隐藏，`true` 表示恢复 |
| `reason` | string | 否 | 治理原因，MVP 可只写日志，若要持久化需增量迁移 |
| `version` | integer | 是 | 乐观锁版本 |

## 4. VO

### 4.1 `ForumTopicListItemVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `topicId` | string | 主题 ID |
| `courseId` | string | 课程 ID |
| `title` | string | 标题 |
| `authorId` | string | 作者 ID |
| `authorName` | string/null | 作者展示名 |
| `status` | string | `VISIBLE/HIDDEN` |
| `pinned` | boolean | 是否置顶 |
| `replyCount` | integer | 回复数 |
| `lastRepliedAt` | datetime/null | 最近回复时间 |
| `createdAt` | datetime | 创建时间 |
| `version` | integer | 乐观锁版本 |

### 4.2 `ForumTopicDetailVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `topicId` | string | 主题 ID |
| `courseId` | string | 课程 ID |
| `title` | string | 标题 |
| `content` | string | 正文 |
| `authorId` | string | 作者 ID |
| `authorName` | string/null | 作者展示名 |
| `status` | string | 主题状态 |
| `createdAt` | datetime | 创建时间 |
| `version` | integer | 版本 |

### 4.3 `ForumReplyVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `replyId` | string | 回复 ID |
| `topicId` | string | 主题 ID |
| `courseId` | string | 课程 ID |
| `authorId` | string | 作者 ID |
| `authorName` | string/null | 作者展示名 |
| `parentReplyId` | string/null | 父回复 ID |
| `content` | string | 正文 |
| `status` | string | `VISIBLE/HIDDEN` |
| `createdAt` | datetime | 创建时间 |
| `version` | integer | 版本 |

## 5. RESTful API

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | 查看课程主题列表 |
| `POST` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | 发帖 |
| `GET` | `/api/v1/student/forum/topics/{topicId}` | 学生 | 查看主题详情 |
| `GET` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | 查看回复列表 |
| `POST` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | 回复 |
| `GET` | `/api/v1/teacher/courses/{courseId}/forum/topics` | 教师 | 教师查看课程论坛，包括治理信息 |
| `PATCH` | `/api/v1/teacher/forum/topics/{topicId}/visibility` | 教师 | 隐藏或恢复主题 |
| `PATCH` | `/api/v1/teacher/forum/replies/{replyId}/visibility` | 教师 | 隐藏或恢复回复 |
| `PATCH` | `/api/v1/admin/forum/topics/{topicId}/visibility` | 管理员 | 管理员治理主题 |
| `PATCH` | `/api/v1/admin/forum/replies/{replyId}/visibility` | 管理员 | 管理员治理回复 |

教师和学生路径分开，是因为教师需要看到隐藏内容、治理状态和更多筛选项。前端 C 可以先只接学生路径和教师治理路径。

## 6. 权限规则

- 学生必须是课程有效选课成员才能查看、发帖和回复。
- 教师必须是课程负责人或协作者才能查看教师论坛视图和治理内容。
- 管理员只能做内容治理，不参与课程教学评分。
- 隐藏主题后，普通学生列表和详情不可见；教师和管理员仍可在治理视图看到。
- 隐藏主题后，普通学生不能继续回复该主题。
- 回复必须属于可访问主题，不能跨课程引用 `parentReplyId`。

## 7. 错误码

| code | HTTP | 场景 |
|---|---:|---|
| `FORUM_TOPIC_NOT_FOUND` | 404 | 主题不存在或不可访问 |
| `FORUM_REPLY_NOT_FOUND` | 404 | 回复不存在或不可访问 |
| `FORUM_FORBIDDEN` | 403 | 非课程成员或无治理权限 |
| `FORUM_TOPIC_HIDDEN` | 409 | 主题已隐藏，不允许回复 |
| `FORUM_PARENT_REPLY_INVALID` | 400 | 父回复不属于当前主题 |
| `FORUM_STATE_CONFLICT` | 409 | 状态不允许当前操作 |
| `RESOURCE_CONFLICT` | 409 | 乐观锁冲突 |

## 8. 分页规则

主题列表默认按 `pinned desc, lastRepliedAt desc, id desc` 排序。回复列表默认按 `createdAt asc, id asc` 排序。支持 `page/size/status/keyword`，`keyword` 只匹配标题或短正文摘要，不做全文检索承诺。

## 9. 示例

### 9.1 学生发帖

```http
POST /api/v1/student/courses/21001/forum/topics
Content-Type: application/json
Authorization: Bearer <student-token>
```

```json
{
  "title": "第一章概念讨论",
  "content": "我对第一章中的案例还有疑问。"
}
```

### 9.2 教师隐藏主题

```http
PATCH /api/v1/teacher/forum/topics/34001/visibility
Content-Type: application/json
Authorization: Bearer <teacher-token>
```

```json
{
  "visible": false,
  "reason": "内容与课程无关",
  "version": 0
}
```

## 10. 前端 C 关注字段

- 学生端只展示 `VISIBLE` 主题和回复。
- 教师端需要显示 `status`、`version` 和治理按钮。
- 被隐藏内容应展示“已隐藏”占位，而不是泄露原正文给学生。
- 发帖和回复错误态重点处理 `FORUM_FORBIDDEN`、`FORUM_TOPIC_HIDDEN`。

## 11. 后端 B 上下文

论坛最小闭环不依赖 AI。后端 B 的 Gateway 只需要保证上述 `/api/v1/student/forum/**`、`/api/v1/teacher/forum/**`、`/api/v1/admin/forum/**` 路由能转发到 Biz；AI 不读取论坛正文作为知识库来源，除非后续另立授权和索引契约。
