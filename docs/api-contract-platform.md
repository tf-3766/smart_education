# 在线教育辅助教学系统前后端 API 接口文档

> 版本：2026-07-09  
> 网关地址：`http://localhost:18080`  
> 公开接口前缀：`/api/v1`  
> 内部服务接口前缀：`/_internal/v1`，不得由 Gateway 对外暴露  
> OpenAPI 草案：`docs/openapi/platform-api.openapi.yaml`

## 1. 通用约定

所有公开接口通过 `edu-gateway` 访问。除登录和健康检查外，请求必须携带：

```http
Authorization: Bearer <accessToken>
X-Trace-Id: <optional-client-trace-id>
Content-Type: application/json
```

统一响应：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "errors": [],
  "traceId": "gateway-trace-12345678",
  "timestamp": "2026-07-09T08:00:00Z"
}
```

ID 对外统一按字符串传输；时间使用 RFC 3339；分页参数为 `page`、`size`、`sort`。

## 2. 认证与当前用户

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | 匿名 | 登录，返回 token、当前用户、角色和权限 |
| `GET` | `/api/v1/auth/me` | 已登录 | 获取当前用户信息 |
| `POST` | `/api/v1/auth/logout` | 已登录 | 退出登录，后续可接入 token 黑名单 |

## 3. 课程与学习

课程与学习接口已在 `docs/course-api-contract.md` 和 `docs/openapi/course-module.openapi.yaml` 细化。本接口文档保留总入口：

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET/POST` | `/api/v1/teacher/courses` | 教师 | 教师课程列表、创建课程 |
| `GET/PUT` | `/api/v1/teacher/courses/{courseId}` | 教师 | 教师查看、修改课程 |
| `POST` | `/api/v1/teacher/courses/{courseId}/submit-review` | 教师 | 提交课程审核 |
| `GET` | `/api/v1/admin/course-reviews` | 管理员 | 课程审核列表 |
| `POST` | `/api/v1/admin/course-reviews/{courseId}/approve` | 管理员 | 审核通过 |
| `GET` | `/api/v1/student/courses/catalog` | 学生 | 课程广场 |
| `POST` | `/api/v1/student/courses/{courseId}/enroll` | 学生 | 选课 |
| `GET` | `/api/v1/student/courses/{courseId}/outline` | 学生 | 学习大纲 |
| `POST` | `/api/v1/student/lessons/{lessonId}/complete` | 学生 | 完成课时 |

## 4. 作业、提交与成绩

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | 查看课程作业 |
| `POST` | `/api/v1/teacher/courses/{courseId}/assignments` | 教师 | 创建作业 |
| `PUT` | `/api/v1/teacher/assignments/{assignmentId}` | 教师 | 修改作业 |
| `POST` | `/api/v1/teacher/assignments/{assignmentId}/publish` | 教师 | 发布作业 |
| `GET` | `/api/v1/teacher/assignments/{assignmentId}/submissions` | 教师 | 查看提交列表 |
| `POST` | `/api/v1/teacher/submissions/{submissionId}/grade` | 教师 | 批改并可发布成绩 |
| `GET` | `/api/v1/student/courses/{courseId}/assignments` | 学生 | 查看课程作业 |
| `POST` | `/api/v1/student/assignments/{assignmentId}/submissions` | 学生 | 提交作业 |
| `GET` | `/api/v1/student/grades` | 学生 | 查看本人已发布成绩 |

## 5. 论坛与预警

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | 查看课程讨论 |
| `POST` | `/api/v1/student/courses/{courseId}/forum/topics` | 学生 | 发帖 |
| `POST` | `/api/v1/student/forum/topics/{topicId}/replies` | 学生 | 回复 |
| `PATCH` | `/api/v1/teacher/forum/topics/{topicId}/visibility` | 教师 | 隐藏或恢复主题 |
| `GET` | `/api/v1/student/warnings` | 学生 | 查看本人学习预警 |
| `GET` | `/api/v1/teacher/courses/{courseId}/warnings` | 教师 | 查看课程预警 |
| `POST` | `/api/v1/teacher/warnings/{warningId}/handle` | 教师 | 处理预警 |

## 6. 考试与题库

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `GET/POST` | `/api/v1/teacher/courses/{courseId}/question-banks` | 教师 | 题库列表、创建题库 |
| `GET/POST` | `/api/v1/teacher/question-banks/{bankId}/questions` | 教师 | 题目列表、创建题目 |
| `GET/POST` | `/api/v1/teacher/courses/{courseId}/exams` | 教师 | 考试安排列表、创建考试 |
| `POST` | `/api/v1/teacher/exams/{examId}/papers` | 教师 | 保存试卷草稿或发布稿 |
| `GET` | `/api/v1/student/courses/{courseId}/exams` | 学生 | 查看考试安排 |
| `POST` | `/api/v1/student/exams/{examId}/attempts` | 学生 | 开始考试 |
| `POST` | `/api/v1/student/exam-attempts/{attemptId}/submit` | 学生 | 提交答卷 |

## 7. AI 接口

AI 接口返回回答、草稿、解释、建议或 SSE 事件，不直接保存正式业务事实。

| 方法 | 路径 | 角色 | 响应 | 说明 |
|---|---|---|---|---|
| `POST` | `/api/v1/ai/courses/{courseId}/qa/stream` | 学生/教师 | `text/event-stream` | 课程知识库问答 |
| `POST` | `/api/v1/ai/lessons/{lessonId}/summary-draft` | 教师 | JSON | 章节摘要草稿 |
| `POST` | `/api/v1/ai/submissions/{submissionId}/comment-draft` | 教师 | JSON | 作业评语草稿 |
| `POST` | `/api/v1/ai/warnings/{warningId}/explanation` | 教师 | JSON | 学习风险解释草稿 |
| `POST` | `/api/v1/ai/exams/paper-suggestions` | 教师 | JSON | 智能组卷建议 |

SSE 事件类型固定为 `meta`、`delta`、`citation`、`done`、`error`。未收到 `done` 时，前端不得把内容视为完整结果。

## 8. 内部 Feign 契约

`edu-feign-api` 定义服务间 Java 契约，当前包含：

| 方法 | 路径 | 调用方 | 提供方 | 说明 |
|---|---|---|---|---|
| `POST` | `/_internal/v1/ai-context/course` | `edu-ai-service` | `edu-biz-service` | 获取授权后的最小课程上下文 |

对应代码：

- `backend/edu-feign-api/src/main/java/com/zhongruan/edu/feign/ai/BizAiContextFeignClient.java`
- `backend/edu-biz-service/src/main/java/com/zhongruan/edu/biz/ai/api/controller/InternalAiContextController.java`

内部接口只传输 DTO，不传输 Entity，不由前端调用。
