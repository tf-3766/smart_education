# AI 自带密钥（BYO Key）请求头契约

前端已实现「浏览器自带大模型密钥」：管理员在 `/admin/ai` 填入密钥，存于当前浏览器
`localStorage`，随每个 AI 请求以请求头发送。本文档约定后端 `edu-ai-service` 需要配合的
读取与使用方式。

## 请求头

- **名称**：`X-AI-Api-Key`
- **值**：DashScope / 阿里百炼 API Key（如 `sk-...`），明文。
- **携带范围**：仅 `/api/v1/ai/*` 路径（前端只对该前缀注入，其它请求不带）：
  - `POST /api/v1/ai/lessons/{lessonId}/summary-draft`
  - `POST /api/v1/ai/submissions/{submissionId}/comment-draft`
  - `POST /api/v1/ai/warnings/{warningId}/explanation`
  - `POST /api/v1/ai/exams/paper-suggestions`
  - `POST /api/v1/ai/courses/{courseId}/qa/stream`（SSE）
  - `GET  /api/v1/ai/admin/status`
- **可选**：请求头可能缺失（用户未配置密钥）。

## 语义（后端需实现）

1. **按请求覆盖**：当 `X-AI-Api-Key` 存在且非空时，用该 key 为**本次请求**构建
   qwen / DashScope 客户端，覆盖环境变量 `DASHSCOPE_API_KEY`。请求结束即丢弃，不落库、不缓存。
2. **回退链**：`请求头 key` → `环境变量 DASHSCOPE_API_KEY` → 都没有则维持现有
   fallback（`AI_CHAT_PROVIDER=none` 时返回「AI 模型尚未配置」文本，见 `AiGenerationConfig`）。
3. **`admin/status` 反映实际可用性**：当请求带 key 时，状态应基于该 key 判定
   `available` / `provider` / `model`，以便前端「测试连接」能校验用户刚填的密钥。
4. **安全**：视为机密，禁止写入日志 / trace / 异常信息；不要回显到响应体。

## 实现提示（Spring）

- Controller 用 `@RequestHeader(value = "X-AI-Api-Key", required = false) String apiKey` 取头，
  透传到生成服务；避免放入全局可日志化的上下文。
- 每请求构建模型客户端（或用请求级作用域），而非复用启动时按 env 配好的单例；
  key 为空时回退到 env 配好的单例。
- SSE `qa/stream` 与普通 POST 走同一取头逻辑。

## 前端已就绪

- 头注入：`smart-education-frontend/src/services/aiKey.ts`（`aiKeyHeader`），
  接入点 `httpClient.request` 与 `services/api/client.ts` 的 `postEventStream`。
- 设置页：`src/domains/admin/AiSettingsPage.vue`（`/admin/ai`）。
- 后端按本契约读取头后，前端零改动即可用浏览器密钥跑真实模型。

## 前置依赖

本契约生效前，需先解决 AI 运行态崩溃（`reactor-http-nio` 线程在清理路径抛
`NoClassDefFoundError: io/netty/buffer/PoolArena$1`，疑似 netty 版本冲突，
用 `dependency:tree -Dincludes=io.netty` 排查并以 netty-bom 统一版本后重打包）。
