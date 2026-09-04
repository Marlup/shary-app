param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [string]$DeviceSerial = "",
    [switch]$SkipAdb,
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

if (-not $SkipAdb) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    $adbPath = if ($null -ne $adbCommand) {
        $adbCommand.Source
    } elseif (Test-Path -LiteralPath "C:\platform-tools\adb.exe") {
        "C:\platform-tools\adb.exe"
    } else {
        throw "adb was not found on PATH or at C:\platform-tools\adb.exe"
    }

    $devicesOutput = & $adbPath devices
    $deviceLines = @($devicesOutput | Where-Object { $_ -match "\sdevice$" })
    Assert-SharyTrue `
        -Condition ($deviceLines.Count -gt 0) `
        -Message "At least one authorized ADB target must have state 'device'"

    $adbTarget = @()
    if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
        $adbTarget = @("-s", $DeviceSerial)
    } elseif ($deviceLines.Count -gt 1) {
        throw "Several ADB targets are connected. Pass -DeviceSerial."
    }

    & $adbPath @adbTarget forward tcp:8765 tcp:8765 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "adb forward failed with exit code $LASTEXITCODE"
    }

    $forwardList = @(& $adbPath forward --list)
    Assert-SharyTrue `
        -Condition (($forwardList -join "`n") -match "tcp:8765\s+tcp:8765") `
        -Message "ADB forwarding must contain tcp:8765 -> tcp:8765"
}

$request = [System.Net.HttpWebRequest]::Create($McpUrl)
$request.Method = "GET"
$statusCode = 0
$responseBody = ""
try {
    $response = $request.GetResponse()
    $statusCode = [int]$response.StatusCode
} catch [System.Net.WebException] {
    if ($null -eq $_.Exception.Response) {
        throw
    }
    $response = $_.Exception.Response
    $statusCode = [int]$response.StatusCode
}

try {
    $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
    $responseBody = $reader.ReadToEnd()
    $reader.Dispose()
} finally {
    $response.Dispose()
}

Assert-SharyEqual `
    -Actual $statusCode `
    -Expected 405 `
    -Message "MCP GET transport probe must return HTTP 405"
Assert-SharyTrue `
    -Condition ($responseBody -match "SSE stream is not supported") `
    -Message "MCP GET response must explain that SSE is unsupported"

Write-SharyTestPass "ADB forwarding and MCP transport"
if ($PassThru) {
    [PSCustomObject]@{
        statusCode = $statusCode
        body = $responseBody
    }
}
