# 文档索引

`docs/` 不会被 Spring Boot、Vue 或 Docker Compose 在运行时自动读取；它的作用是统一设计、接口、协作和启动规则，避免三个人各自按不同理解开发。

## 当前必须维护

| 文档 | 用途 |
|---|---|
| [api-reference.md](./api-reference.md) | 前端联调入口，区分已实现、待实现和内部接口 |
| [team-division.md](./team-division.md) | 当前三人任务边界和优先级 |
| [local-development.md](./local-development.md) | 本地启动、环境变量、Bootstrap SQL 初始化 |
| [database-conventions.md](./database-conventions.md) | 表设计和唯一 Bootstrap SQL 规则 |
| [module-ownership.md](./module-ownership.md) | 后端模块、表和公共文件归属 |

## 契约与设计参考

- `api-reference.md`：项目全部公开、内部和测试接口的唯一文档。
- `migration-register.md`：Bootstrap SQL 变更记录。

## 历史与辅助文档

- `adr/`：历史架构决策；被标记为 Superseded 的 ADR 不再作为当前规则。
- `dev-log/`：开发记录，不影响程序运行。
- `team-division.md`：当前三人的任务边界、交付顺序和联调规则。
