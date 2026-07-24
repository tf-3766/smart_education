# 前端 Docker 部署

前端镜像内部固定使用 **Node 22.13.0** 与 **pnpm 11.7.0** 构建，运行阶段仅保留 Nginx 和静态文件。因此目标电脑无需安装 Node.js 或 npm，只需 Docker Engine（含 Docker Compose v2）。

在仓库根目录运行：

```powershell
.\deploy\start-frontend.ps1 -Detach
```

默认访问地址为 `http://localhost:8088`，前端会把 `/api/` 请求代理到宿主机的 `http://host.docker.internal:18080`。

若网关已作为同一 Docker 网络中的服务运行，例如服务名为 `gateway`：

```powershell
.\deploy\start-frontend.ps1 -ApiUpstream http://gateway:18080 -Port 8088 -Detach
```

停止前端：

```powershell
docker compose --project-directory . -f .\deploy\docker-compose.frontend.yml down
```
