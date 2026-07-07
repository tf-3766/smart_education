# API 风格与协议规范

> 所有公开接口版本统一使用 `/api/v1`。内部服务接口统一使用 `/_internal/v1`，且不得由 Gateway 对外暴露。

## 1. 通用约定

- 协议：生产环境只允许 HTTPS；JSON 使用 UTF-8。
- 普通响应：`Content-Type: application/json`。
- SSE：`Content-Type: text/event-stream;charset=UTF-8`。
- 字段名：JSON 使用 `camelCase`。
- ID：数据库为 Long，对外一律返回字符串。
- 时间：RFC 3339，必须带时区偏移。
- 请求追踪：请求/响应使用 `X-Trace-Id`；未提供时由 Gateway 生成。
- 幂等：高风险创建/提交接口支持 `Idempotency-Key`，同一 key 和相同请求返回同一结果；不同请求复用 key 返回 409。
- 语言：稳定 machine-readable 信息放 `code`，中文提示放 `message`；客户端不得通过解析 message 判断逻辑。

## 2. 路径与 HTTP 方法

### 2.1 路径

```text
/api/v1/auth/login
/api/v1/student/courses
/api/v1/student/assignments/{assignmentId}
/api/v1/teacher/courses/{courseId}/assignments
/api/v1/teacher/assignments/{assignmentId}/submissions
/api/v1/admin/course-reviews/{reviewId}
/api/v1/ai/course-qa/streams
```

- 使用小写复数名词和连字符。
- 路径参数使用有语义的 `courseId`、`submissionId`，不用通用 `id`。
- 筛选使用 query 参数，不复制多个列表接口。
- 只有确实是业务命令时使用子资源，例如 `POST /grades/{gradeId}/publication`。

### 2.2 HTTP 状态

| 状态 | 使用场景 |
|---:|---|
| 200 | 查询、更新、业务命令成功 |
| 201 | 创建资源成功 |
| 202 | 异步任务已接受，返回 taskId |
| 204 | 删除成功且无响应体 |
| 400 | JSON/查询格式或参数校验错误 |
| 401 | 未登录、Token 无效/过期 |
| 403 | 已登录但无功能/资源权限 |
| 404 | 资源不存在或按安全策略不向当前用户暴露 |
| 409 | 状态冲突、版本冲突、幂等冲突 |
| 413 | 文件/请求体过大 |
| 415 | 不支持的媒体类型 |
| 422 | 可选：语法正确但复杂业务校验无法执行；若团队不采用则统一 400/409 |
| 429 | 限流或 AI 并发超限 |
| 500 | 未知服务端错误 |
| 503 | 依赖/AI 服务暂时不可用 |

团队需统一是否使用 422；未确认前，字段/跨字段校验使用 400，资源状态冲突使用 409。

## 3. 统一响应

### 3.1 成功响应

HTTP 200：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "courseId": "1908874353992142848",
    "name": "数据结构",
    "courseStatus": "IN_PROGRESS"
  },
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

创建成功使用 HTTP 201，`data` 至少包含新资源 ID；是否返回 `Location` header 由该类接口统一决定。

### 3.2 分页响应

HTTP 200：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "records": [
      {
        "assignmentId": "1908874353992143001",
        "title": "二叉树遍历实现",
        "assignmentStatus": "PUBLISHED",
        "deadlineAt": "2026-07-10T23:59:00.000+08:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 46,
    "totalPages": 3
  },
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

### 3.3 无内容响应

DELETE 采用 HTTP 204 时不得再返回 JSON body。若某资源删除需要返回影响范围，则统一采用 HTTP 200 + `ApiResponse`，不能同一路径随机变化。

## 4. 错误响应

统一结构：

```json
{
  "code": "MODULE-0000",
  "message": "面向用户的稳定提示",
  "data": null,
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

只有参数错误等多明细场景使用 `errors`；其他场景可以省略或为空数组，全项目选择一种并保持一致。本文推荐始终返回数组。

### 4.1 参数错误

HTTP 400：

```json
{
  "code": "PARAM_VALIDATION_ERROR",
  "message": "请求参数不正确",
  "data": null,
  "errors": [
    {
      "field": "deadlineAt",
      "reason": "必须晚于发布时间",
      "rejectedValue": "2026-07-01T08:00:00+08:00"
    },
    {
      "field": "title",
      "reason": "长度必须为 1 到 100 个字符"
    }
  ],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

`rejectedValue` 只能返回非敏感且长度受限的值；密码、Token、作业全文、考试答案不得回显。

### 4.2 未登录

HTTP 401：

```json
{
  "code": "UNAUTHORIZED",
  "message": "请先登录或检查 Token",
  "data": null,
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

### 4.3 无权限

HTTP 403：

```json
{
  "code": "FORBIDDEN",
  "message": "你没有查看或操作该资源的权限",
  "data": null,
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

不能在 message 中泄露其他学生、私有课程或未公开试题是否存在。按安全策略可对敏感资源返回 404。

### 4.4 资源不存在

HTTP 404：

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "课程不存在或已不可访问",
  "data": null,
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

### 4.5 业务冲突

HTTP 409：

```json
{
  "code": "OPERATION_NOT_ALLOWED",
  "message": "该作业已截止，当前不允许提交",
  "data": {
    "assignmentId": "1908874353992143001",
    "currentStatus": "CLOSED",
    "deadlineAt": "2026-07-05T23:59:00.000+08:00"
  },
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

乐观锁或唯一约束冲突统一使用 409 和 `RESOURCE_CONFLICT`，提示客户端刷新或修改输入，不返回数据库内部约束名。

### 4.6 AI 限流

HTTP 429，建议包含 `Retry-After` header：

```json
{
  "code": "AI_RATE_LIMITED",
  "message": "AI 请求过于频繁，请稍后再试",
  "data": {
    "retryAfterSeconds": 20
  },
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

### 4.7 AI 流建立前中断/不可用

如果错误发生在 SSE 响应头发送前，返回普通 JSON。HTTP 503：

```json
{
  "code": "AI_SERVICE_UNAVAILABLE",
  "message": "AI 服务暂时不可用，你的输入已保留，请稍后重试",
  "data": {
    "retryable": true,
    "conversationId": "1908874353992143999"
  },
  "errors": [],
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

如果流已经建立，必须发送 `event: error` 后关闭连接，不能再切换为 HTTP JSON；示例见 SSE 章节。

## 5. 列表查询参数

### 5.1 统一参数

| 参数 | 类型 | 默认 | 规则 |
|---|---|---:|---|
| `page` | integer | 1 | 从 1 开始 |
| `size` | integer | 20 | 允许 10/20/50/100，最大 100 |
| `sort` | string | 模块定义 | `field,asc` 或 `field,desc`，字段白名单 |
| `keyword` | string | 空 | trim，限制长度和搜索列 |
| `status` | enum | 空 | 必须是该资源业务枚举 code |
| `startAt` | datetime | 空 | 与 `endAt` 成对校验 |
| `endAt` | datetime | 空 | 必须大于等于 `startAt` |

示例：

```http
GET /api/v1/teacher/assignments?page=1&size=20&courseId=1908874353992142848&status=PUBLISHED&sort=deadlineAt,asc
```

### 5.2 规则

- 不允许客户端传数据库列名和任意排序表达式。
- 多值筛选重复 query 参数或使用逗号分隔，团队统一后不可混用；建议重复参数：`status=DRAFT&status=PUBLISHED`。
- 返回列表必须有稳定次排序，推荐 `id DESC`。
- 筛选无结果仍返回 HTTP 200 和空 `records`，不是 404。
- 大数据导出不能使用超大 `size`；创建异步导出任务并返回 202。

## 6. 日期时间格式

- 时间点：`2026-07-06T20:30:00.123+08:00`。
- UTC 示例：`2026-07-06T12:30:00.123Z`。
- 纯日期：`2026-07-06`。
- 时间段必须明确 `startAt/endAt`，禁止用“今天”“最近”作为 API 值。
- 数据库存 UTC，API 可以按请求/用户时区返回带偏移时间；同一响应保持一致。
- 截止/开考/交卷判断以服务端 Clock 为准，前端传来的当前时间无效。
- 不使用 `yyyy-MM-dd HH:mm:ss` 作为公共 API 时间点，因为缺少时区。

## 7. 文件上传接口

### 7.1 基础协议

MVP 统一入口：

```http
POST /api/v1/files
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `file` | 是 | 单文件；多文件由客户端逐个上传或使用明确批量端点 |
| `purpose` | 是 | `COURSE_RESOURCE`、`ASSIGNMENT_ATTACHMENT`、`SUBMISSION_ATTACHMENT` 等枚举 |
| `courseId` | 按用途 | 用于权限和归属校验 |
| `checksum` | 建议 | SHA-256，用于完整性/去重，不作为唯一安全校验 |

成功响应 HTTP 201：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "fileId": "1908874353992144888",
    "fileName": "chapter-3.pdf",
    "contentType": "application/pdf",
    "size": 2457812,
    "checksum": "sha256:...",
    "status": "UPLOADED"
  },
  "traceId": "01J1F6M8K9Q2R3S4T5V6W7X8Y9",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

### 7.2 安全与业务规则

- 上传成功不等于作业已提交、课程资料已发布；业务对象需再引用 `fileId` 并执行正式命令。
- 服务端校验扩展名、真实 MIME、文件头、大小、数量、用途和资源权限。
- 文件名只作展示，存储使用不可预测 object key；禁止拼接用户文件名作为服务器路径。
- 下载使用受鉴权接口或短时签名 URL，不暴露物理路径/object storage 密钥。
- 返回文件元数据，不返回 base64。
- 超过单机直传阈值时升级为预签名分片上传，但仍由 Biz 创建上传会话和确认归属；不新增文件微服务。
- 恶意文件扫描、预览失败和索引失败是独立状态，不应覆盖原始上传状态。

## 8. SSE 流式输出规范

### 8.1 端点与请求

推荐使用 POST 创建流：

```http
POST /api/v1/ai/course-qa/streams
Accept: text/event-stream
Content-Type: application/json
Authorization: Bearer <token>
```

请求示例：

```json
{
  "conversationId": "1908874353992143999",
  "courseId": "1908874353992142848",
  "lessonId": "1908874353992142888",
  "question": "二叉树和普通树的主要区别是什么？",
  "selectedText": null
}
```

Gateway 负责 JWT、限流和流转发；AI 服务必须从 Biz 取得授权上下文，不能只信请求中的 courseId/lessonId。

### 8.2 事件顺序

正常顺序：

```text
meta → delta* → citation* → done
```

异常顺序：

```text
meta → delta* → citation* → error → close
```

- `meta` 必须第一条且仅一条。
- `delta` 可以 0～N 条，`sequence` 严格递增。
- `citation` 可以在对应内容生成后发送；`citationId` 在本次回答内稳定。
- `done` 或 `error` 二选一作为终止事件。
- 心跳使用 SSE comment `: heartbeat`，默认约 15 秒，不定义成业务事件。

### 8.3 `meta`

```text
event: meta
id: 01J1F6M8:1
data: {"traceId":"01J1F6M8K9Q2R3S4T5V6W7X8Y9","conversationId":"1908874353992143999","messageId":"1908874353992144000","context":{"courseId":"1908874353992142848","lessonId":"1908874353992142888","scope":"CURRENT_LESSON"},"status":"RETRIEVING"}
```

### 8.4 `delta`

```text
event: delta
id: 01J1F6M8:2
data: {"sequence":1,"text":"二叉树是一种每个节点最多有两个子节点的树结构。"}
```

`delta.text` 只包含面向用户的回答增量，不包含模型原始推理链、隐藏标记或供应商原始事件。

### 8.5 `citation`

```text
event: citation
id: 01J1F6M8:3
data: {"citation":{"citationId":"c1","sourceId":"1908874353992145001","resourceId":"1908874353992145000","resourceVersion":3,"resourceType":"COURSE_PDF","title":"数据结构课程讲义","locator":{"kind":"PAGE","label":"第 18 页","page":18,"lessonId":"1908874353992142888"},"snippet":"二叉树中每个结点的度不大于 2……","accessUrl":"/api/v1/student/courses/1908874353992142848/resources/1908874353992145000?anchor=page-18"}}
```

### 8.6 `done`

```text
event: done
id: 01J1F6M8:4
data: {"finishReason":"STOP","citationCount":1,"groundingStatus":"GROUNDED","usage":{"inputTokens":820,"outputTokens":196}}
```

`usage` 是否对普通用户展示由产品决定；即使返回也不得包含供应商密钥、计费账号或内部 prompt。

### 8.7 `error`

```text
event: error
id: 01J1F6M8:4
data: {"code":"SSE_STREAM_INTERRUPTED","message":"回答未完整生成，已保留现有内容","retryable":true,"partial":true,"lastSequence":1,"traceId":"01J1F6M8K9Q2R3S4T5V6W7X8Y9"}
```

发送后立即结束流。客户端保留已生成内容、问题和上下文，并提供重试；不能把已有内容清空。

### 8.8 取消与恢复

- 用户停止生成时，客户端关闭连接并可调用取消端点；服务端传播取消到模型 provider。
- 服务端最终状态为 `CANCELLED`，已生成片段可保存为非正式会话记录。
- MVP 不保证 `Last-Event-ID` 断点续传；重试创建新的 message/version，不静默拼接两个回答。
- 页面离开时首期统一停止生成并保留会话记录，避免后台无界生成。

## 9. AI 引用来源结构

统一 JSON：

```json
{
  "citationId": "c1",
  "sourceId": "1908874353992145001",
  "resourceId": "1908874353992145000",
  "resourceVersion": 3,
  "resourceType": "COURSE_PDF",
  "title": "数据结构课程讲义",
  "locator": {
    "kind": "PAGE",
    "label": "第 18 页",
    "page": 18,
    "chapterId": null,
    "lessonId": "1908874353992142888",
    "section": "3.2 二叉树"
  },
  "snippet": "二叉树中每个结点的度不大于 2……",
  "accessUrl": "/api/v1/student/courses/1908874353992142848/resources/1908874353992145000?anchor=page-18"
}
```

规则：

- `citationId` 用于回答内 `[1]` 等编号映射，不等同数据库 ID。
- `sourceId` 可代表 chunk/索引来源，但不得让客户端据此读取向量库。
- `resourceVersion` 必须返回，资料更新后旧引用应标记失效/版本不一致。
- `snippet` 短小且经过权限和内容安全过滤，禁止返回整页/整章。
- `accessUrl` 必须再次鉴权，不能是永久公开对象存储 URL。
- 无可靠来源时返回 `groundingStatus: "NO_RELIABLE_SOURCE"`，不得伪造 citation。
- 使用通用知识补充时明确 `groundingStatus: "GENERAL_KNOWLEDGE_SUPPLEMENT"`，是否允许由课程/考试策略决定。

## 10. AI 输出红线

AI 接口禁止返回：

- 模型原始推理过程、chain-of-thought、隐藏 reasoning token。
- 系统提示词、检索内部 prompt、模型 key 或供应商原始错误体。
- 未授权课程/学生资料的标题、片段、链接。
- 看似正式的成绩、已发布评语、已确认试卷或已关闭预警状态。

可以返回简短、面向用户的结论依据摘要，例如“依据课程讲义第 18 页和本章定义”，但这不是模型内部思维链。

## 11. 版本与兼容性

- `/api/v1` 内新增可选响应字段通常视为兼容；改变类型、含义、必填性、枚举 code 属于破坏性变更。
- 枚举只可新增，不可复用旧 code 表达新含义。
- 错误码一经使用不改含义。
- 内部 Contract DTO 和 MQ 事件同样版本化；破坏性变化创建 `V2`/`.v2`。
- OpenAPI 按业务模块拆文件，由 CI 校验示例和实现。

## 12. 接口评审清单

- [ ] 路径以 `/api/v1` 开头，资源名和角色域清晰。
- [ ] 请求/响应没有 Entity、MyBatis Page 或无结构 Map。
- [ ] Long ID 以 JSON 字符串返回。
- [ ] 时间带时区，分页和排序使用统一参数。
- [ ] HTTP 状态、业务错误码和 message 各司其职。
- [ ] 明确角色、功能权限、资源范围和对象状态。
- [ ] 文件上传与正式提交/发布分离。
- [ ] SSE 事件遵守 `meta/delta/citation/done/error`。
- [ ] AI 引用可定位、可鉴权、带资源版本。
- [ ] AI 未返回原始推理过程，也未直接修改正式业务数据。
