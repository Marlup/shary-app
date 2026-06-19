param(
    [string]$ManifestPath = (Join-Path $PSScriptRoot "session-init.manifest.json"),
    [ValidateSet("text", "json")]
    [string]$OutputMode = "text",
    [switch]$EmitPrompt
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ManifestPath)) {
    throw "Manifest not found: $ManifestPath"
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$requiredByPath = @{}
foreach ($required in $manifest.required_files) {
    $requiredByPath[$required.path] = $required
}

foreach ($path in $manifest.load_order) {
    if (-not $requiredByPath.ContainsKey($path)) {
        throw "Load-order path is not declared in required_files: $path"
    }
}

$results = @()
foreach ($path in $manifest.load_order) {
    $file = $requiredByPath[$path]
    $fullPath = Join-Path $repoRoot $file.path

    if (-not (Test-Path -LiteralPath $fullPath)) {
        throw "Missing required session-init file: $($file.path)"
    }

    if ([string]::IsNullOrWhiteSpace($file.sha256)) {
        throw "Missing checksum in manifest for: $($file.path). Run tools/codex/update-manifest.ps1"
    }

    $actualHash = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $file.sha256.ToLowerInvariant()) {
        throw "Checksum mismatch: $($file.path). Expected=$($file.sha256) Actual=$actualHash"
    }

    $results += [PSCustomObject]@{
        path = $file.path
        role = $file.role
        sha256 = $actualHash
        full_path = $fullPath
    }
}

if (-not $EmitPrompt) {
    Write-Host "Session-init checks passed. policy_version=$($manifest.policy_version)"
    return
}

if ($OutputMode -eq "json") {
    $payload = [PSCustomObject]@{
        policy_version = $manifest.policy_version
        generated_at_utc = [DateTime]::UtcNow.ToString("o")
        load_order = $manifest.load_order
        files = @()
    }

    foreach ($result in $results) {
        $payload.files += [PSCustomObject]@{
            path = $result.path
            role = $result.role
            content = (Get-Content -LiteralPath $result.full_path -Raw)
        }
    }

    $payload | ConvertTo-Json -Depth 8
    return
}

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("# Session Init Context")
[void]$sb.AppendLine("policy_version=$($manifest.policy_version)")
[void]$sb.AppendLine("manifest=$ManifestPath")
[void]$sb.AppendLine("")

foreach ($result in $results) {
    [void]$sb.AppendLine("## FILE: $($result.path)")
    [void]$sb.AppendLine((Get-Content -LiteralPath $result.full_path -Raw).TrimEnd())
    [void]$sb.AppendLine("")
}

$sb.ToString().TrimEnd()
