# Edu Backend

JDK 21 + Maven 多模块后端：

- `edu-common`：统一响应、错误码、JWT 等稳定技术协议。
- `edu-feign-api`：服务间 Feign Client 与内部 DTO 契约，不单独部署。
- `edu-gateway`：统一入口、JWT、CORS、traceId 和 AI Redis 限流。
- `edu-biz-service`：认证、文件、课程、作业、成绩、论坛、预警、考试和平台治理。
- `edu-ai-service`：Spring AI、授权上下文、课程 RAG 问答 SSE、会话记忆、课程工具调用、知识库同步、摘要、评语、风险解释、组卷建议和运行状态；未配置模型或向量库时安全降级。

```powershell
.\mvnw.cmd clean verify
```

完整配置、启动顺序和测试账号见 [本地开发说明](../docs/local-development.md)。接口契约以 [API 参考](../docs/api-reference.md) 为准。