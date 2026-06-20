param(
    [AllowEmptyString()]
    [string]$Token = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [string]$ExpectedEmail = "",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")
$Token = Resolve-SharyMcpToken $Token "SHARY_MCP_READ_ONLY_TOKEN"

$response = Invoke-SharyTool `
    -Name "shary.users.list" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "users-list-1"

Assert-SharyToolSuccess $response "List users"
$items = @($response.result.structuredContent.data.items)
if (-not [string]::IsNullOrWhiteSpace($ExpectedEmail)) {
    Assert-SharyTrue `
        -Condition ($null -ne ($items | Where-Object { $_.email -eq $ExpectedEmail })) `
        -Message "User list must contain '$ExpectedEmail'"
}

Write-SharyTestPass "Authenticated user list ($($items.Count) item(s))"
if ($PassThru) { $response }
