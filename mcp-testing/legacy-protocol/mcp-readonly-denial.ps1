param(
    [AllowEmptyString()]
    [string]$Token = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")
$Token = Resolve-SharyMcpToken $Token "SHARY_MCP_READ_ONLY_TOKEN"
$suffix = New-SharyTestSuffix

$response = Invoke-SharyTool `
    -Name "shary.fields.create" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "readonly-denial-$suffix" `
    -Arguments @{
        key = "e2e-denied-$suffix"
        value = "must-not-be-created"
        idempotency_key = "readonly-denial-$suffix"
        confirm = $true
    }

Assert-SharyToolError $response "unauthorized_scope" "Read-only write"
Write-SharyTestPass "Read-only mutation denial"
if ($PassThru) { $response }
