# Edu Backend

Maven 多模块后端：

- `edu-common`：稳定技术协议，不包含业务 Entity/Service/DTO。
- `edu-feign-api`：服务间 Feign Client 与内部 DTO 契约，不单独部署。
- `edu-gateway`：统一入口、JWT、CORS、traceId、AI Redis 限流。
- `edu-biz-service`：业务事实、MySQL、Bootstrap SQL 初始化、权限、课程学习、作业/成绩/论坛/预警/考试基础表和审计。
- `edu-ai-service`：当前阶段有可启动骨架、健康检查和 Feign 契约接入；尚未接入模型、向量库、RAG 或公开 SSE。

构建：

```powershell
.\mvnw.cmd clean verify
```

完整本地说明见 [`../docs/local-development.md`](../docs/local-development.md)。
