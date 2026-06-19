param(
    [string]$ManifestPath = (Join-Path $PSScriptRoot "session-init.manifest.json")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ManifestPath)) {
    throw "Manifest not found: $ManifestPath"
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

foreach ($file in $manifest.required_files) {
    $fullPath = Join-Path $repoRoot $file.path
    if (-not (Test-Path -LiteralPath $fullPath)) {
        throw "Cannot hash missing file: $($file.path)"
    }

    $hash = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $file.sha256 = $hash
}

$manifest.generated_at_utc = [DateTime]::UtcNow.ToString("o")

$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $ManifestPath -NoNewline
Write-Host "Updated manifest hashes: $ManifestPath"
