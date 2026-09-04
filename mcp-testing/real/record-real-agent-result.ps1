param(
    [Parameter(Mandatory = $true)]
    [string]$RunFile,
    [Parameter(Mandatory = $true)]
    [string]$ScenarioId,
    [Parameter(Mandatory = $true)]
    [ValidateSet("Pass", "Fail", "Skip")]
    [string]$Status,
    [string[]]$ToolCalls = @(),
    [string]$SharyResult = "",
    [string]$AuditResult = "",
    [string]$Notes = "",
    [string[]]$GeneratedRecords = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$resolvedRunFile = (Resolve-Path -LiteralPath $RunFile).Path
$allowedResultsRoot = (Resolve-Path (Join-Path $PSScriptRoot "results")).Path
if (-not $resolvedRunFile.StartsWith($allowedResultsRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "RunFile must be inside $allowedResultsRoot."
}

$run = Get-Content -LiteralPath $resolvedRunFile -Raw | ConvertFrom-Json
$scenario = @($run.scenarios | Where-Object { $_.id -eq $ScenarioId })
if ($scenario.Count -ne 1) {
    throw "Expected exactly one scenario with ID '$ScenarioId'; found $($scenario.Count)."
}

$scenario[0].result.status = $Status
$scenario[0].result.toolCalls = @($ToolCalls)
$scenario[0].result.sharyResult = $SharyResult
$scenario[0].result.auditResult = $AuditResult
$scenario[0].result.notes = $Notes
$scenario[0].result.generatedRecords = @($GeneratedRecords)

$run | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $resolvedRunFile -Encoding UTF8
Write-Host "[RECORDED] $ScenarioId = $Status" -ForegroundColor Green
