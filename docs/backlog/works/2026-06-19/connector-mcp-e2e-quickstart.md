# Connector and MCP E2E quick-start guide

## Scope

Create a detailed, executable quick guide for validating the Shary connector
and MCP server end to end from a Windows desktop through an Android
emulator/device.

## Change list

1. Added `docs/testing/connector-mcp-e2e-quickstart.md`.
2. Documented Android build/install, app setup, MCP lifecycle, pairing, and
   credential handling.
3. Added the required `adb forward tcp:8765 tcp:8765` desktop-to-Android bridge.
4. Added reusable PowerShell JSON-RPC commands.
5. Covered read-only denial, read-write field/user operations, preview/commit,
   idempotency, origin protection, audit checks, cleanup, and troubleshooting.
6. Linked the guide from `docs/README.md`.
7. Added physical-device ADB authorization and `unauthorized` recovery steps.
8. Added purpose and scenario-specific comments to every executable command.
9. Clarified that pairing buttons are below the MCP controls and added stale-APK
   recovery guidance.
10. Made the Settings content scrollable so connector pairing controls remain
    reachable on smaller device screens.
11. Added active connector management with per-connector revoke and revoke-all
    actions while preserving audit history.
12. Added a configurable maximum of five active connectors and blocked pairing
    while MCP is stopped or the limit is reached.
13. Added a **Copy bearer** action that follows the configured clipboard
    auto-clear duration.
14. Added focused tests for connector limits, session revocation, replacement
    pairing, and revoke-all behavior.

## Viability and complexity

- Viability: High. The guide uses the implemented Settings UI, localhost MCP
  transport, connector capability model, and current tool schemas.
- Complexity: Low. This is documentation-only and does not change runtime
  behavior.

## Design decisions

- Treat manual JSON-RPC over PowerShell as the reference E2E path because it
  exposes exact request and response contracts independently of a specific MCP
  client.
- Keep desktop-client configuration generic because configuration file formats
  vary between MCP clients.
- Explicitly require checking `result.isError`; MCP tool failures can be
  transported in successful HTTP/JSON-RPC responses.
- Use only non-sensitive `.test` records in examples.
- Put explanations directly beside commands so readers can understand both the
  underlying tool behavior and why each invocation exists in this E2E flow.
- Keep MCP listener state separate from credential lifecycle: stopping the
  listener pauses access, while explicit revoke invalidates credentials.
- Count only non-revoked connectors and retain revoked records for auditability.

## Validation plan

1. Cross-check commands against `SharyMcpServer`, `SharyMcpHttpServer`,
   `ConnectorSettingsViewModel`, and `LoopbackConnectorRouter`.
2. Compile the debug Kotlin sources after making Settings scrollable.
3. Run focused connector management tests.
4. Verify repository links and Markdown formatting.
5. Run `git diff --check`.

## Result summary

The detailed guide is available at:

`docs/testing/connector-mcp-e2e-quickstart.md`

The commands and expected results were cross-checked against the current MCP
server, HTTP transport, pairing use case, connector router, and focused MCP
tests. The guide also explains the Android RSA authorization prompt and how to
reset a device that remains `unauthorized`. Commands now include general and
scenario-specific explanations. The Settings screen scrolls so pairing controls
below the MCP server row remain accessible. Connector credentials are now
manageable, capped at five active entries, revocable individually or together,
and the latest bearer value can be copied from Settings.
