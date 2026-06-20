param(
    [AllowEmptyString()]
    [string]$Token = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [string]$ExpectedKey = "",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")
$Token = Resolve-SharyMcpToken $Token "SHARY_MCP_READ_ONLY_TOKEN"

$response = Invoke-SharyTool `
    -Name "shary.fields.list" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "fields-list-1"

Assert-SharyToolSuccess $response "List fields"
$items = @($response.result.structuredContent.data.items)
if (-not [string]::IsNullOrWhiteSpace($ExpectedKey)) {
    Assert-SharyTrue `
        -Condition ($null -ne ($items | Where-Object { $_.key -eq $ExpectedKey })) `
        -Message "Field list must contain '$ExpectedKey'"
}

Write-SharyTestPass "Authenticated field list ($($items.Count) item(s))"
if ($PassThru) { $response }
