param(
    [AllowEmptyString()]
    [string]$Token = "",
    [string]$McpUrl = "http://127.0.0.1:8765/mcp",
    [string]$TestSuffix = "",
    [switch]$PassThru
)

. (Join-Path $PSScriptRoot "mcp-common.ps1")
$Token = Resolve-SharyMcpToken $Token "SHARY_MCP_READ_WRITE_TOKEN"
if ([string]::IsNullOrWhiteSpace($TestSuffix)) {
    $TestSuffix = New-SharyTestSuffix
}

$fieldKey = "e2e-batch-field-$TestSuffix"
$userEmail = "e2e-batch-$TestSuffix@example.test"
$previewId = "batch-preview-$TestSuffix"
$commitId = "batch-commit-$TestSuffix"

$preview = Invoke-SharyTool `
    -Name "shary.actions.preview" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id $previewId `
    -Arguments @{
        idempotency_key = $previewId
        actions = @(
            @{
                type = "CREATE_FIELD"
                key = $fieldKey
                value = "created-by-preview-commit"
                key_alias = "E2E Batch Field"
            },
            @{
                type = "CREATE_USER"
                username = "E2E Batch User"
                email = $userEmail
            }
        )
    }
Assert-SharyToolSuccess $preview "Preview batch"
$pendingId = $preview.result.structuredContent.data.pending_id
Assert-SharyTrue (-not [string]::IsNullOrWhiteSpace($pendingId)) "Preview must return pending_id"

$commitArguments = @{
    pending_id = $pendingId
    idempotency_key = $commitId
    confirm = $true
}
$commit = Invoke-SharyTool `
    -Name "shary.actions.commit" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id $commitId `
    -Arguments $commitArguments
Assert-SharyToolSuccess $commit "Commit batch"
Assert-SharyEqual $commit.result.structuredContent.data.committed $true "Batch committed flag"
Assert-SharyEqual $commit.result.structuredContent.data.applied_count 2 "Batch applied count"
Assert-SharyEqual $commit.result.structuredContent.data.failed_count 0 "Batch failed count"

$commitAgain = Invoke-SharyTool `
    -Name "shary.actions.commit" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id $commitId `
    -Arguments $commitArguments
Assert-SharyToolSuccess $commitAgain "Repeat batch commit"
Assert-SharyEqual `
    $commitAgain.result.structuredContent.data.pending_id `
    $commit.result.structuredContent.data.pending_id `
    "Repeated commit pending ID"
Assert-SharyEqual `
    $commitAgain.result.structuredContent.data.applied_count `
    $commit.result.structuredContent.data.applied_count `
    "Repeated commit applied count"

Write-SharyTestPass "Preview, commit, and commit idempotency"
if ($PassThru) {
    [PSCustomObject]@{
        pendingId = $pendingId
        fieldKey = $fieldKey
        userEmail = $userEmail
        suffix = $TestSuffix
    }
}
