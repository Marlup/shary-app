param(
    [string]$DeviceSerial = "",
    [switch]$SkipBuild,
    [switch]$SkipInstall
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$gradle = Join-Path $repoRoot "gradlew.bat"

if (-not $SkipBuild) {
    # Compile the Android debug code before touching the installed application.
    & $gradle :app:compileDebugKotlin
    if ($LASTEXITCODE -ne 0) {
        throw "Debug Kotlin compilation failed with exit code $LASTEXITCODE"
    }

    # Run the focused in-process MCP regression suite from the guide.
    & $gradle :app:testDebugUnitTest --tests "com.shary.app.infrastructure.connector.mcp.SharyMcpServerTest"
    if ($LASTEXITCODE -ne 0) {
        throw "Focused MCP unit tests failed with exit code $LASTEXITCODE"
    }
}

if (-not $SkipInstall) {
    # Build and install the latest debug APK on the selected ADB target.
    $installArguments = @(":app:installDebug")
    $previousAndroidSerial = [Environment]::GetEnvironmentVariable("ANDROID_SERIAL")
    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $env:ANDROID_SERIAL = $DeviceSerial
    }
    try {
        & $gradle @installArguments
        if ($LASTEXITCODE -ne 0) {
            throw "Debug APK installation failed with exit code $LASTEXITCODE"
        }
    } finally {
        if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
            if ([string]::IsNullOrWhiteSpace($previousAndroidSerial)) {
                Remove-Item Env:ANDROID_SERIAL -ErrorAction SilentlyContinue
            } else {
                $env:ANDROID_SERIAL = $previousAndroidSerial
            }
        }
    }
}

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

# Launch Shary through Android's launcher category without Android Studio.
& $adbPath @adbTarget shell monkey -p com.shary.app -c android.intent.category.LAUNCHER 1
if ($LASTEXITCODE -ne 0) {
    throw "Shary launch failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "Preparation passed." -ForegroundColor Green
Write-Host "In Shary, open Settings > Assistant Connectors and press Start MCP."
Write-Host "Then pair the required connector(s) and run .\mcp-testing\protocol\run-mcp-tests.ps1."
