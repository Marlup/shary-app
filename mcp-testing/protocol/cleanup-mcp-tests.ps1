param(
    [string]$DeviceSerial = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($null -ne $adbCommand) {
    $adbCommand.Source
} elseif (Test-Path -LiteralPath "C:\platform-tools\adb.exe") {
    "C:\platform-tools\adb.exe"
} else {
    throw "adb was not found on PATH or at C:\platform-tools\adb.exe"
}
$adbTarget = @()
if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $adbTarget = @("-s", $DeviceSerial)
}

# Remove the Windows-to-Android MCP tunnel created by the preparation or runner.
# Check first so cleanup remains successful when the rule was already removed.
$forwardList = @(& $adbPath forward --list)
if (($forwardList -join "`n") -match "tcp:8765\s+tcp:8765") {
    & $adbPath @adbTarget forward --remove tcp:8765
    if ($LASTEXITCODE -ne 0) {
        throw "Removing ADB forwarding failed with exit code $LASTEXITCODE"
    }
}

# Remove bearer values from this PowerShell process.
Remove-Item Env:SHARY_MCP_READ_ONLY_TOKEN -ErrorAction SilentlyContinue
Remove-Item Env:SHARY_MCP_READ_WRITE_TOKEN -ErrorAction SilentlyContinue
Remove-Item Env:SHARY_MCP_TOKEN -ErrorAction SilentlyContinue

Write-Host "[PASS] ADB forwarding removed and MCP token variables cleared." -ForegroundColor Green
Write-Host "Stop MCP and revoke test connectors from Shary Settings if testing is complete."
