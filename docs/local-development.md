# 后端第一阶段开发与启动说明

## 1. 当前交付范围

当前已完成基础工程、统一响应、JWT 登录、角色/权限校验、课程学习基础接口、课程审核、Bootstrap SQL 认证/课程/协作学习表、Gateway 路由、`edu-feign-api` 契约模块、AI 服务健康检查和 Biz 课程上下文内部接口。尚未实现注册、找回密码、刷新令牌、Redis Token 黑名单、作业/成绩/考试/论坛/预警的公开业务闭环、真实模型、向量库、RAG 或公开 SSE 业务。

版本基线：JDK 21、Spring Boot 3.5.0、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0、MyBatis-Plus 3.5.12、MySQL 8.0。

版本依据：[Spring Cloud Alibaba 2025.x 版本说明](https://sca.aliyun.com/docs/2025.x/overview/version-explain/)、[Spring Cloud Gateway WebFlux Starter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html)。

## 2. 最终目录树

```text
backend/
├─ pom.xml                         # edu-parent
├─ mvnw / mvnw.cmd / .mvn/        # 固定 Maven 运行环境
├─ .env.example
├─ scripts/import-env.ps1
├─ edu-common/                     # 响应、分页、错误码、异常、JWT、请求上下文
├─ edu-feign-api/                  # 服务间 Feign DTO/Client 契约，不单独启动
├─ edu-gateway/                    # 路由、CORS、JWT、X-Trace-Id、AI 限流预留
├─ edu-biz-service/
│  └─ src/main/
│     ├─ java/.../auth/            # 登录、当前用户、角色权限
│     ├─ java/.../course/          # 课程、章节、课时、资料、选课、学习进度
│     ├─ java/.../assignment/      # 作业、附件、提交 Entity/Mapper 骨架
│     ├─ java/.../grade/           # 成绩 Entity/Mapper 骨架
│     ├─ java/.../forum/           # 论坛 Entity/Mapper 骨架
│     ├─ java/.../warning/         # 学习预警 Entity/Mapper 骨架
│     ├─ java/.../exam/            # 考试、题库、试卷 Entity/Mapper 骨架
│     ├─ java/.../ai/              # Biz 提供给 AI 的内部上下文接口
│     ├─ java/.../shared/          # Security、异常、审计、MyBatis-Plus
│     └─ resources/db/
│        └─ online_education_bootstrap.sql  # 完整建表和本地演示数据
└─ edu-ai-service/                 # 可启动骨架、Actuator 健康检查和 Feign 客户端接入

deploy/docker-compose.yml          # MySQL、Redis、RabbitMQ、Nacos、Qdrant
docs/                              # 架构、编码、数据库、API 与协作规范
```

## 3. 本地配置与环境变量

```powershell
Copy-Item backend\.env.example backend\.env
Copy-Item backend\edu-biz-service\src\main\resources\application-local.yml.example backend\edu-biz-service\src\main\resources\application-local.yml
Copy-Item backend\edu-gateway\src\main\resources\application-local.yml.example backend\edu-gateway\src\main\resources\application-local.yml
Copy-Item backend\edu-ai-service\src\main\resources\application-local.yml.example backend\edu-ai-service\src\main\resources\application-local.yml
. .\backend\scripts\import-env.ps1
```

`application-dev.yml.example` 也已在三个应用服务中提供；共享 dev 环境复制为 `application-dev.yml` 后使用，不能提交真实文件。`edu-common` 与 `edu-feign-api` 是库模块，不需要 profile 文件。

| 变量 | 用途 | 是否必填 |
|---|---|---|
| `DB_URL/DB_USERNAME/DB_PASSWORD` | Biz MySQL 连接 | 是 |
| `MYSQL_ROOT_PASSWORD` | 本地 Compose 初始化 | 本地 Compose 必填 |
| `JWT_SECRET` | Gateway/Biz/AI 共享签名密钥，至少 32 UTF-8 字节 | 是 |
| `JWT_TTL/JWT_ISSUER` | Token 有效期和发行者 | 有默认值 |
| `REDIS_HOST/REDIS_PORT/REDIS_PASSWORD` | Gateway 限流与未来缓存 | 地址有默认值 |
| `RABBITMQ_*` | RabbitMQ 本地基础设施 | 使用 Compose 时必填密码 |
| `NACOS_*` | 服务注册与配置中心 | local/dev 使用 |
| `CORS_ALLOWED_ORIGINS` | 前端允许来源 | 有本地默认值 |
| `GATEWAY_SERVER_PORT/BIZ_SERVER_PORT/AI_SERVER_PORT` | 服务端口 | 有默认值 |

`.env`、`application-local.yml`、`application-dev.yml`、日志、IDE 文件与 `target/` 已被 `.gitignore` 排除。

## 4. 启动基础设施与服务

```powershell
Set-Location backend
. .\scripts\import-env.ps1
docker compose -f ..\deploy\docker-compose.yml up -d
.\mvnw.cmd clean package
```

分别在三个已加载 `.env` 的终端中按顺序启动：

```powershell
java -jar .\edu-biz-service\target\edu-biz-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
java -jar .\edu-ai-service\target\edu-ai-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
java -jar .\edu-gateway\target\edu-gateway-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

顺序：MySQL/Redis/RabbitMQ/Nacos/Qdrant → Biz `18081` → AI `18082` → Gateway `18080`。前端只访问 Gateway。

## 5. SQL 初始化

- 项目只使用 `backend/edu-biz-service/src/main/resources/db/online_education_bootstrap.sql` 初始化数据库；它包含完整表结构、测试账号和演示数据。
- Compose 仅在首次创建 `smart-education-mysql-data` 数据卷时自动执行该脚本。若需要重新初始化，先停止 Compose，再删除该项目的 MySQL 数据卷后重新启动。
- Biz 服务启动时不会修改表结构，也不会执行 Flyway。
- 脚本不使用物理外键；关联一致性由应用服务、唯一约束和索引保证。
- 任何表结构或演示数据变更都必须同步修改这一个脚本，并在空 MySQL 8 数据库中验证。

已有 MySQL 8 时，先创建 `smart_education` 数据库，再手工执行 `online_education_bootstrap.sql`；数据库连接配置使用 `DB_*`。

## 6. 测试账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| STUDENT | `student` | `Student@123` |
| TEACHER | `teacher` | `Teacher@123` |
| ADMIN | `admin` | `Admin@123` |
| TEACHER（越权测试） | `teacher2` | `Teacher@123` |

## 7. 当前 API

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
GET/POST /api/v1/teacher/courses
GET/PUT  /api/v1/teacher/courses/{courseId}
POST /api/v1/teacher/courses/{courseId}/submit-review
POST /api/v1/teacher/courses/{courseId}/publish
POST /api/v1/teacher/courses/{courseId}/offline
GET/POST /api/v1/teacher/courses/{courseId}/chapters
GET/POST /api/v1/teacher/chapters/{chapterId}/lessons
GET/POST /api/v1/teacher/courses/{courseId}/materials
GET  /api/v1/admin/course-reviews
POST /api/v1/admin/course-reviews/{courseId}/approve
POST /api/v1/admin/course-reviews/{courseId}/reject
GET  /api/v1/student/courses/catalog
GET  /api/v1/student/courses
POST /api/v1/student/courses/{courseId}/enroll
GET  /api/v1/student/courses/{courseId}/outline
POST /api/v1/student/lessons/{lessonId}/start
POST /api/v1/student/lessons/{lessonId}/complete
POST /_internal/v1/ai-context/course # 内部 Feign，不经 Gateway 对外暴露
GET  /api/v1/test/student
GET  /api/v1/test/teacher
GET  /api/v1/test/admin
GET  /actuator/health              # 各服务内网健康检查
```

请求追踪头统一为 `X-Trace-Id`。除 login 和 Actuator health 外，接口需要 `Authorization: Bearer <accessToken>`。

### 登录成功

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "expiresAt": "2026-07-06T18:00:00Z",
    "user": {
      "userId": "1001",
      "username": "student",
      "displayName": "测试学生",
      "activeRole": "STUDENT",
      "roles": ["STUDENT"],
      "permissions": ["auth:profile:read", "student:access"]
    },
    "roles": ["STUDENT"],
    "permissions": ["auth:profile:read", "student:access"]
  },
  "errors": [],
  "traceId": "trace-demo-12345678",
  "timestamp": "2026-07-06T16:00:00Z"
}
```

### 未登录或非法 Token

```json
{
  "code": "UNAUTHORIZED",
  "message": "请先登录或检查 Token",
  "data": null,
  "errors": [],
  "traceId": "trace-demo-12345678",
  "timestamp": "2026-07-06T16:00:00Z"
}
```

### 无权限

```json
{
  "code": "FORBIDDEN",
  "message": "你没有查看或操作该资源的权限",
  "data": null,
  "errors": [],
  "traceId": "trace-demo-12345678",
  "timestamp": "2026-07-06T16:00:00Z"
}
```

### 参数错误

```json
{
  "code": "PARAM_VALIDATION_ERROR",
  "message": "请求参数不正确",
  "data": null,
  "errors": [
    {"field": "username", "reason": "用户名不能为空", "rejectedValue": ""},
    {"field": "password", "reason": "密码不能为空", "rejectedValue": null}
  ],
  "traceId": "trace-demo-12345678",
  "timestamp": "2026-07-06T16:00:00Z"
}
```

## 8. 运行测试

```powershell
Set-Location backend
.\mvnw.cmd clean verify
```

无 Docker 时，H2 MySQL 模式执行快速认证、审计与 Bootstrap SQL 测试；真实 MySQL 8 Testcontainers 测试会明确跳过。启动 Docker Desktop 后重新执行同一命令即可运行 MySQL 8 验证。

## 9. 组员开发规则

可直接使用：

- `ApiResponse<T>`、`PageResponse<T>`、`ApiError`；
- `CommonErrorCode`、`BusinessException`；
- `JwtTokenService`、`JwtClaims`；
- `RequestContext`、`RequestSource`、`TraceIds`；
- Biz 内的 `BaseAuditEntity`、`ResourceScopeAuthorizer` 和统一安全/异常设施。

不要随意修改：父 POM 版本矩阵、`edu-common` 公共契约、`edu-feign-api` 服务间契约、Security filter chain、`BaseAuditEntity`、Bootstrap SQL、Gateway 路由和三个服务的基础配置。公共改动必须单独 PR 并说明兼容性。

后续新增规则：

- 数据表：先从 `develop` 更新，登记并使用下一个连续迁移号，例如 `V2__init_course_tables.sql`；禁止改 `V1`、禁止物理外键、必须有审计字段和必要索引。
- 接口：统一 `/api/v1`、Request DTO 使用校验注解、Controller 只做协议适配、返回 VO 而非 Entity。
- 枚举：状态、角色、权限集中定义稳定 code，禁止 ordinal、裸字符串和散落 magic number。
- 权限：角色/功能权限之后仍必须调用资源归属校验；管理员不自动获得评分修改或私人 AI 会话查看权限。
- AI：下一阶段立项后再接入模型、向量库、RAG 与 SSE；本阶段不要向 AI 服务添加传统业务 CRUD。
