[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$backendRoot = $PSScriptRoot
$envFile = Join-Path $backendRoot '.env'

function Import-DotEnv {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing local configuration file: $Path"
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        if ($line -notmatch '^\s*([^#=]+?)\s*=\s*(.*)$') {
            throw "Cannot parse .env line: $line"
        }

        $name = $Matches[1].Trim()
        $value = $Matches[2].Trim().Trim('"').Trim("'")
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

function Get-ApplicationJar {
    param([Parameter(Mandatory)][string]$ServiceName)

    $target = Join-Path $backendRoot "$ServiceName\target"
    $jar = Get-ChildItem -LiteralPath $target -Filter "$ServiceName-*.jar" -File |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "No executable JAR for $ServiceName. Run: mvn clean package -DskipTests"
    }
    return $jar.FullName
}

function Assert-PortAvailable {
    param([Parameter(Mandatory)][int]$Port)

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($listener) {
        throw "Port $Port is already used by PID $($listener.OwningProcess). Stop it before starting."
    }
}

function Start-ServiceWindow {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$JarPath
    )

    $command = "`$Host.UI.RawUI.WindowTitle = 'Smart Education - $Name'; Set-Location -LiteralPath '$backendRoot'; & java -jar '$JarPath'"
    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $command
    ) | Out-Null
}

Import-DotEnv -Path $envFile

if ([Text.Encoding]::UTF8.GetByteCount([string]$env:JWT_SECRET) -lt 32) {
    throw 'JWT_SECRET must contain at least 32 UTF-8 bytes. Check backend/.env.'
}

Get-Command java -ErrorAction Stop | Out-Null

$services = @(
    @{ Name = 'Biz';     Module = 'edu-biz-service'; Port = 18081 },
    @{ Name = 'AI';      Module = 'edu-ai-service';  Port = 18082 },
    @{ Name = 'Gateway'; Module = 'edu-gateway';     Port = 18080 }
)

foreach ($service in $services) { Assert-PortAvailable -Port $service.Port }

foreach ($service in $services) {
    $jar = Get-ApplicationJar -ServiceName $service.Module
    Start-ServiceWindow -Name $service.Name -JarPath $jar
    Write-Host "Started $($service.Name) on port $($service.Port)" -ForegroundColor Green
    Start-Sleep -Seconds 1
}

Write-Host 'Biz, AI, and Gateway were started in separate PowerShell windows.' -ForegroundColor Cyan
