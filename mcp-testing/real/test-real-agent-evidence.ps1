param(
    [Parameter(Mandatory = $true)]
    [string]$RunFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$resolvedRunFile = (Resolve-Path -LiteralPath $RunFile).Path
$allowedResultsRoot = (Resolve-Path (Join-Path $PSScriptRoot "results")).Path
if (-not $resolvedRunFile.StartsWith($allowedResultsRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "RunFile must be inside $allowedResultsRoot."
}

$run = Get-Content -LiteralPath $resolvedRunFile -Raw | ConvertFrom-Json
$errors = New-Object System.Collections.Generic.List[string]

foreach ($scenario in $run.scenarios) {
    $status = [string]$scenario.result.status
    if ($status -notin @("Pass", "Fail", "Skip")) {
        $errors.Add("$($scenario.id): status is '$status', not Pass, Fail, or Skip.")
        continue
    }

    $actualTools = @($scenario.result.toolCalls)
    if ($status -eq "Pass") {
        foreach ($actualTool in $actualTools) {
            if (@($scenario.expectedTools) -notcontains $actualTool) {
                $errors.Add("$($scenario.id): passing evidence contains unexpected tool '$actualTool'.")
            }
        }
        foreach ($expectedTool in @($scenario.expectedTools)) {
            if ($actualTools -notcontains $expectedTool) {
                $errors.Add("$($scenario.id): passing evidence is missing expected tool '$expectedTool'.")
            }
        }
        foreach ($forbiddenTool in @($scenario.forbiddenTools)) {
            if ($actualTools -contains $forbiddenTool) {
                $errors.Add("$($scenario.id): passing evidence contains forbidden tool '$forbiddenTool'.")
            }
        }
        if ([string]::IsNullOrWhiteSpace([string]$scenario.result.sharyResult)) {
            $errors.Add("$($scenario.id): passing evidence must describe the Shary result.")
        }
        if ([string]::IsNullOrWhiteSpace([string]$scenario.result.auditResult)) {
            $errors.Add("$($scenario.id): passing evidence must describe the audit result.")
        }
    }

    if ($status -eq "Skip" -and [string]::IsNullOrWhiteSpace([string]$scenario.result.notes)) {
        $errors.Add("$($scenario.id): skipped evidence must state the unavailable prerequisite.")
    }
}

$run.completedAtUtc = [DateTime]::UtcNow.ToString("o")
$run | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $resolvedRunFile -Encoding UTF8

$reportPath = [System.IO.Path]::ChangeExtension($resolvedRunFile, ".md")
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Shary real-agent E2E result")
$lines.Add("")
$lines.Add("- Run ID: ``$($run.runId)``")
$lines.Add("- Mode: ``$($run.mode)``")
$lines.Add("- Started UTC: ``$($run.startedAtUtc)``")
$lines.Add("- Completed UTC: ``$($run.completedAtUtc)``")
$lines.Add("")
$lines.Add("| Scenario | Status | Tools | Shary result | Audit result |")
$lines.Add("|---|---|---|---|---|")
foreach ($scenario in $run.scenarios) {
    $tools = (@($scenario.result.toolCalls) -join ", ") -replace "\|", "\|"
    $sharyResult = ([string]$scenario.result.sharyResult) -replace "\|", "\|"
    $auditResult = ([string]$scenario.result.auditResult) -replace "\|", "\|"
    $lines.Add("| $($scenario.title) | $($scenario.result.status) | $tools | $sharyResult | $auditResult |")
}

$generatedRecords = @(
    $run.scenarios |
        ForEach-Object { @($_.result.generatedRecords) } |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) } |
        Select-Object -Unique
)
$lines.Add("")
$lines.Add("## Manual cleanup")
$lines.Add("")
if ($generatedRecords.Count -eq 0) {
    $lines.Add("No generated records were reported.")
} else {
    foreach ($record in $generatedRecords) {
        $lines.Add("- ``$record``")
    }
}

if ($errors.Count -gt 0) {
    $lines.Add("")
    $lines.Add("## Validation errors")
    $lines.Add("")
    foreach ($validationError in $errors) {
        $lines.Add("- $validationError")
    }
}

$lines | Set-Content -LiteralPath $reportPath -Encoding UTF8

if ($errors.Count -gt 0) {
    Write-Host "REAL-AGENT EVIDENCE INVALID" -ForegroundColor Red
    foreach ($validationError in $errors) {
        Write-Host "- $validationError" -ForegroundColor Red
    }
    Write-Host "Report: $reportPath"
    exit 1
}

$passed = @($run.scenarios | Where-Object { $_.result.status -eq "Pass" }).Count
$failed = @($run.scenarios | Where-Object { $_.result.status -eq "Fail" }).Count
$skipped = @($run.scenarios | Where-Object { $_.result.status -eq "Skip" }).Count

Write-Host "REAL-AGENT EVIDENCE VALID" -ForegroundColor Green
Write-Host "Pass=$passed Fail=$failed Skip=$skipped"
Write-Host "Evidence: $resolvedRunFile"
Write-Host "Report: $reportPath"

if ($failed -gt 0) {
    exit 1
}
