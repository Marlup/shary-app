param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

$response = Invoke-SharyMcp `
    -Method "ping" `
    -Id "ping-1" `
    -WithoutAuth `
    -McpUrl $McpUrl

Assert-SharyTrue ($null -ne $response.result) "Ping must return a JSON-RPC result"
Assert-SharyTrue `
    ($null -eq $response.PSObject.Properties["error"]) `
    "Ping must not return a JSON-RPC error"

Write-SharyTestPass "MCP ping"
if ($PassThru) { $response }
