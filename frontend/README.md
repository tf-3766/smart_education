# 在线教育辅助教学系统前端

Vue 3 + Vite + TypeScript 前端，覆盖学生、教师和管理员三类角色。界面采用信息密集的控制台风格，并通过统一 API 层在演示数据与真实 Gateway 之间切换。

## 安装与验证

项目统一使用 npm，`package-lock.json` 是唯一依赖锁文件。

```powershell
npm ci
npm test
npm run type-check
npm run build
npm run dev
```

## 运行模式

- 演示模式（默认）：数据位于 `src/services/api/demo/db.ts`，通过 `localStorage` 保存业务演示状态。
- 真实模式：设置 `VITE_API_MODE=real`，所有业务请求经 Gateway 发往后端；失败时不会静默回退演示数据。

局域网或同源代理模式：

```powershell
$env:VITE_API_MODE = 'real'
$env:VITE_GATEWAY_URL = ''
npm run dev
```

`VITE_GATEWAY_URL` 未设置时默认使用 `http://localhost:18080`。置为空字符串时，请求使用同源 `/api`，由 Vite 代理到本机 Gateway。

## 代码结构

- `src/domains/`：学生、教师、管理员、账号和系统页面。
- `src/services/api/`：按业务域拆分的契约 API 与演示实现。
- `src/stores/`：登录态和站内信状态。
- `src/components/`：共享交互组件。
- `src/tests/`：Vitest 页面、契约和状态测试。

接口字段和实现状态以仓库根目录的 [API 参考](../docs/api-reference.md) 为准，不在前端目录维护第二份接口文档。

## AI 配置

前端只显示 AI 服务返回的真实状态，不保存或发送模型密钥。百炼密钥、聊天/嵌入模型和 Qdrant 均通过后端环境变量配置。教师可在课程内容页查看并同步课程知识库；学生课程问答会携带会话编号，支持多轮上下文与课程目录工具调用。

AI 回答、评语、风险解释和组卷建议均是草稿或辅助信息，不会自动修改正式成绩、评语、试卷或预警状态。