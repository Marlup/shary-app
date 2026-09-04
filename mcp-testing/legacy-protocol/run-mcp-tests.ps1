param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [string]$DeviceSerial = "",
    [AllowEmptyString()]
    [string]$ReadOnlyToken = "",
    [AllowEmptyString()]
    [string]$ReadWriteToken = "",
    [string]$ExpectedFieldKey = "",
    [string]$ExpectedUserEmail = "",
    [switch]$SkipAdb,
    [switch]$IncludeMutations
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-TestScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName,
        [hashtable]$Arguments = @{}
    )

    $scriptPath = Join-Path $PSScriptRoot $ScriptName
    Write-Host "`n=== $ScriptName ===" -ForegroundColor Cyan
    & $scriptPath @Arguments
}

$commonArguments = @{ McpUrl = $McpUrl }

try {
    Invoke-TestScript "mcp-prerequisites.ps1" @{
        McpUrl = $McpUrl
        DeviceSerial = $DeviceSerial
        SkipAdb = $SkipAdb
    }
    Invoke-TestScript "mcp-init.ps1" $commonArguments
    Invoke-TestScript "mcp-ping-noAuth.ps1" $commonArguments
    Invoke-TestScript "mcp-discover-tools.ps1" $commonArguments
    Invoke-TestScript "mcp-origin-protection.ps1" $commonArguments

    if ([string]::IsNullOrWhiteSpace($ReadOnlyToken)) {
        $ReadOnlyToken = [Environment]::GetEnvironmentVariable("SHARY_MCP_READ_ONLY_TOKEN")
    }
    if (-not [string]::IsNullOrWhiteSpace($ReadOnlyToken)) {
        Invoke-TestScript "mcp-list-fields.ps1" @{
            McpUrl = $McpUrl
            Token = $ReadOnlyToken
            ExpectedKey = $ExpectedFieldKey
        }
        Invoke-TestScript "mcp-list-users.ps1" @{
            McpUrl = $McpUrl
            Token = $ReadOnlyToken
            ExpectedEmail = $ExpectedUserEmail
        }
        Invoke-TestScript "mcp-readonly-denial.ps1" @{
            McpUrl = $McpUrl
            Token = $ReadOnlyToken
        }
    } else {
        Write-Host "[SKIP] Read-only tests: no token supplied." -ForegroundColor Yellow
    }

    if ([string]::IsNullOrWhiteSpace($ReadWriteToken)) {
        $ReadWriteToken = [Environment]::GetEnvironmentVariable("SHARY_MCP_READ_WRITE_TOKEN")
    }
    if (-not [string]::IsNullOrWhiteSpace($ReadWriteToken)) {
        Invoke-TestScript "mcp-policy-errors.ps1" @{
            McpUrl = $McpUrl
            Token = $ReadWriteToken
        }

        if ($IncludeMutations) {
            $suffix = "{0}-{1}" -f [DateTime]::UtcNow.ToString("yyyyMMddHHmmss"), ([Guid]::NewGuid().ToString("N").Substring(0, 8))
            Invoke-TestScript "mcp-readwrite-crud.ps1" @{
                McpUrl = $McpUrl
                Token = $ReadWriteToken
                TestSuffix = $suffix
            }
            Invoke-TestScript "mcp-preview-commit.ps1" @{
                McpUrl = $McpUrl
                Token = $ReadWriteToken
                TestSuffix = $suffix
            }
        } else {
            Write-Host "[SKIP] Mutating tests: pass -IncludeMutations to create test records." -ForegroundColor Yellow
        }
    } else {
        Write-Host "[SKIP] Read-write tests: no token supplied." -ForegroundColor Yellow
    }

    Write-Host "`nALL REQUESTED MCP TESTS PASSED" -ForegroundColor Green
} catch {
    Write-Host "`nMCP TEST RUN FAILED" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
