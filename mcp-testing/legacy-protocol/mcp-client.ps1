param(
    [Parameter(Mandatory = $true)]
    [string]$Method,

    [string]$ParamsJson = "{}",

    [string]$McpUrl = "http://127.0.0.1:8765/mcp",

    [AllowEmptyString()]
    [string]$Token = "",

    [switch]$WithoutAuth
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

$paramsObject = $ParamsJson | ConvertFrom-Json
$params = @{}
if ($null -ne $paramsObject) {
    foreach ($property in $paramsObject.PSObject.Properties) {
        $params[$property.Name] = $property.Value
    }
}

if (-not $WithoutAuth) {
    $Token = Resolve-SharyMcpToken `
        -Token $Token `
        -EnvironmentVariableName "SHARY_MCP_TOKEN"
}

Invoke-SharyMcp `
    -Method $Method `
    -Params $params `
    -Id "manual-$([Guid]::NewGuid().ToString('N'))" `
    -McpUrl $McpUrl `
    -Token $Token `
    -WithoutAuth:$WithoutAuth |
    ConvertTo-Json -Depth 16
