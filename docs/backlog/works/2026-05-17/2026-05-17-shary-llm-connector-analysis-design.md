# Shary LLM/GenAI Connector - Analysis and Design (2026-05-17)

## Scope
Evaluate adding connector capabilities so LLMs/GenAI assistants/agents can:
1. Access a user-installed Shary app to read, add, and edit fields and users.
2. Optionally support an `agent2user` send flow for data-fields (design only, no implementation decision yet).

## Current Baseline (Relevant)
- Shary is offline-first and device-centric; cloud should not hard-gate local flows.
- Existing sharing model is primarily `user2user` with explicit user actions.
- Sensitive data operations already require cautious UX and reversible behaviors where possible.

## Goal Definition
A connector should let authorized assistants act on Shary data in a controlled, auditable way without weakening:
- local data confidentiality,
- explicit user intent,
- current security/auth posture,
- performance and app responsiveness.

## Architectural Options for Agent Access (Read/Add/Edit)

### Option A - Local Loopback API inside Android app (Recommended)
Expose a local-only API endpoint from the Shary app (loopback interface) with strict capabilities and per-action consent.

Pros:
- Keeps data local to device by default.
- Works with local agent runtimes and desktop bridge tools.
- Minimal dependency on backend availability.

Cons:
- Requires robust local auth/session and anti-abuse controls.
- Android lifecycle and background execution constraints need careful handling.

### Option B - Shary Cloud Broker API for Agents
Agents interact with backend APIs; app syncs resulting updates.

Pros:
- Centralized auth/governance.
- Easier enterprise policy enforcement.

Cons:
- Larger cloud attack surface.
- Weaker offline-first fit.
- Higher latency and more schema/version coupling.

### Option C - File-based Intent Queue (Import/Export contract)
Agents write signed intent files, app imports after review.

Pros:
- Very safe and transparent review surface.
- Easy rollback by rejecting intents.

Cons:
- Poor realtime UX.
- Less ergonomic for iterative assistant workflows.

## Recommended Direction
Adopt **Option A (Local Loopback API)** first, with strict capability and consent boundaries.
Use Option C patterns as fallback for high-risk operations (batch writes).
Keep Option B for later, if multi-device/cloud-assistant scale is required.

## Connector Security Model (P0)

1. Capability-scoped tokens:
- `fields.read`, `fields.write`, `users.read`, `users.write`, `share.prepare`.
- No wildcard "all access" capability.

2. Session bootstrapping:
- User opens "Connect assistant" screen.
- One-time pairing code or QR binds a local client.
- Short-lived access token + renewable session key stored in app secure storage.

3. Explicit consent classes:
- Read actions can be pre-approved per scope/time window.
- Write actions require either:
  - pre-approved policy for low-risk ops, or
  - per-operation confirmation for high-risk ops.

4. Policy engine in app:
- Deny by default.
- Enforce field-level restrictions (e.g., protected tags).
- Enforce rate limits and cooldown on failed auth.

5. Full audit trail:
- timestamp, connector client id, operation, target ids, result.
- User-visible "Assistant activity" ledger with revoke button.

## API Surface Draft (P0)

- `POST /connector/session/pair`
- `POST /connector/session/refresh`
- `GET /connector/fields`
- `POST /connector/fields`
- `PATCH /connector/fields/{fieldId}`
- `GET /connector/users`
- `POST /connector/users`
- `PATCH /connector/users/{userId}`
- `POST /connector/actions/preview` (dry run + impact summary)
- `POST /connector/actions/commit` (executes approved mutation set)

Design notes:
- All mutating operations accept idempotency keys.
- All responses include `request_id`, `schema_version`, normalized error envelope.
- Partial failures return structured per-item status.

## UX Contract Draft

- New settings page: `Assistant Connectors`.
- States: disconnected, paired-limited, paired-full, revoked, locked.
- Each capability has toggles + risk label.
- "Ask every time" mode available globally and per capability.
- Activity ledger supports search/filter + one-tap revoke.

## Data Model/Contract Additions

- `connector_clients`
- `connector_sessions`
- `connector_policies`
- `connector_audit_events`
- `connector_pending_actions` (for approval queue)

All connector records should include explicit schema versioning and migration guards.

## Optional Design - `agent2user` Send Flow (Future)

### Intent
Allow assistant-originated field delivery to users, while preserving current review-before-import posture.

### Proposed Model
- Assistant cannot directly deliver into recipient vault.
- Assistant creates `delivery_intent` signed by connector identity.
- Recipient sees inbox item with sender=assistant/client + owner user context.
- Recipient chooses: Accept / Reject / Block sender.

### Safety Controls
- Default rate limits per sender and per recipient.
- Trust tiers for connector senders (new/untrusted/verified).
- Mandatory content preview before accept.
- Optional quarantine for high-risk content classes.

### Open Product Decisions
- Whether assistant sender identity is user-bound only or can be org-bound.
- Whether `agent2user` should require recipient opt-in whitelist.
- Monetization/plan boundaries for automated sends.

## Viability and Complexity

- P0 (read + limited write, local connector): **Medium-High**, viable.
- P1 (advanced policy engine + action queue UX): **High**.
- Optional `agent2user`: **High** product/security complexity, postpone until P0 telemetry is stable.

## Proposed Delivery Plan

1. Design freeze:
- Finalize capability matrix, risk classes, and error contract.

2. Technical skeleton:
- Add connector domain models, policy evaluator, audit logging path, and dry-run executor.

3. UX shell:
- Build `Assistant Connectors` settings + activity ledger + revoke controls.

4. Read-only pilot:
- Enable `fields.read` and `users.read` first with audit.

5. Controlled write pilot:
- Enable `fields.write` behind explicit confirmation and dry-run preview.

6. Evaluate telemetry and abuse signals:
- failed auth, mutation denial rates, user revocations, latency.

7. Decide on `agent2user` go/no-go.

## Risks and Mitigations

- Risk: silent mass mutation.
  Mitigation: commit gate + idempotency keys + policy deny defaults.

- Risk: local endpoint abuse by other apps.
  Mitigation: pairing handshake, app-bound keys, short token TTL, strict origin/process checks where feasible.

- Risk: UX overload from prompts.
  Mitigation: policy presets (strict/balanced/automation) and per-capability defaults.

## Recommendation
Proceed with a **P0 local connector MVP** focused on read plus narrowly-scoped write, with mandatory auditability and explicit revoke controls. Keep `agent2user` in design backlog pending P0 adoption and security telemetry.

## MCP Approach Evaluation (Added 2026-05-17)

### Feasibility of MCP for Shary
MCP is feasible for Shary as a protocol/interface layer, but it should be treated as a **client-facing adapter** over Shary's own security-governed connector core, not as the security boundary itself.

Practical interpretation:
- Keep Shary authorization, consent, and policy enforcement in-app (same as Option A).
- Expose Shary operations as MCP tools via a local MCP server implementation.
- Optionally offer both transports:
  - local stdio for desktop/local agent runtimes,
  - streamable HTTP for broader clients when needed.

Feasibility rating:
- Technical feasibility: **High**
- Product/security feasibility for P0: **Medium-High** (still needs careful consent and policy UX)
- Recommended timing: **After or in parallel with Option A core**, not before it.

### Should MCP be done?
**Yes, conditionally**:
- Do MCP if the objective includes broad compatibility with existing LLM agent ecosystems and MCP-native clients.
- Do not replace the local connector core with MCP-only logic.
- Preferred architecture: `Shary Core Connector (auth/policy/audit)` + `MCP Adapter Layer (tool schema + protocol transport)`.

### MCP vs Option A (Local Loopback API) - Advantages and Disadvantages

Advantages of MCP (relative to pure local custom API):
- Standardized protocol used by many assistant/client ecosystems.
- Lower integration effort for third-party agent tools already speaking MCP.
- Better portability across assistant hosts (one tool contract style).
- Easier future multi-tool composition (filesystem/db/search + Shary tools in one MCP client session).

Disadvantages of MCP (relative to pure local custom API):
- Additional protocol abstraction and operational surface to maintain.
- More moving parts for lifecycle/version compatibility (MCP SDKs, transports, client quirks).
- Security can become ambiguous if teams mistakenly treat protocol-level wiring as authorization.
- Debugging can be harder across host/client/server boundaries than direct app-local API calls.

Net assessment:
- For Shary's offline-first/security-sensitive context, **Option A remains the foundation**.
- MCP is strong as an interoperability adapter, not as the foundational security model.

### Open-Source MCP Tooling Availability (Checked 2026-05-17)

There is already substantial open-source tooling that can accelerate a Shary MCP adapter:

1. Official protocol/spec and docs:
- `modelcontextprotocol/modelcontextprotocol`
- spec/docs hosted on `modelcontextprotocol.io`

2. Official SDKs (including Kotlin/Java relevant to Android-adjacent stacks):
- `modelcontextprotocol/kotlin-sdk`
- `modelcontextprotocol/java-sdk`
- plus Python/TypeScript/Go/C#/Rust/Swift SDKs

3. Official maintained server examples/components:
- `modelcontextprotocol/servers` (includes filesystem and multiple reference servers)

4. Testing/validation tooling:
- `modelcontextprotocol/inspector` (visual MCP server testing)

5. Ecosystem discovery:
- `modelcontextprotocol/registry` (community-driven MCP server registry service)

6. Third-party servers that show implementation patterns:
- SQLite-focused MCP servers (`panasenco/mcp-sqlite`, `jparkerweb/mcp-sqlite`)
- Proxy/interoperability server (`grll/open-mcp-proxy`)
- Quarkus-based MCP server framework (`quarkiverse/quarkus-mcp-servers`)

Important caveat:
- We did not identify a mature, drop-in open-source MCP server specific to Shary's domain model (fields/users + consent/audit semantics). We should expect to implement a **custom Shary MCP server/adapter**, while reusing official SDKs, inspector, and reference patterns.

### Implementation Recommendation for MCP Track

If MCP track is approved, implement in two layers:

1. Layer 1 (mandatory first): Shary core connector service
- capability checks
- consent policies
- audit events
- dry-run and commit semantics

2. Layer 2: MCP adapter
- map Shary operations to MCP tools:
  - `shary.fields.list`
  - `shary.fields.create`
  - `shary.fields.update`
  - `shary.users.list`
  - `shary.users.create`
  - `shary.users.update`
  - `shary.actions.preview`
  - `shary.actions.commit`
- standardized typed envelopes and explicit error taxonomies
- run MCP Inspector test suite before pilot rollout

Suggested rollout:
- P0: read-only MCP tools (`fields.list`, `users.list`)
- P1: controlled write tools with mandatory preview/commit
- P2: evaluate `agent2user` as explicit `delivery_intent` tool family
