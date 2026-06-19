# MCP server for agent access

## Scope

Implement a Shary MCP server so Codex and other MCP-capable agents can access Shary through the existing connector security model.

## Change list

1. Added an MCP JSON-RPC adapter at `app/infrastructure/connector/mcp/`.
2. Added a localhost Streamable HTTP endpoint at `http://127.0.0.1:8765/mcp`.
3. Mapped MCP tools to existing Shary connector routes:
   - `shary.fields.list`
   - `shary.fields.create`
   - `shary.fields.update`
   - `shary.users.list`
   - `shary.users.create`
   - `shary.users.update`
   - `shary.actions.preview`
   - `shary.actions.commit`
4. Added Settings controls to start/stop the MCP server and show latest pairing credentials.
5. Added targeted unit coverage for tool discovery, authenticated field reads, and read-only write denial.

## Viability and complexity

- Viability: High. The previous connector work already provided policy-gated read/write operations and preview/commit orchestration.
- Complexity: Medium. The main risk was avoiding a second authorization path; the MCP implementation delegates to the existing loopback router instead.

## Design decisions

- MCP is an adapter, not a new security boundary.
- Tool calls use the existing `Authorization: Bearer <connector token>` header, capability checks, confirmation behavior, and denial audit path.
- The endpoint binds to `127.0.0.1` and rejects non-local `Origin` headers to reduce local-server exposure.
- The implementation supports request/response JSON-RPC over HTTP POST. It returns 405 for GET/SSE because server-to-client streaming is not required for the current tool surface.
- Mutating tools require `idempotency_key`; strict-policy writes still require `confirm=true`.

## Implementation plan

1. Implement MCP lifecycle and tool calls: `initialize`, `ping`, `tools/list`, and `tools/call`.
2. Translate each MCP tool into the current `LoopbackRequest` contract.
3. Add a minimal localhost HTTP transport for `/mcp`.
4. Surface start/stop and pairing token output in Settings.
5. Validate with compile and targeted unit tests.

## Validation plan

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.infrastructure.connector.mcp.SharyMcpServerTest"`

## Result summary

Implemented and verified. The MCP server is available from Settings after the user starts it and pairs a connector. Agents should connect to `http://127.0.0.1:8765/mcp` and send the shown bearer token in the `Authorization` header.

Revalidated on 2026-06-19:

- Repository session-init checksum gate passed with policy version `2026-05-31.1`.
- `./gradlew :app:compileDebugKotlin` completed with `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.infrastructure.connector.mcp.SharyMcpServerTest" --rerun-tasks` completed with `BUILD SUCCESSFUL`; all 43 Gradle tasks were executed.
- Remaining output is limited to existing Gradle/Kotlin deprecation and annotation-target warnings; no MCP compile or test failures remain.
