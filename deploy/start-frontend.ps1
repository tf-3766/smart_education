[CmdletBinding()]
param(
  [ValidateRange(1, 65535)]
  [int]$Port = 8088,
  [string]$ApiUpstream = 'http://host.docker.internal:18080',
  [switch]$Detach
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $PSScriptRoot 'docker-compose.frontend.yml'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw '未检测到 Docker。请先安装 Docker Desktop 或 Docker Engine + Docker Compose v2。'
}

docker compose version | Out-Null
$env:FRONTEND_HOST_PORT = [string]$Port
$env:API_UPSTREAM = $ApiUpstream

$arguments = @('compose', '--project-directory', $repoRoot, '-f', $composeFile, 'up', '--build')
if ($Detach) { $arguments += '-d' }

Write-Host "前端将发布到 http://localhost:$Port ，API 代理到 $ApiUpstream"
& docker @arguments
if ($LASTEXITCODE -ne 0) { throw "Docker Compose 执行失败，退出码：$LASTEXITCODE" }
