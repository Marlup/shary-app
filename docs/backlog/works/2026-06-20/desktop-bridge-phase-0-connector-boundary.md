# Desktop bridge Phase 0 connector boundary

## Scope

Implement the first production desktop-bridge prerequisite by separating raw
MCP dispatch from the Android loopback HTTP socket and enforcing Shary
lifecycle and request-rate controls at the shared dispatcher boundary.

## Change list

1. Extracted `SharyMcpDispatcher` from `SharyMcpServer`.
2. Retained `SharyMcpServer` as a compatibility facade for the existing
   localhost HTTP server.
3. Added tool-call denial while Shary credentials are locked or the user is
   signed out.
4. Added a fixed-window request limit per bearer credential.
5. Bounded tracked credential windows to avoid unbounded memory growth from
   arbitrary invalid bearer values.
6. Added focused locked-vault and independent-credential rate-limit tests.
7. Updated the desktop bridge architecture phase status.

## Viability and complexity

- Dispatcher extraction: low-to-moderate risk because MCP parsing, tool
  definitions, and connector routing remain unchanged.
- Lifecycle enforcement: moderate behavioral impact; initialization,
  discovery, and ping remain available, while only sensitive tool calls fail
  closed.
- Rate limiting: low operational complexity with deterministic in-memory
  windows and no credential logging or persistence.

## Design decisions

1. New transports depend directly on `SharyMcpDispatcher`.
2. Existing HTTP code continues through `SharyMcpServer` to avoid a broad
   socket-layer refactor.
3. Vault lock and signed-out states return MCP tool errors with
   `policy_denied`, preserving HTTP and JSON-RPC transport success semantics.
4. Rate limiting uses a SHA-256 bearer fingerprint as the in-memory key and
   returns `rate_limited` plus `retry_after_seconds`.
5. Public MCP methods remain available while locked for client diagnostics.

## Implementation plan

1. Complete Phase 0 in the Android app.
2. Start Phase 1 with the transport-only `:connector-link-protocol` module.
3. Add the `:desktop-bridge` skeleton only after shared envelopes and
   validation rules compile independently.

## Validation plan

1. Run the mandatory session-init policy gate.
2. Run `git diff --check`.
3. Compile debug and Android-test Kotlin.
4. Rerun the focused MCP server and HTTP transport unit tests.

## Result summary

Implementation is complete. The dispatcher is transport-neutral, sensitive
tool calls fail closed when Shary is unavailable, and bearer-scoped rate
limits are enforced with bounded state. Focused tests and final compilation
results are recorded in the delivery response.
