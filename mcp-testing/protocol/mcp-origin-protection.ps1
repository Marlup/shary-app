param(
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")

$request = [System.Net.HttpWebRequest]::Create($McpUrl)
$request.Method = "POST"
$request.ContentType = "application/json"
$request.Headers.Add("Origin", "https://example.com")
$body = '{"jsonrpc":"2.0","id":"origin-1","method":"ping"}'
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$request.ContentLength = $bodyBytes.Length
$requestStream = $request.GetRequestStream()
$requestStream.Write($bodyBytes, 0, $bodyBytes.Length)
$requestStream.Dispose()

$statusCode = 0
$responseBody = ""
try {
    $response = $request.GetResponse()
    $statusCode = [int]$response.StatusCode
} catch [System.Net.WebException] {
    if ($null -eq $_.Exception.Response) {
        throw
    }
    $response = $_.Exception.Response
    $statusCode = [int]$response.StatusCode
}

try {
    $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
    $responseBody = $reader.ReadToEnd()
    $reader.Dispose()
} finally {
    $response.Dispose()
}

Assert-SharyEqual $statusCode 403 "Untrusted origin HTTP status"
Assert-SharyTrue ($responseBody -match "Forbidden origin") "Untrusted origin response body"

Write-SharyTestPass "MCP origin protection"
if ($PassThru) {
    [PSCustomObject]@{
        statusCode = $statusCode
        body = $responseBody
    }
}
