# Shary MCP legacy protocol PowerShell tests

## Purpose

This folder preserves the direct protocol and local ADB conformance scripts
for the older localhost MCP flow. It remains useful for development-only
transport checks and for the `LocalAdb` preflight path used by
`mcp-testing/real/start-real-agent-run.ps1`.

## Positioning

- Active supported product path:
  - remote OAuth MCP as documented in
    `docs/testing/connector-mcp-real-agent-e2e.md`.
- This folder:
  - legacy/development conformance tooling for localhost MCP protocol checks.

## Operator flow

1. Optionally prepare the Android debug build and launch Shary with
   `prepare-mcp-tests.ps1`.
2. Start MCP in the app and pair the needed connector mode.
3. Run `run-mcp-tests.ps1` for transport plus authenticated scenarios.
4. Run `cleanup-mcp-tests.ps1` when finished.

## Script contracts

### `prepare-mcp-tests.ps1`

- Purpose:
  - compile the debug app, run the focused MCP unit suite, optionally install
    the APK, and launch Shary on the target device.
- Inputs:
  - optional `-DeviceSerial`, `-SkipBuild`, `-SkipInstall`.
- Outputs:
  - compiled/tested app state and a launched debug build.
- Side effects:
  - runs Gradle tasks;
  - can install the debug APK;
  - launches the app over ADB.

### `run-mcp-tests.ps1`

- Purpose:
  - orchestrate the selected protocol scripts in the correct order.
- Inputs:
  - optional `-McpUrl`, `-DeviceSerial`, `-ReadOnlyToken`, `-ReadWriteToken`,
    `-ExpectedFieldKey`, `-ExpectedUserEmail`, `-SkipAdb`,
    `-IncludeMutations`.
- Outputs:
  - pass/fail process result for the requested protocol scenarios.
- Side effects:
  - may create test fields and users when `-IncludeMutations` is supplied.

### `cleanup-mcp-tests.ps1`

- Purpose:
  - remove ADB forwarding and clear MCP token environment variables.
- Inputs:
  - optional `-DeviceSerial`.
- Outputs:
  - cleaned local shell environment and removed TCP forward.

### `mcp-common.ps1`

- Purpose:
  - shared helper library for token resolution, JSON-RPC calls, tool calls,
    assertions, and test suffix generation.
- Inputs:
  - dot-sourced by the other protocol scripts.
- Outputs:
  - helper functions, not a standalone test result.

### `mcp-client.ps1`

- Purpose:
  - manual JSON-RPC caller for one MCP method.
- Inputs:
  - required `-Method`;
  - optional `-ParamsJson`, `-McpUrl`, `-Token`, `-WithoutAuth`.
- Outputs:
  - JSON response rendered to stdout.

### `mcp-prerequisites.ps1`

- Purpose:
  - verify ADB connectivity and forwarding, then probe MCP transport behavior.
- Inputs:
  - optional `-McpUrl`, `-DeviceSerial`, `-SkipAdb`, `-PassThru`.
- Outputs:
  - pass/fail result, optionally structured transport data.
- Expected transport assertion:
  - `GET` returns HTTP `405` and explains SSE is unsupported.

### `mcp-init.ps1`

- Purpose:
  - verify MCP `initialize` behavior and advertised server metadata.
- Inputs:
  - optional `-McpUrl`, `-PassThru`.
- Outputs:
  - pass/fail result, optionally raw initialize response.

### `mcp-ping-noAuth.ps1`

- Purpose:
  - verify unauthenticated `ping` behavior.
- Inputs:
  - optional `-McpUrl`, `-PassThru`.

### `mcp-discover-tools.ps1`

- Purpose:
  - verify `tools/list` and the expected Shary tool inventory.
- Inputs:
  - optional `-McpUrl`, `-PassThru`.

### `mcp-origin-protection.ps1`

- Purpose:
  - verify non-local `Origin` requests are rejected.
- Inputs:
  - optional `-McpUrl`, `-PassThru`.
- Expected assertion:
  - HTTP `403` with a forbidden-origin response.

### `mcp-list-fields.ps1`

- Purpose:
  - perform an authenticated field metadata list.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-ExpectedKey`, `-PassThru`.
- Expected assertion:
  - metadata list must not expose field values.

### `mcp-list-users.ps1`

- Purpose:
  - perform an authenticated user list.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-ExpectedEmail`, `-PassThru`.

### `mcp-readonly-denial.ps1`

- Purpose:
  - verify a read-only token cannot create fields.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-PassThru`.
- Expected error:
  - `unauthorized_scope`.

### `mcp-policy-errors.ps1`

- Purpose:
  - verify mutation-control failures such as missing idempotency, missing
    confirmation, and invalid token handling.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-PassThru`.

### `mcp-readwrite-crud.ps1`

- Purpose:
  - perform direct field/user create and update scenarios with a read-write
    token.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-TestSuffix`, `-PassThru`.
- Side effects:
  - creates and updates `.test` field and user records.

### `mcp-preview-commit.ps1`

- Purpose:
  - verify batch preview and commit semantics.
- Inputs:
  - optional `-Token`, `-McpUrl`, `-TestSuffix`, `-PassThru`.
- Side effects:
  - creates `.test` records through the preview/commit path.

## Token handling

- Keep bearer values only in the current shell process or pass them directly.
- Do not store them in repo files.

## Update triggers

Update this README and `docs/ai/SCRIPT_CATALOG.md` when:

- a script parameter changes;
- assertions or expected tool lists change;
- the folder's legacy/development positioning changes;
- a new protocol helper or scenario script is added.
