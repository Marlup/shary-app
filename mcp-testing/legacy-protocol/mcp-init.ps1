param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

$response = Invoke-SharyMcp `
    -Method "initialize" `
    -Id "initialize-1" `
    -WithoutAuth `
    -McpUrl $McpUrl `
    -Params @{
        protocolVersion = "2025-06-18"
        capabilities = @{}
        clientInfo = @{
            name = "shary-mcp-testing"
            version = "1.0"
        }
    }

Assert-SharyEqual $response.result.protocolVersion "2025-06-18" "Protocol version"
Assert-SharyEqual $response.result.serverInfo.name "shary-mcp" "Server name"
Assert-SharyEqual $response.result.capabilities.tools.listChanged $false "Tool list change capability"

Write-SharyTestPass "MCP initialize"
if ($PassThru) { $response }
