# 后端开发约定

> 本文约束三个服务和所有后端组员。若代码与本文冲突，以合并前更新并评审后的文档为准。

## 1. 基本原则

- 使用 JDK 21、UTF-8、Maven Wrapper，禁止依赖个人机器上的未声明环境。
- 按业务模块组织代码，再在模块内分层；禁止全项目只有一个巨大的 `controller/service/mapper` 包。
- Controller 只处理协议转换，Service 组织业务用例，权限和状态规则必须可测试。
- 业务数据库 Entity、接口 DTO、返回 VO 和服务间 Contract 必须分开。
- 不为了“分层”创建只调用下一层同名方法的浅层类；复杂规则应集中在清晰的 module interface 后。

## 2. 推荐包结构

根包示例：`com.example.edu`。正式创建骨架时将 `example` 替换为团队确定的组织名，之后禁止随意变更。

### 2.1 `edu-biz-service`

```text
com.example.edu.biz
├─ BizServiceApplication
├─ shared
│  ├─ config
│  ├─ exception
│  ├─ security
│  ├─ audit
│  ├─ idempotency
│  └─ persistence
├─ auth
├─ user
├─ course
├─ learning
├─ assignment
├─ grade
├─ exam
├─ warning
├─ announcement
└─ forum
```

每个业务模块内部采用相同结构：

```text
course/
├─ api/
│  ├─ controller/
│  ├─ dto/request/
│  └─ vo/
├─ application/
│  ├─ service/
│  ├─ command/
│  ├─ query/
│  └─ assembler/
├─ domain/
│  ├─ model/
│  ├─ enums/
│  ├─ rule/
│  └─ event/
└─ infrastructure/
   ├─ persistence/entity/
   ├─ persistence/mapper/
   ├─ client/
   └─ messaging/
```

小模块可以省略空目录，但不能把不同职责混回一个 `util` 包。跨模块调用优先通过对方 `application` 暴露的少量 interface；禁止直接调用其他模块的 Mapper。

### 2.2 `edu-gateway`

```text
com.example.edu.gateway
├─ config
├─ security
├─ filter
├─ ratelimit
├─ routing
├─ exception
└─ observability
```

### 2.3 `edu-ai-service`

```text
com.example.edu.ai
├─ api/
│  ├─ controller
│  ├─ dto
│  └─ sse
├─ application/
│  ├─ qa
│  ├─ summary
│  ├─ grading
│  ├─ warning
│  ├─ paper
│  └─ indexing
├─ domain/
│  ├─ model
│  ├─ enums
│  └─ policy
├─ infrastructure/
│  ├─ modelprovider
│  ├─ vectorstore
│  ├─ bizclient
│  ├─ redis
│  └─ messaging
└─ shared
```

## 3. 类型职责边界

| 类型 | 职责 | 允许 | 禁止 |
|---|---|---|---|
| Controller | HTTP/SSE 协议适配 | 路由、鉴权注解、参数绑定、`@Valid`、调用应用 Service | 业务判断、事务、Mapper、拼 SQL、手工解析 JWT |
| Application Service | 一个完整业务用例 | 权限/状态规则编排、事务、调用本模块接口、发布领域事件 | 拼接 HTTP 响应、依赖前端字段名、返回 Entity |
| Domain Rule/Service | 可复用的业务规则和状态流转 | 纯计算、显式输入输出、单元测试 | 直接操作 Web、数据库或 MQ |
| Mapper | MyBatis-Plus 持久化适配 | 单表 CRUD、明确 SQL、分页查询 | 权限决策、跨模块流程、返回 VO 给 Controller |
| Entity | 数据库表映射 | 表字段、MyBatis-Plus 注解、持久化版本 | 作为请求/响应、承载前端展示字段、跨服务传输 |
| Request DTO | 接收并校验接口输入 | Bean Validation、字段级格式约束 | 数据库注解、输出字段、通用 `Map<String,Object>` |
| Command/Query | 应用层内部输入 | 经过协议转换的稳定业务参数 | Web 注解、Entity 泄漏 |
| VO | 对外返回模型 | 按页面/用例组织、脱敏、稳定字段 | 数据库注解、懒加载、直接继承 Entity |
| Assembler | 类型转换 | Request→Command、Entity/Result→VO、Contract 转换 | 查数据库、调用远程服务、执行业务决策 |
| Enum | 稳定的角色、状态、类型语义 | code、description、允许的流转辅助方法 | 使用 ordinal 落库、在 Controller 硬编码字符串 |
| Contract DTO | 服务间版本化契约 | 最小字段、不可变 record、显式版本 | 复制完整 Entity、复用内部 VO |

### 3.1 Controller 硬性约束

Controller 方法通常只包含四步：

1. 接收路径、查询和请求体参数。
2. 由 Spring Security 获取当前身份。
3. 调用一个应用 Service 用例。
4. 将结果组装为统一响应。

禁止在 Controller 中：

- 写 `if (role.equals("ADMIN"))` 等业务权限逻辑。
- 使用 `QueryWrapper`、Mapper 或事务注解。
- 计算成绩、判断截止时间、改变状态。
- 捕获所有异常并统一返回 HTTP 200。

## 4. 命名规范

### 4.1 Java 类型

| 对象 | 规则 | 示例 |
|---|---|---|
| Controller | `<场景><资源>Controller` | `TeacherAssignmentController` |
| 应用 Service | `<用例/资源>ApplicationService` | `AssignmentGradingApplicationService` |
| 领域规则 | `<业务语义>Rule/Policy` | `SubmissionDeadlinePolicy` |
| Mapper | `<资源>Mapper` | `CourseMapper` |
| Entity | `<资源>Entity` | `CourseEntity` |
| 创建请求 | `<资源>CreateRequest` | `CourseCreateRequest` |
| 更新请求 | `<资源>UpdateRequest` | `AssignmentUpdateRequest` |
| 查询请求 | `<资源>Query` | `CoursePageQuery` |
| 详情返回 | `<资源>DetailVO` | `CourseDetailVO` |
| 列表项返回 | `<资源>ListItemVO` | `AssignmentListItemVO` |
| 转换器 | `<资源>Assembler` | `CourseAssembler` |
| 状态枚举 | `<资源>Status` | `SubmissionStatus` |
| 领域异常 | `<资源><原因>Exception` | `CourseStateConflictException` |

不使用 `IUserService` + `UserServiceImpl` 作为默认组合。只有确有两个实现时才定义实现接口；否则直接使用语义明确的应用 Service。

### 4.2 方法和变量

- 查询单个：`getRequiredCourse`（不存在抛异常）、`findCourse`（返回 Optional）、`getCourseDetail`（用例结果）。
- 查询集合：`listCourses`；分页：`pageCourses`。
- 命令：`createCourse`、`publishAssignment`、`submitAssignment`、`gradeSubmission`。
- 布尔值：`enabled`、`published`、`allowResubmission`，避免含义含混的 `flag`。
- 集合使用复数；ID 使用 `courseId`、`assignmentId`，禁止到处使用无语义 `id`。
- 缩写只保留团队公认形式：`DTO`、`VO`、`SSE`、`JWT`；变量使用 `dto`、`vo`、`jwt`。

### 4.3 常量与枚举

- 角色：`RoleCode.STUDENT/TEACHER/ADMIN`。
- 权限：集中为 `PermissionCode.COURSE_CONTENT_WRITE` 等类型安全常量。
- 状态落库使用稳定 code，例如 `PENDING_REVIEW`，禁止使用枚举 ordinal。
- 错误码由各模块 `XxxErrorCode` 维护，禁止散落字符串。

## 5. RESTful API 规范

- 公共版本前缀统一为 `/api/v1`；内部接口为 `/_internal/v1`。
- 资源路径使用小写复数名词和连字符：`/course-categories`。
- 面向不同角色且返回/动作明显不同的接口使用角色域：
  - `/api/v1/student/courses`
  - `/api/v1/teacher/courses/{courseId}`
  - `/api/v1/admin/course-reviews/{reviewId}`
- 路径只表达资源归属，筛选、分页和排序放查询参数。
- 动词优先由 HTTP 方法表达。只有无法自然表达的业务命令使用子资源/动作：
  - `POST /assignments/{assignmentId}/submissions`：正式提交。
  - `POST /courses/{courseId}/review-submissions`：提交课程审核。
  - `POST /grades/{gradeId}/publication`：发布成绩。
- 不使用 `/getCourseList`、`/deleteByIds`、`/doPublish`。

HTTP 方法和状态：

| 操作 | 方法 | 成功状态 |
|---|---|---|
| 查询单个/列表 | GET | 200 |
| 创建资源 | POST | 201，返回资源 ID/详情 |
| 完整可替换更新 | PUT | 200 |
| 局部更新或状态命令 | PATCH/POST 子资源 | 200 |
| 删除 | DELETE | 204 或 200；项目内按资源统一 |
| 异步任务创建 | POST | 202，返回 taskId |

错误必须使用正确 HTTP 状态，不得“永远 200 + code 表示失败”。统一 JSON 和示例见 [`api-style.md`](./api-style.md)。

## 6. 分页与列表查询

统一请求参数：

- `page`：从 1 开始，默认 1。
- `size`：默认 20，允许 10/20/50/100，最大 100。
- `sort`：`field,direction`，例如 `createdAt,desc`；只允许后端白名单字段。
- `keyword`：模糊搜索；必须限定可搜索列。
- 模块筛选使用清晰名称：`courseId`、`status`、`startAt`、`endAt`。

统一响应数据：

```json
{
  "records": [],
  "page": 1,
  "size": 20,
  "total": 0,
  "totalPages": 0
}
```

- Controller 不直接返回 MyBatis-Plus `Page`、`IPage`。
- 禁止客户端传数据库列名；API 字段由后端映射到白名单列。
- 列表必须有稳定的次级排序，通常为 `id DESC`，避免翻页重复/遗漏。

## 7. 统一响应与异常

普通 JSON 接口统一使用：

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "traceId": "01J...",
  "timestamp": "2026-07-06T20:30:00.123+08:00"
}
```

- `data` 可以为对象、数组、分页对象或 `null`。
- `message` 面向用户且不包含堆栈、SQL、内部 URL、模型密钥。
- SSE 建立连接后不套 `ApiResponse`，按事件协议返回。

基础设施错误码使用稳定、可读的符号名称；业务模块后续使用同一方式扩展：

| 范围 | 示例 |
|---|---|
| 通用与参数 | `PARAM_VALIDATION_ERROR`、`RESOURCE_NOT_FOUND`、`RESOURCE_CONFLICT`、`OPERATION_NOT_ALLOWED` |
| 认证授权 | `UNAUTHORIZED`、`TOKEN_EXPIRED`、`FORBIDDEN`、`INVALID_CREDENTIALS` |
| 文件 | `FILE_UPLOAD_FAILED` |
| AI | `AI_SERVICE_UNAVAILABLE`、`AI_NO_RELIABLE_CONTEXT`、`SSE_STREAM_INTERRUPTED` |
| 未知异常 | `INTERNAL_ERROR` |

规则：

- 错误码一经前后端使用不得更改含义，只能新增。
- HTTP 状态表达协议语义，业务错误码表达稳定业务原因。
- 每个模块维护自己的枚举，`shared` 只定义接口和通用码。
- 全局异常处理器映射已知异常；未知异常记录完整服务端日志，对外返回 `INTERNAL_ERROR`。

## 8. 参数校验

- Request DTO 使用 Jakarta Bean Validation：`@NotNull`、`@NotBlank`、`@Size`、`@Pattern`、`@Positive` 等。
- Controller 请求体使用 `@Valid`，查询对象使用 `@Validated`。
- 简单格式约束放 DTO；跨字段、权限、数据库存在性和状态流转放应用/领域层。
- 创建与更新请求分开，避免复杂 validation groups 让必填规则难读。
- ID 必须为正 Long；JSON 中按字符串序列化，避免 JavaScript 精度丢失。
- 字符串入库前做 trim 和长度限制；富文本必须经过安全清洗。
- 文件格式、真实 MIME、大小、数量在服务端再次校验，不能只信扩展名和前端限制。
- 禁止使用 `Map<String, Object>` 接收复杂业务请求；动态配置也要定义带版本的 DTO/schema。

## 9. Entity 和审计字段

核心表 Entity 统一继承/组合持久化基类，字段定义见 [`database-conventions.md`](./database-conventions.md)：

- `id: Long`
- `createdAt: LocalDateTime/Instant`
- `createdBy: Long`（系统任务固定为 `0`）
- `updatedAt: LocalDateTime/Instant`
- `updatedBy: Long`（系统任务固定为 `0`）
- `deleted: Integer`（只允许 `0/1`）
- `version: Integer`

约束：

- 使用 MyBatis-Plus 自动填充创建/更新字段。
- `@TableLogic` 统一使用 `deleted=0/1`。
- `@Version` 用于会发生并发编辑的核心表；更新冲突返回 409。
- 审计人来自认证上下文；系统任务使用团队规定的 system actor ID，不允许随意写 `null`。
- Entity 只在 persistence 和 application 内部流动，禁止直接作为 Controller 返回值。

## 10. 时间、时区和数值

- 数据库存储 UTC 的 `DATETIME(3)`；应用通过统一 `Clock` 获取时间，禁止散落 `LocalDateTime.now()`。
- API 使用 RFC 3339，带偏移量：`2026-07-06T20:30:00.123+08:00`。
- 纯日期使用 `yyyy-MM-dd`；学期等业务周期单独建字段，不用字符串拼接。
- 截止时间、开考时间、提交时间由服务端判定；前端倒计时只用于显示。
- 分数、比例使用 `BigDecimal` 和 MySQL `DECIMAL`，禁止 `float/double` 参与正式成绩计算。
- 数据库 ID 使用 `BIGINT`/Java `Long`，对外 JSON 序列化为字符串。

## 11. 事务、幂等与并发

- `@Transactional` 放应用 Service 的写用例上，不放 Controller/Mapper。
- 事务内禁止等待模型生成、调用不受控远程服务或上传大文件。
- 写接口需要业务幂等时使用 `Idempotency-Key` 或明确业务唯一键；RabbitMQ 消费必须幂等。
- 更新请求携带 `version` 或通过 `If-Match` 扩展，冲突返回 409，不静默覆盖他人修改。
- 发布成绩、交卷、课程审核等高风险命令必须在服务端再次读取当前状态并加乐观锁/必要行锁。

## 12. 日志规范

统一结构化字段：

- `timestamp`、`level`、`service`、`traceId`。
- `userId`、`role`（已登录时）、`operation`、`resourceType`、`resourceId`。
- `errorCode`、异常类型、耗时。

级别：

- `INFO`：启动、关键业务命令结果、MQ 消费结果、AI 任务状态；避免记录每一行循环。
- `WARN`：可恢复业务异常、限流、重试、乐观锁冲突、外部依赖退化。
- `ERROR`：未知异常、数据一致性问题、重试耗尽、DLQ。
- `DEBUG`：开发诊断；生产默认关闭高噪声 SQL 和完整请求体。

禁止记录：密码、JWT/刷新令牌、Cookie、模型/API 密钥、学生提交全文、考试答案、私人 AI 会话正文、文件二进制和敏感个人信息。需要审计时记录对象 ID、动作、前后关键字段摘要和人工操作者，不把应用日志当业务审计表。

## 13. 配置规范

- `application.yml` 只放通用默认值；环境差异由环境变量/Nacos 配置覆盖。
- 密钥、数据库密码、JWT 私钥、模型 key 不提交 Git。
- 所有配置项使用统一前缀，如 `edu.security.*`、`edu.ai.*`。
- 新配置必须有默认值、用途说明、环境影响和测试覆盖。
- 禁止业务代码直接读取散落字符串配置；使用 `@ConfigurationProperties` 绑定并校验。

## 14. 测试规范

- 领域规则：纯单元测试，覆盖正常、边界和非法状态。
- 应用 Service：通过接口 fake/in-memory adapter 测试用例结果，不穿透内部实现细节。
- Mapper/Flyway：使用 MySQL Testcontainers 做集成测试，不用 H2 假装 MySQL 8。
- Controller：验证参数、HTTP 状态、响应结构和安全边界。
- Feign/MQ：做契约测试、幂等和重试耗尽测试。
- SSE：验证事件顺序、取消、超时、引用、错误和连接关闭。
- 权限测试至少包含：本人成功、同角色越权资源失败、跨角色失败、资源状态不允许。

## 15. 明确禁止项

- 禁止在 Controller 中写业务逻辑或直接调用 Mapper。
- 禁止 Entity 直接作为请求或响应对象。
- 禁止用 `Map` 接收复杂业务请求。
- 禁止硬编码角色、状态、权限字符串、错误码。
- 禁止 AI/Gateway 引用 Biz 的 Entity/Mapper 包。
- 禁止跨模块直接访问 Mapper。
- 禁止异常全部返回 HTTP 200。
- 禁止使用枚举 ordinal 落库。
- 禁止在业务层返回 MyBatis-Plus `Page`、`QueryWrapper` 等框架类型给 API 层。
- 禁止把通用 `util` 当作无归属代码仓库；可复用代码必须有明确 module 和 interface。
