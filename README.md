# smart_education

面向学生、教师和管理员的在线教育辅助教学系统。后端采用 Spring Cloud Alibaba 多模块架构，前端采用 Vue 3、Vite 和 TypeScript。

## 项目结构

- `frontend/`：学生端、教师端和管理员端前端，支持演示模式与真实后端模式。
- `backend/`：Gateway、Biz、AI、公共协议和 Feign 契约模块。
- `deploy/docker-compose.yml`：MySQL、Redis、RabbitMQ、Nacos 和 Qdrant 基础设施。
- `tests/integration/`：前后端契约、真实接口和 AI 冒烟脚本。
- `docs/`：API、本地开发、数据库和协作规范。

## 快速开始

后端需要 JDK 21，前端需要 Node.js 20 或更高版本。完整环境变量和服务启动顺序见 [本地开发说明](./docs/local-development.md)。

```powershell
Copy-Item backend\.env.example backend\.env
Set-Location backend
. .\scripts\import-env.ps1
docker compose -f ..\deploy\docker-compose.yml up -d
.\mvnw.cmd clean package
```

前端默认使用本地演示数据：

```powershell
Set-Location frontend
npm ci
npm run dev
```

连接真实 Gateway：

```powershell
$env:VITE_API_MODE = 'real'
$env:VITE_GATEWAY_URL = ''
npm run dev
```

## 核心文档

- [API 参考](./docs/api-reference.md)：项目唯一公开和内部接口文档。
- [本地开发](./docs/local-development.md)：配置、启动、测试账号和验证命令。
- [团队分工](./docs/team-division.md)：模块边界和协作规则。
- [文档索引](./docs/README.md)：其余架构、数据库和历史文档入口。
- [项目设计文档](./在线教育辅助教学系统_项目设计文档_第一组.md)：项目设计 Markdown 源。