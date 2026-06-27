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

$fieldKey = "e2e-mcp-field-$TestSuffix"
$userEmail = "e2e-mcp-$TestSuffix@example.test"

$createField = Invoke-SharyTool `
    -Name "shary.fields.create" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "field-create-$TestSuffix" `
    -Arguments @{
        key = $fieldKey
        value = "created-through-mcp"
        key_alias = "E2E MCP Field"
        idempotency_key = "field-create-$TestSuffix"
        confirm = $true
    }
Assert-SharyToolSuccess $createField "Create field"
Assert-SharyEqual $createField.result.structuredContent.data.created $true "Field created flag"
Assert-SharyEqual $createField.result.structuredContent.data.key $fieldKey "Created field key"

$updateField = Invoke-SharyTool `
    -Name "shary.fields.update" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "field-update-$TestSuffix" `
    -Arguments @{
        key = $fieldKey
        value = "updated-through-mcp"
        key_alias = "Updated E2E MCP Field"
        idempotency_key = "field-update-$TestSuffix"
        confirm = $true
    }
Assert-SharyToolSuccess $updateField "Update field"
Assert-SharyEqual $updateField.result.structuredContent.data.updated $true "Field updated flag"

$field = Invoke-SharyTool `
    -Name "shary.fields.get" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "field-after-update-$TestSuffix" `
    -Arguments @{ key = $fieldKey }
Assert-SharyToolSuccess $field "Get field after update"
$storedField = $field.result.structuredContent.data
Assert-SharyEqual $storedField.key $fieldKey "Returned field key"
Assert-SharyEqual $storedField.value "updated-through-mcp" "Updated field value"
Assert-SharyEqual $storedField.key_alias "Updated E2E MCP Field" "Updated field alias"

$createUser = Invoke-SharyTool `
    -Name "shary.users.create" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "user-create-$TestSuffix" `
    -Arguments @{
        username = "E2E MCP User"
        email = $userEmail
        idempotency_key = "user-create-$TestSuffix"
        confirm = $true
    }
Assert-SharyToolSuccess $createUser "Create user"
Assert-SharyEqual $createUser.result.structuredContent.data.created $true "User created flag"

$updateUser = Invoke-SharyTool `
    -Name "shary.users.update" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "user-update-$TestSuffix" `
    -Arguments @{
        email = $userEmail
        username = "Updated E2E MCP User"
        idempotency_key = "user-update-$TestSuffix"
        confirm = $true
    }
Assert-SharyToolSuccess $updateUser "Update user"
Assert-SharyEqual $updateUser.result.structuredContent.data.updated $true "User updated flag"

$users = Invoke-SharyTool `
    -Name "shary.users.list" `
    -Token $Token `
    -McpUrl $McpUrl `
    -Id "users-after-update-$TestSuffix"
Assert-SharyToolSuccess $users "List users after update"
$storedUser = @($users.result.structuredContent.data.items) |
    Where-Object { $_.email -eq $userEmail } |
    Select-Object -First 1
Assert-SharyTrue ($null -ne $storedUser) "Created user must be returned by list"
Assert-SharyEqual $storedUser.username "Updated E2E MCP User" "Updated username"

Write-SharyTestPass "Read-write field and user CRUD"
if ($PassThru) {
    [PSCustomObject]@{
        fieldKey = $fieldKey
        userEmail = $userEmail
        suffix = $TestSuffix
    }
}
