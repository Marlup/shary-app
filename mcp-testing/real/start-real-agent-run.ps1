param(
    [ValidateSet("ReadOnly", "ReadWrite")]
    [string]$Mode = "ReadOnly",
    [string]$DeviceSerial = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$SkipProtocolPreflight
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$realRoot = $PSScriptRoot
$testingRoot = (Resolve-Path (Join-Path $realRoot "..")).Path
$protocolRoot = Join-Path $testingRoot "protocol"
$scenarioPath = Join-Path $realRoot "scenarios.json"
$resultsRoot = Join-Path $realRoot "results"

if (-not $SkipProtocolPreflight) {
    $runner = Join-Path $protocolRoot "run-mcp-tests.ps1"
    $arguments = @{
        McpUrl = $McpUrl
    }
    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $arguments["DeviceSerial"] = $DeviceSerial
    }
    & $runner @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Protocol preflight failed with exit code $LASTEXITCODE."
    }
}

$scenarioDocument = Get-Content -LiteralPath $scenarioPath -Raw | ConvertFrom-Json
$selectedScenarios = @(
    $scenarioDocument.scenarios |
        Where-Object { $_.modes -contains $Mode } |
        ForEach-Object {
            [ordered]@{
                id = $_.id
                title = $_.title
                expectedTools = @($_.expectedTools)
                forbiddenTools = @($_.forbiddenTools)
                instruction = $_.instruction
                passCondition = $_.passCondition
                result = [ordered]@{
                    status = "Pending"
                    toolCalls = @()
                    sharyResult = ""
                    auditResult = ""
                    notes = ""
                    generatedRecords = @()
                }
            }
        }
)

$runId = "{0}-{1}" -f [DateTime]::UtcNow.ToString("yyyyMMddHHmmss"), ([Guid]::NewGuid().ToString("N").Substring(0, 8))
$run = [ordered]@{
    schemaVersion = 1
    runId = $runId
    mode = $Mode
    startedAtUtc = [DateTime]::UtcNow.ToString("o")
    completedAtUtc = $null
    mcpUrl = $McpUrl
    scenarios = $selectedScenarios
}

New-Item -ItemType Directory -Path $resultsRoot -Force | Out-Null
$runPath = Join-Path $resultsRoot "real-agent-$runId.json"
$run | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $runPath -Encoding UTF8

Write-Host ""
Write-Host "REAL-AGENT RUN INITIALIZED" -ForegroundColor Green
Write-Host "Mode: $Mode"
Write-Host "Run ID: $runId"
Write-Host "Evidence: $runPath"
Write-Host ""
Write-Host "Execute each pending scenario through the connected Shary MCP tools."
Write-Host "Record each result with record-real-agent-result.ps1, then run test-real-agent-evidence.ps1."

return $runPath
