param(
    [string]$ProjectId = "shary-21b61",
    [string]$Region = "europe-southwest1",
    [string]$SchedulerLocation = "europe-west1",
    [string]$SchedulerJob = "cleanup-expired-payloads"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$gcloudCmd = "C:\Program Files (x86)\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
if (-not (Test-Path -LiteralPath $gcloudCmd)) {
    $gcloud = Get-Command gcloud -ErrorAction SilentlyContinue
    if ($null -eq $gcloud) {
        throw "gcloud is not available on PATH."
    }
    $gcloudCmd = $gcloud.Source
}

$script:failures = [System.Collections.Generic.List[string]]::new()
$script:warnings = [System.Collections.Generic.List[string]]::new()

function Add-Pass([string]$Message) {
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Add-Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    $script:failures.Add($Message)
}

function Add-Warning([string]$Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
    $script:warnings.Add($Message)
}

function Invoke-Gcloud {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $gcloudCmd @Arguments 2>$null
        return [PSCustomObject]@{
            ExitCode = $LASTEXITCODE
            Text = (@($output) -join [Environment]::NewLine).Trim()
        }
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Convert-FromJson {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    return $Text | ConvertFrom-Json
}

Write-Host "Shary GCP readiness check (read-only)"
Write-Host "Project: $ProjectId"
Write-Host "Region: $Region"
Write-Host ""

$auth = Invoke-Gcloud @("auth", "list", "--format=value(account,status)")
if ($auth.ExitCode -ne 0 -or $auth.Text -notmatch "\*") {
    Add-Fail "No active gcloud account. Run: gcloud auth login"
} else {
    Add-Pass "gcloud account active: $($auth.Text -split "`n" | Select-Object -First 1)"
}

$dbResult = Invoke-Gcloud @(
    "firestore", "databases", "describe",
    "--project=$ProjectId",
    "--database=(default)",
    "--format=json"
)
$db = Convert-FromJson $dbResult.Text
if ($dbResult.ExitCode -ne 0 -or $null -eq $db) {
    Add-Fail "Unable to read Firestore database metadata."
} else {
    if ($db.locationId -eq $Region) {
        Add-Pass "Firestore location is $($db.locationId)."
    } else {
        Add-Fail "Firestore location is '$($db.locationId)', expected '$Region'."
    }

    if ($db.pointInTimeRecoveryEnablement -eq "POINT_IN_TIME_RECOVERY_ENABLED") {
        Add-Pass "PITR is enabled."
    } else {
        Add-Fail "PITR is '$($db.pointInTimeRecoveryEnablement)'."
    }

    if ($db.databaseEdition) {
        Add-Pass "Firestore edition: $($db.databaseEdition)."
    }
}

$schedulesResult = Invoke-Gcloud @(
    "firestore", "backups", "schedules", "list",
    "--project=$ProjectId",
    "--database=(default)",
    "--format=json"
)
$schedules = Convert-FromJson $schedulesResult.Text
if ($schedulesResult.ExitCode -ne 0) {
    Add-Fail "Unable to list Firestore backup schedules."
} elseif ($null -eq $schedules -or @($schedules).Count -eq 0) {
    Add-Fail "No Firestore backup schedule exists."
} else {
    Add-Pass "Firestore backup schedule count: $(@($schedules).Count)."
}

$backupsResult = Invoke-Gcloud @(
    "firestore", "backups", "list",
    "--project=$ProjectId",
    "--location=$Region",
    "--format=json"
)
$backups = Convert-FromJson $backupsResult.Text
if ($backupsResult.ExitCode -ne 0) {
    Add-Fail "Unable to list Firestore backups."
} elseif ($null -eq $backups -or @($backups).Count -eq 0) {
    Add-Warning "No completed Firestore backup is listed yet."
} else {
    Add-Pass "Firestore backups listed: $(@($backups).Count)."
}

$schedulerResult = Invoke-Gcloud @(
    "scheduler", "jobs", "describe", $SchedulerJob,
    "--project=$ProjectId",
    "--location=$SchedulerLocation",
    "--format=json"
)
$scheduler = Convert-FromJson $schedulerResult.Text
if ($schedulerResult.ExitCode -ne 0 -or $null -eq $scheduler) {
    Add-Fail "Unable to read scheduler job '$SchedulerJob'."
} else {
    if ($scheduler.state -eq "ENABLED") {
        Add-Pass "Scheduler '$SchedulerJob' is ENABLED."
    } else {
        Add-Fail "Scheduler '$SchedulerJob' is '$($scheduler.state)'."
    }

    $targetUri = [string]$scheduler.httpTarget.uri
    if ($targetUri -match "clean_expired_docs") {
        Add-Pass "Scheduler target points to clean_expired_docs."
    } else {
        Add-Fail "Scheduler target is unexpected: '$targetUri'."
    }

    if ($scheduler.schedule) {
        Add-Pass "Scheduler schedule: $($scheduler.schedule)."
    }
}

$ttlResult = Invoke-Gcloud @(
    "firestore", "fields", "ttls", "list",
    "--project=$ProjectId",
    "--format=json"
)
$ttls = Convert-FromJson $ttlResult.Text
$ttlNames = @()
if ($null -ne $ttls) {
    $ttlNames = @($ttls | ForEach-Object { [string]$_.name })
}

$expectedTtlCollections = @(
    "mcp_grant_invitations_v1",
    "mcp_oauth_codes_v1",
    "mcp_oauth_consumptions_v1",
    "mcp_oauth_refresh_tokens_v1",
    "mcp_oauth_requests_v1",
    "mcp_operations_v1",
    "mcp_rate_limits_v1"
)

if ($ttlResult.ExitCode -ne 0) {
    Add-Fail "Unable to list Firestore TTL policies."
} else {
    foreach ($collection in $expectedTtlCollections) {
        $match = $ttlNames | Where-Object {
            $_ -match "collectionGroups/$([regex]::Escape($collection))/fields/delete_after"
        }
        if ($match) {
            Add-Pass "TTL delete_after enabled for $collection."
        } else {
            Add-Fail "TTL delete_after missing for $collection."
        }
    }
}

Write-Host ""
if ($script:failures.Count -eq 0) {
    Write-Host "Readiness: PASS ($($script:warnings.Count) warning(s))" -ForegroundColor Green
    exit 0
}

Write-Host "Readiness: FAIL ($($script:failures.Count) failure(s), $($script:warnings.Count) warning(s))" -ForegroundColor Red
foreach ($failure in $script:failures) {
    Write-Host " - $failure" -ForegroundColor Red
}
exit 1
