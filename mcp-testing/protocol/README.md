# Shary MCP protocol PowerShell tests

These scripts are executable versions of
`docs/testing/connector-mcp-e2e-quickstart.md`. Each script sends real requests
and throws when an expected result is missing. `run-mcp-tests.ps1` exits with
code `1` on the first failed assertion.

## Before running

1. Prepare and open the current Shary debug build:

   ```powershell
   # Compile, run the focused MCP tests, install Shary, and launch it.
   .\mcp-testing\protocol\prepare-mcp-tests.ps1 -DeviceSerial "emulator-5554"
   ```

2. In **Settings > Assistant Connectors**, start MCP.
3. Pair the connector mode needed by the tests.
4. Copy the bearer value. Do not save tokens in repository files.

Tokens may include or omit the `Bearer ` prefix.

## Public and transport tests

```powershell
.\mcp-testing\protocol\run-mcp-tests.ps1
```

This configures ADB forwarding and tests transport, initialize, ping, tool
discovery, and origin protection.

If forwarding is already configured:

```powershell
.\mcp-testing\protocol\run-mcp-tests.ps1 -SkipAdb
```

For multiple connected Android targets:

```powershell
.\mcp-testing\protocol\run-mcp-tests.ps1 -DeviceSerial "emulator-5554"
```

## Read-only tests

Keep the token only in the current PowerShell process:

```powershell
# Read the token interactively so its value is not part of the command history.
$env:SHARY_MCP_READ_ONLY_TOKEN = Read-Host "Paste read-only bearer"

# Run public tests plus authenticated read-only assertions.
.\mcp-testing\protocol\run-mcp-tests.ps1
```

This adds authenticated field/user reads and verifies that a write is denied
with `unauthorized_scope`.

## Read-write policy tests

```powershell
# Read the token interactively so its value is not part of the command history.
$env:SHARY_MCP_READ_WRITE_TOKEN = Read-Host "Paste read-write bearer"

# Run public tests plus non-mutating read-write policy assertions.
.\mcp-testing\protocol\run-mcp-tests.ps1
```

Without `-IncludeMutations`, the runner checks only requests that must fail:
missing idempotency, missing confirmation, and invalid token.

## Full mutating run

```powershell
# Keep both secrets in this PowerShell process only.
$env:SHARY_MCP_READ_ONLY_TOKEN = Read-Host "Paste read-only bearer"
$env:SHARY_MCP_READ_WRITE_TOKEN = Read-Host "Paste read-write bearer"

# Opt in to field/user creation and preview/commit tests.
.\mcp-testing\protocol\run-mcp-tests.ps1 -IncludeMutations
```

The mutating run creates uniquely named `.test` fields and users. The current
MCP tool set has no delete operations, so remove those records from Shary after
the run if they are no longer needed.

Run cleanup afterward:

```powershell
# Remove ADB forwarding and clear token variables from this PowerShell process.
.\mcp-testing\protocol\cleanup-mcp-tests.ps1 -DeviceSerial "emulator-5554"
```

## Run an individual script

```powershell
.\mcp-testing\protocol\mcp-init.ps1
.\mcp-testing\protocol\mcp-list-fields.ps1 -Token "Bearer PASTE_TOKEN_HERE"
.\mcp-testing\protocol\mcp-client.ps1 -Method "tools/list" -WithoutAuth
```
