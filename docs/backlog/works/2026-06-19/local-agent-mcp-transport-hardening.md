# Local-agent MCP transport hardening

## Scope

Remove consumer AI-app and cloud-relay architecture from the active design,
focus Shary on MCP-capable agent tools, and implement the first local transport
hardening slice.

## Change list

1. Replaced the broad same-device AI design with
   `docs/architecture/SHARY_LOCAL_AGENT_MCP_DESIGN.md`.
2. Added a reusable `SharyMcpHttpTransport` between the socket listener and
   JSON-RPC adapter.
3. Added case-insensitive header lookup and local-origin enforcement.
4. Added content-type, Accept, and MCP protocol-version validation.
5. Added protocol and no-store response headers.
6. Added explicit stateless `GET` and `DELETE` responses.
7. Added request body and header limits.
8. Added focused JVM transport tests.

## Viability and complexity

- Architecture narrowing: low risk; it removes unsupported consumer-app and
  relay commitments.
- Transport extraction: moderate but contained; connector tool routing and
  policy remain unchanged.
- Full SSE support: deferred because current target agents work with
  request/response HTTP and no demonstrated client requires server streaming.

## Design decisions

1. Keep `127.0.0.1:8765/mcp` and bearer pairing.
2. Remain stateless at the HTTP layer.
3. Allow missing `Accept` and protocol-version headers for compatibility, but
   validate them when present.
4. Reject non-JSON POSTs and non-local browser origins before JSON-RPC
   dispatch.
5. Reuse the existing connector core for all authorization and audit behavior.

## Implementation plan

1. Extract and test transport policy.
2. Integrate it into the socket listener.
3. Preserve existing JSON-RPC and connector behavior.
4. Update architecture and operator documentation.

## Validation plan

1. Run `SharyMcpHttpTransportTest`.
2. Run `SharyMcpServerTest`.
3. Compile debug Kotlin.
4. Run `git diff --check`.

## Result summary

Implementation and static validation completed. On June 20, 2026,
`git diff --check` passed and the new transport code was reviewed for stale
protocol constants and paths. The debug Kotlin and Android-test Kotlin sources
compiled successfully, and the focused MCP transport, adapter, connector
management, preview/commit, refresh-session, and policy tests passed. Live ADB
protocol and real-agent validation remain an operator acceptance step because
no emulator or physical device was connected.
