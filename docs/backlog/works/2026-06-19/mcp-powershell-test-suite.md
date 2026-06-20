# MCP PowerShell test suite

## Scope

Convert the manual connector/MCP E2E guide into executable PowerShell scripts
that can be run independently or through one assertion-based orchestrator.

## Change list

1. Replaced the initial ad hoc MCP scripts with standalone parameterized tests.
2. Added shared JSON-RPC request, token normalization, and assertion helpers.
3. Added preparation and cleanup scripts for build/install/launch, ADB
   forwarding, and process-scoped token cleanup.
4. Added public tests for transport, initialize, ping, discovery, and origin
   protection.
5. Added authenticated read, read-only denial, mutation-policy, CRUD, and
   preview/commit/idempotency tests.
6. Added `run-mcp-tests.ps1`, which exits non-zero on the first failed
   assertion and skips phases whose token is not supplied.
7. Removed a bearer token that had been hardcoded in the original
   `mcp-client.ps1`.
8. Added `mcp-testing/protocol/README.md` and linked the automated path from the E2E
   guide.

## Design decisions

- Pairing remains manual because it is an explicit in-app authorization action.
- Tokens are accepted through parameters or process-scoped environment
  variables and are never committed to scripts.
- Mutating tests require `-IncludeMutations` because the current MCP tool set
  cannot delete the generated test records.
- Every generated field and user uses a unique timestamp/GUID suffix.
- The runner supports public-only, read-only, read-write policy, and full
  mutation modes from the same entry point.

## Validation

- Parsed every generated `.ps1` with PowerShell's language parser.
- Scanned `mcp-testing/` for token-shaped hardcoded secrets.
- Ran `git diff --check`.
- Ran the public suite against `emulator-5554`; ADB forwarding, transport,
  initialize, ping, discovery, and origin protection passed.

## Main entry point

```powershell
.\mcp-testing\protocol\run-mcp-tests.ps1 -DeviceSerial "emulator-5554"
```
