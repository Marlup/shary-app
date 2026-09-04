Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-SharyMcpToken {
    param(
        [AllowEmptyString()]
        [string]$Token,

        [Parameter(Mandatory = $true)]
        [string]$EnvironmentVariableName
    )

    $resolved = $Token
    if ([string]::IsNullOrWhiteSpace($resolved)) {
        $resolved = [Environment]::GetEnvironmentVariable($EnvironmentVariableName)
    }
    if ([string]::IsNullOrWhiteSpace($resolved)) {
        throw "Missing token. Pass -Token or set $EnvironmentVariableName."
    }

    return ($resolved -replace '^\s*Bearer\s+', '').Trim()
}

function Invoke-SharyMcp {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,

        [hashtable]$Params = @{},

        [object]$Id = 1,

        [string]$McpUrl = "http://127.0.0.1:8765/mcp",

        [AllowEmptyString()]
        [string]$Token = "",

        [switch]$WithoutAuth
    )

    $payload = [ordered]@{
        jsonrpc = "2.0"
        id = $Id
        method = $Method
    }
    if ($Params.Count -gt 0) {
        $payload["params"] = $Params
    }

    $headers = @{
        "Content-Type" = "application/json"
    }
    if (-not $WithoutAuth) {
        if ([string]::IsNullOrWhiteSpace($Token)) {
            throw "A bearer token is required for method '$Method'."
        }
        $headers["Authorization"] = "Bearer $Token"
    }

    return Invoke-RestMethod `
        -Uri $McpUrl `
        -Method Post `
        -Headers $headers `
        -Body ($payload | ConvertTo-Json -Depth 16)
}

function Invoke-SharyTool {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [hashtable]$Arguments = @{},

        [Parameter(Mandatory = $true)]
        [string]$Token,

        [string]$McpUrl = "http://127.0.0.1:8765/mcp",

        [object]$Id = 1
    )

    return Invoke-SharyMcp `
        -Method "tools/call" `
        -Id $Id `
        -McpUrl $McpUrl `
        -Token $Token `
        -Params @{
            name = $Name
            arguments = $Arguments
        }
}

function Assert-SharyTrue {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Assert-SharyEqual {
    param(
        [AllowNull()]
        [object]$Actual,

        [AllowNull()]
        [object]$Expected,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "ASSERTION FAILED: $Message. Expected='$Expected' Actual='$Actual'"
    }
}

function Assert-SharyToolSuccess {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Response,

        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    Assert-SharyTrue `
        -Condition ($null -ne $Response.result) `
        -Message "$Operation must return a JSON-RPC result"
    Assert-SharyEqual `
        -Actual $Response.result.isError `
        -Expected $false `
        -Message "$Operation must succeed"
}

function Assert-SharyToolError {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Response,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedCode,

        [Parameter(Mandatory = $true)]
        [string]$Operation
    )

    Assert-SharyTrue `
        -Condition ($null -ne $Response.result) `
        -Message "$Operation must return a JSON-RPC result"
    Assert-SharyEqual `
        -Actual $Response.result.isError `
        -Expected $true `
        -Message "$Operation must return a tool error"
    Assert-SharyEqual `
        -Actual $Response.result.structuredContent.error.code `
        -Expected $ExpectedCode `
        -Message "$Operation must return error code '$ExpectedCode'"
}

function Write-SharyTestPass {
    param([Parameter(Mandatory = $true)][string]$Name)
    Write-Host "[PASS] $Name" -ForegroundColor Green
}

function New-SharyTestSuffix {
    return "{0}-{1}" -f [DateTime]::UtcNow.ToString("yyyyMMddHHmmss"), ([Guid]::NewGuid().ToString("N").Substring(0, 8))
}
