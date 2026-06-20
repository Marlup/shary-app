param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

$response = Invoke-SharyMcp `
    -Method "tools/list" `
    -Id "tools-1" `
    -WithoutAuth `
    -McpUrl $McpUrl

$expectedTools = @(
    "shary.fields.list",
    "shary.fields.create",
    "shary.fields.update",
    "shary.users.list",
    "shary.users.create",
    "shary.users.update",
    "shary.actions.preview",
    "shary.actions.commit"
)
$actualTools = @($response.result.tools | ForEach-Object { $_.name })
foreach ($expectedTool in $expectedTools) {
    Assert-SharyTrue `
        -Condition ($expectedTool -in $actualTools) `
        -Message "Tool discovery must include '$expectedTool'"
}
Assert-SharyEqual $actualTools.Count $expectedTools.Count "Advertised tool count"

Write-SharyTestPass "MCP tool discovery"
if ($PassThru) { $response }
