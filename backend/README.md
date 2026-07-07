# Edu Backend

Maven 多模块后端：

- `edu-common`：稳定技术协议，不包含业务 Entity/Service/DTO。
- `edu-gateway`：统一入口、JWT、CORS、traceId、AI Redis 限流。
- `edu-biz-service`：业务事实、MySQL/Flyway、权限和审计。
- `edu-ai-service`：当前阶段只有可启动骨架和健康检查；尚未接入模型、向量库、RAG 或 SSE。

构建：

```powershell
.\mvnw.cmd clean verify
```

完整本地说明见 [`../docs/local-development.md`](../docs/local-development.md)。
