param(
    [AllowEmptyString()]
    [string]$Token = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")
$Token = Resolve-SharyMcpToken $Token "SHARY_MCP_READ_WRITE_TOKEN"
$suffix = New-SharyTestSuffix

$missingIdempotency = Invoke-SharyTool `
    -Name "shary.fields.create" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "missing-idempotency-$suffix" `
    -Arguments @{
        key = "e2e-invalid-$suffix"
        value = "must-not-be-created"
        confirm = $true
    }
Assert-SharyToolError $missingIdempotency "validation_failed" "Missing idempotency key"

$missingConfirmation = Invoke-SharyTool `
    -Name "shary.fields.create" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "missing-confirmation-$suffix" `
    -Arguments @{
        key = "e2e-unconfirmed-$suffix"
        value = "must-not-be-created"
        idempotency_key = "missing-confirmation-$suffix"
    }
Assert-SharyToolError $missingConfirmation "confirmation_required" "Missing confirmation"

$invalidToken = Invoke-SharyTool `
    -Name "shary.fields.list" `
    -Token "invalid-token" `
    -McpUrl $McpUrl `
    -Id "invalid-token-$suffix"
Assert-SharyToolError $invalidToken "session_expired" "Invalid token"

Write-SharyTestPass "Mutation controls and invalid-token handling"
if ($PassThru) {
    [PSCustomObject]@{
        missingIdempotency = $missingIdempotency
        missingConfirmation = $missingConfirmation
        invalidToken = $invalidToken
    }
}
