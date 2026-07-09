# 团队开发与 Git 协作流程

> 目标：让两名后端和一名前端可以并行开发，同时把接口漂移、权限遗漏、数据库冲突、公共配置冲突和页面联调风险控制在合并前。

## 1. 分支模型

| 分支 | 用途 | 规则 |
|---|---|---|
| `main` | 可演示、可发布的稳定版本 | 只接受从 `develop` 发起的发布 PR；禁止直接提交 |
| `develop` | 日常集成分支 | 功能/修复 PR 的默认目标；必须通过检查和评审 |
| `feature/*` | 新功能 | 从最新 `develop` 创建，合并后删除 |
| `fix/*` | 非紧急缺陷修复 | 从最新 `develop` 创建，合并后删除 |

建议分支名：

```text
feature/course-create-api
feature/assignment-submission
feature/ai-course-qa-sse
fix/grading-deadline-validation
fix/gateway-ai-rate-limit
```

规则：

- 一个分支只处理一个清晰目标，避免“顺便重构其他模块”。
- 功能超过约 3～5 个可独立评审的用例时应拆分。
- 分支存活期间定期同步 `develop`；在自己的功能分支解决冲突，不把冲突直接推给集成者。
- 不使用个人名作为分支唯一含义，如 `zhangsan-dev`、`mybranch`。
- 禁止 force push 到 `main`、`develop`；功能分支如需改写历史，先确认没有其他人基于该分支工作。

## 2. 开发前：契约先行

每个功能开始编码前必须先完成以下最小设计：

1. 在 OpenAPI/接口文档中增加或更新路径、方法、请求、响应和错误码。
2. 明确角色、功能权限、资源数据范围和对象状态约束。
3. 如涉及数据库，提交表/字段、索引、状态枚举和 Flyway 计划。
4. 如跨服务，明确同步 Contract DTO 或 RabbitMQ 事件版本、超时、幂等和失败方式。
5. 给出至少一个正常示例和一个越权/冲突示例。

契约评审通过后再让前后端并行实现。接口字段临时口头约定不算契约。

### 2.1 推荐 issue/任务模板

```text
目标：
非目标：
负责模块：
接口前缀：
角色与数据范围：
状态流转：
数据库变更：
跨模块/跨服务依赖：
验收场景：
```

## 3. 提交信息规范

使用 Conventional Commits 风格：

```text
<type>(<scope>): <subject>
```

示例：

```text
feat(course): add course creation API
fix(assignment): validate submission deadline
docs(api): update grading API contract
test(auth): cover teacher resource scope
refactor(ai): isolate vector store adapter
chore(build): align Spring BOM versions
```

### 3.1 type

| type | 含义 |
|---|---|
| `feat` | 新能力 |
| `fix` | 缺陷修复 |
| `docs` | 文档/契约 |
| `test` | 测试 |
| `refactor` | 不改变外部行为的重构 |
| `perf` | 性能改进 |
| `build` | 构建和依赖 |
| `ci` | CI/CD |
| `chore` | 其他维护 |

### 3.2 scope

优先使用模块名：`auth`、`course`、`learning`、`assignment`、`grade`、`exam`、`warning`、`ai`、`gateway`、`db`、`api`、`deploy`。

### 3.3 提交粒度

- 一个提交表达一个可解释改变，并保持可构建/可测试。
- 数据库迁移、Entity/Mapper、接口和测试可以在同一功能提交组中，但不要混入无关格式化。
- 禁止提交密钥、真实账号、IDE 私有文件、构建产物和大文件。
- 不提交“temp”“update”“final”这类无法审计的消息。

## 4. Pull Request 规范

### 4.1 基本要求

- 功能/修复 PR 默认合入 `develop`；发布 PR 才从 `develop` 合入 `main`。
- 不允许直接向 `main` 提交。
- PR 标题遵循提交信息格式。
- PR 尽量控制在一个模块/一个目标；公共变更与业务变更能拆则拆。
- 至少一名非作者评审；涉及公共契约、权限、数据库、POM、部署时必须有对应所有者评审。
- 作者负责解决评审意见和冲突，不能由合并者猜测意图。

### 4.2 PR 描述模板

```markdown
## 目标

## 变更范围
- 服务/模块：
- 接口：
- 数据库：
- 公共配置：

## 权限与数据范围

## 兼容性
- 是否修改现有接口/事件：
- 是否需要前端同步：
- 是否支持滚动升级：

## 验证
- [ ] 单元测试
- [ ] 集成测试
- [ ] 权限/越权测试
- [ ] Flyway 空库/升级验证
- [ ] 本地联调或契约测试

## 风险与回滚
```

### 4.3 每个 PR 至少检查

- [ ] 接口兼容性：路径、字段、类型、分页、HTTP 状态、错误码是否符合契约。
- [ ] 权限：角色、功能权限、资源归属和状态约束是否全部校验。
- [ ] 参数校验：格式、长度、跨字段和服务端时间规则是否覆盖。
- [ ] 异常处理：没有泄露堆栈/SQL，HTTP 状态与错误码正确。
- [ ] 数据库迁移：Flyway 文件不可变、空库和升级均验证、索引/约束合理。
- [ ] 测试：正常、边界、冲突、越权和幂等场景。
- [ ] 审计：高风险操作记录操作者、对象、结果和 traceId。
- [ ] 日志：无密码、Token、学生作业全文、考试答案或私人 AI 会话。
- [ ] AI 边界：只返回建议/草稿/引用/任务状态，正式业务变更由 Biz 完成。

## 5. 公共变更的额外说明

修改以下内容时，PR 描述必须单独列出影响面、迁移方式和需要同步的组员：

- Maven 父 `pom.xml`、BOM、插件或 JDK 版本。
- `edu-common` 公共类型。
- `edu-feign-api` 服务间契约类型。
- Gateway 路由、CORS、JWT、安全过滤器、AI 限流。
- `application.yml`、Nacos 配置、环境变量名。
- Flyway 脚本、核心表、公共索引。
- OpenAPI 公共响应、分页、错误码。
- RabbitMQ exchange、queue、routing key、事件 schema。
- Docker Compose、端口、健康检查、CI 工作流。
- 前端 API client、路由守卫、角色菜单、全局错误处理和演示数据切换。

公共修改不得藏在大业务 PR 末尾。能独立合并时应先提交一个小 PR，再让业务分支同步。

## 6. 高冲突目录与处理规则

| 高风险位置 | 主要风险 | 处理规则 |
|---|---|---|
| `backend/pom.xml`、各模块 `pom.xml` | 依赖/BOM 冲突 | 由基础负责人评审；同一时段指定一人合并依赖升级 |
| `edu-common/**` | 全服务编译影响、错误抽象 | 只放技术契约；修改需至少两个服务验证 |
| `edu-feign-api/**` | 服务间契约漂移、调用双方编译影响 | 只放 Feign Client 和内部 DTO；修改需提供方和消费方同时验证 |
| `edu-gateway/**` | 全入口安全和路由影响 | 网关负责人所有；业务组只提契约需求 |
| `shared/security/**` | 权限绕过 | 权限负责人评审；必须补越权测试 |
| `db/migration/**` | 版本号、表结构冲突 | 时间戳版本；已执行脚本不可改；冲突方新建更晚 migration |
| `application*.yml`、Nacos | 环境覆盖、密钥泄露 | 配置负责人评审；密钥只用环境/secret |
| OpenAPI 根文件/公共 schema | 大量文本冲突 | 按模块拆文件，根文件只引用；契约所有者合并 |
| MQ 公共声明 | 消费重复/丢失 | 先更新事件契约，版本化 routing key/schema |
| `docker-compose.yml`、CI | 全员本地/流水线受影响 | 部署负责人评审并提供启动验证 |

### 6.1 冲突解决顺序

1. 先确认两边的业务意图和数据所有者，不能只让 Git 自动选择一边。
2. 由文件/模块所有者主持解决；原作者验证自己的逻辑没有丢失。
3. 数据库 migration 冲突通过新增版本解决，不删除已共享脚本。
4. 接口契约冲突先确定最终契约，再改实现和测试。
5. 解决后重新执行完整构建、相关集成测试和权限测试。

## 7. 模块边界协作

- 模块负责人拥有表、接口前缀和状态枚举，详见 [`module-ownership.md`](./module-ownership.md)。
- 依赖模块只能调用公开的 application interface、HTTP contract 或事件；禁止直接调用对方 Mapper。
- 需要修改他人模块时，优先提交接口需求或由所有者实现；紧急共同修改也必须由所有者 review。
- 服务间禁止共享数据库和复制业务 Entity；`edu-common` 只共享技术协议，`edu-feign-api` 只共享服务间契约 DTO。
- 多模块功能按“契约 → 提供方 → 消费方 → 端到端测试”的顺序合并，避免互相等待未定义接口。

## 8. 数据库协作

- 任何 schema 变更必须有 Flyway migration。
- 禁止多人手工直改线上、测试或共享数据库结构。
- 已在共享环境执行的 migration 不得修改 checksum。
- 开发前先同步 `develop` 中已有 migration；合并前重新从空库执行全量脚本。
- 跨模块字段由表所有者创建；请求方不能“先加一列以后再说”。
- 演示数据、测试 fixtures 与生产结构 migration 分离。

## 9. 接口兼容策略

- `/api/v1` 内优先做向后兼容的新增字段；删除/重命名字段需新版本或明确迁移窗口。
- 新增响应字段默认允许旧前端忽略；改变字段类型、含义、是否可空属于破坏性变更。
- 错误码不能复用成新含义。
- MQ 事件只追加可选字段；破坏性变化发布 `.v2` 事件并保留过渡消费者。
- 数据库采用 expand → migrate/backfill → contract 三阶段，避免一次部署同时删列和改代码。

## 10. CI 合并门禁

PR 至少通过：

1. Maven 格式/静态检查、编译。
2. 单元测试。
3. MySQL Testcontainers + Flyway validate/集成测试。
4. 权限/安全关键测试。
5. OpenAPI/事件 schema 校验。
6. 密钥扫描、依赖漏洞基础扫描。
7. 受影响模块启动和健康检查。

涉及 Gateway、AI SSE、RabbitMQ 或数据库时，增加对应集成测试。只有文档 PR 可以跳过运行时测试，但仍需链接、格式和术语检查。

## 11. 合并与发布

- 推荐 squash merge，PR 标题成为主提交；若团队需要保留分步提交，可统一使用 merge commit，但不能混乱使用。
- 合并 `develop` 前保持分支最新并通过 CI。
- 发布时从 `develop` 发 PR 到 `main`，列出版本、migration、配置变更、回滚方案和已知问题。
- `main` 打版本 tag；禁止使用移动的 `latest-final` 之类 tag。
- 数据库 migration 上线前备份；应用回滚必须确认旧版本是否兼容已执行的新 schema。

## 12. Definition of Done

一个后端功能只有同时满足以下条件才算完成：

- 接口契约已合并并与实现一致。
- 角色、权限、资源范围和状态规则已实现并测试。
- 必要的 Flyway migration 可从空库和现有库执行。
- 正常、参数错误、无权限、不存在、冲突场景有自动测试。
- 日志、审计、错误码、幂等和并发策略符合规范。
- 依赖方拿到测试数据、接口示例和可运行环境。
- PR 通过评审和 CI，且没有留下未说明的公共配置变化。
