param(
    [string]$Path = (Join-Path $PSScriptRoot '..\.env')
)

$resolved = Resolve-Path -LiteralPath $Path -ErrorAction Stop
foreach ($line in Get-Content -LiteralPath $resolved) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#')) {
        continue
    }
    $separator = $trimmed.IndexOf('=')
    if ($separator -lt 1) {
        throw "Invalid .env line: $line"
    }
    $name = $trimmed.Substring(0, $separator).Trim()
    $value = $trimmed.Substring($separator + 1).Trim()
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

Write-Host "Loaded local environment variables from $resolved"

