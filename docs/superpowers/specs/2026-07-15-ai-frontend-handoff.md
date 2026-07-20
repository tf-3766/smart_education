# AI 前端就绪 · 后端待办交接

本轮前端把 AI 相关能力全部接好并测试通过，剩余工作集中在后端 / 运行态 / 凭证。
本文档汇总「前端已做什么」「后端要做什么」「按什么顺序」，供在 Mac 上推进。

## 前端已完成（lms 分支，均带测试）

| 提交 | 内容 |
|---|---|
| BYO 密钥 | `aiKey.ts`：密钥存浏览器 localStorage；仅 `/api/v1/ai/*` 注入 `X-AI-Api-Key` 头（`httpClient.request` 与 SSE `postEventStream`）。管理员 `/admin/ai` 设置页：状态 + 密钥输入/保存/清除/测试连接。 |
| 三能力切真实 | `commentDraft` / `warningExplanation` / `paperSuggestion` 真实模式请求后端；demo 回退。组卷 `courseId` 字符串直传保雪花精度。 |
| 去假置信度 | `AiResult.confidence` 改可选；QA 不再写死 0.8；`AiResultPanel` 仅在有置信度时展示。 |
| 教师端 UI 接线 | 批改台「AI 评语草稿」→ 采用到评语；预警「生成 AI 解读」→ 采用为干预记录；组卷「AI 组卷建议」。`AiResultPanel` 增加 adopt 动作。 |

学生课程答疑（`qaStream`，SSE）本就走真实接口，未改。

## 后端待办（按优先级）

### #1 修 AI 运行态崩溃（总阻塞）

现象：`edu-ai-service` 启动成功后，`reactor-http-nio` 事件循环线程在**终止清理路径**
（`SingleThreadEventExecutor$4.run:1052` → `freeChunk`）抛
`NoClassDefFoundError: io/netty/buffer/PoolArena$1`，逐个死亡 → HTTP 无存活 worker →
`/actuator/health` 超时。netty 报错是级联症状，非根因。

排查：
```
./mvnw -pl edu-ai-service dependency:tree -Dincludes=io.netty
```
- 若出现**多个 netty 版本**（netty-buffer / netty-common 不一致）→ 用 netty-bom 在
  dependencyManagement 统一版本后重打包。
- 若版本一致 → 查打包是否裁剪掉合成内部类：
  `unzip` 出嵌套 `netty-buffer-*.jar`，`jar tf | grep 'PoolArena\$'` 应含 `PoolArena$1.class`。
- 单纯重启无效，必须对齐后重打包。

### #2 后端读 `X-AI-Api-Key` 头（按已有契约）

见 `2026-07-15-ai-byo-key-header-contract.md`。要点：
- `@RequestHeader(value="X-AI-Api-Key", required=false)` 取头，按请求覆盖 env
  `DASHSCOPE_API_KEY` 构建 qwen 客户端；用完即弃、不落库、不进日志。
- 回退链：请求头 key → env key → 现有 fallback（`AI_CHAT_PROVIDER=none` 返回「AI 模型尚未配置」）。
- `admin/status` 带 key 时反映该 key 的实际可用性（前端「测试连接」依赖）。
- 覆盖端点：`comment-draft` / `warnings/explanation` / `exams/paper-suggestions` /
  `lessons/summary-draft` / `courses/qa/stream` / `admin/status`。

请求体契约（前端已按此发送）：
- comment-draft / explanation：`{ instruction?: string|null }`（≤500）。
- paper-suggestions：`{ courseId: string(Long), questionCount: int, totalScore: number, requirements?: string|null }`。

配置真实模型：`AI_CHAT_PROVIDER=dashscope` + 注入 `DASHSCOPE_API_KEY`（或靠前端头）。
`application.yml` 已支持 qwen-plus。

### #3 端到端验证

#1+#2 完成后：管理员 `/admin/ai` 填 key → 测试连接可用 → 教师三能力 + 学生答疑
出真实 qwen 结果。跑 `AiServiceApplicationTest`（覆盖 6 接口 + 越权）回归。

### #4 增量（最后）

- 生成记录落库：`edu_ai_generation_record`（Mapper 已存在但业务未调用），
  每次 AI 生成写一条。
- 真 RAG：Qdrant 当前 `vectorStore=false`，答疑只是把课时正文拼进提示词
  （`AiApplicationService.java:238`）；接入向量检索后前端无需改动。
- 「采用/确认」持久化：若需记录教师采用了哪条草稿，后端加 adopt 端点，前端再接。

## 契约不变量

- 头名固定 `X-AI-Api-Key`；改名需前后端同步（前端在 `services/aiKey.ts`）。
- AI 只产出草稿/建议，正式数据仍由业务接口经人工确认写入——前端「采用」只填表单，不直接落库。
