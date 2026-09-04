# P0 Option A Technical Design - Shary Local Connector (2026-05-17)

## 1. Scope
Technical design for implementing P0 Option A local connector architecture in Android app, before coding logic.

## 2. Architecture Overview
Primary pattern:
- `Connector Transport Layer` (local loopback HTTP)
- `Connector Application Layer` (use-cases + validation + mapping)
- `Connector Security Layer` (auth/session/capability/policy)
- `Shary Domain Layer` (fields/users operations)
- `Audit Layer` (immutable event records + query model)

Design principle:
- Transport is replaceable (future MCP adapter), security/policy stays in core.

## 3. Module Boundaries (Proposed)
- `app/application/connector/`
  - PairConnectorUseCase
  - RefreshSessionUseCase
  - PreviewConnectorActionUseCase
  - CommitConnectorActionUseCase
- `app/application/connector/policy/`
  - ConnectorPolicyEvaluator
  - RiskClassifier
- `app/infrastructure/connector/transport/`
  - LoopbackServer
  - RequestRouter
  - AuthInterceptor
- `app/infrastructure/connector/repositories/`
  - ConnectorClientRepository
  - ConnectorSessionRepository
  - ConnectorPolicyRepository
  - ConnectorAuditRepository
- `app/ui/screens/settings/connectors/`
  - ConnectorSettingsScreen
  - ConnectorDetailScreen
  - ConnectorActivityScreen

## 4. Data Model (Local Persistence)
Tables/entities:
1. `connector_clients`
- client_id (pk)
- display_name
- created_at
- status (`paired_limited|paired_full|locked|revoked`)

2. `connector_sessions`
- session_id (pk)
- client_id (fk)
- access_token_hash
- refresh_secret_hash
- issued_at
- expires_at
- revoked_at nullable

3. `connector_policies`
- client_id (pk/fk)
- mode (`strict|balanced|automation`)
- ask_every_time (bool)
- allowed_capabilities (json)
- updated_at

4. `connector_audit_events`
- event_id (pk)
- timestamp
- client_id
- request_id
- operation
- target_type
- target_ids (json)
- decision (`allowed|denied|error`)
- reason_code
- metadata (json)

5. `connector_pending_actions`
- pending_id (pk)
- client_id
- request_id
- proposed_actions (json)
- preview_summary (json)
- status (`previewed|approved|expired|cancelled|committed`)

## 5. Security Model (Technical)
Authentication:
- Pairing bootstrap by one-time challenge.
- Access token short TTL (e.g., 10-20 min).
- Refresh secret with rotation on refresh.

Authorization:
- Capability check per endpoint.
- Policy check per action and risk class.
- Deny default for unknown scope/action.

Protection controls:
- Token hashing at rest (never plaintext storage).
- Pairing attempt rate limit + exponential cooldown.
- Session revocation list check on every request.

## 6. API Endpoint Design (P0)
Base: `http://127.0.0.1:<port>/connector/v1`

Session:
- `POST /session/pair`
- `POST /session/refresh`

Resources:
- `GET /fields`
- `POST /fields`
- `PATCH /fields/{fieldId}`
- `GET /users`
- `POST /users`
- `PATCH /users/{userId}`

Action orchestration:
- `POST /actions/preview`
- `POST /actions/commit`

Headers:
- `Authorization: Bearer <token>`
- `X-Request-Id: <uuid>`
- `X-Schema-Version: 1.0`
- `Idempotency-Key: <uuid>` for mutating routes

## 7. Request/Response Envelope
Success envelope:
- `request_id`
- `schema_version`
- `timestamp`
- `data`

Error envelope:
- `request_id`
- `schema_version`
- `error.code`
- `error.message`
- `error.details`
- `retryable`

## 8. Preview/Commit Execution Model
Preview:
- Validate payload/schema/capabilities.
- Build deterministic execution plan.
- Return summary: item counts, targets, risk level, expected mutations.
- Persist pending action snapshot.

Commit:
- Re-validate session + policy at execution time.
- Execute plan transactionally per item batch.
- Return per-item status list with failures isolated.
- Persist audit event and finalize pending action state.

## 9. Concurrency and Idempotency
- Enforce idempotency key uniqueness for mutation window.
- Deduplicate repeated commits by `request_id + idempotency_key`.
- Use optimistic concurrency/version checks on field/user updates.

## 10. Observability and Audit
Minimum telemetry counters:
- pair_success / pair_failed
- auth_denied / scope_denied / policy_denied
- preview_requests / commit_requests
- commit_success_items / commit_failed_items
- revocations

Audit guarantees:
- append-only event writes.
- event persisted for both allowed and denied operations.

## 11. Failure Modes and Mitigations
- Transport unavailable: expose connector status as `offline` in UI.
- Session replay attempt: reject by token freshness + rotation checks.
- Partial commit failures: return granular status and safe retries.
- Excessive prompts: policy presets and `ask every time` override behavior.

## 12. Test Strategy (Pre-Implementation Design)
Unit tests:
- Policy evaluator decisions by scope/risk/mode.
- Preview planner determinism.
- Commit idempotency and dedup behavior.

Integration tests:
- Pair -> read success flow.
- Read-only scope deny on write.
- Preview success, commit denied by policy.
- Revoke session then deny further requests.

UI tests:
- Connector state transitions.
- Scope toggles and policy mode persistence.
- Activity ledger filtering.

## 13. Implementation Phases (Technical)
Phase 0:
- Schema/entities + repositories + envelope models.

Phase 1:
- Loopback transport and auth interceptor.

Phase 2:
- Read endpoints (`fields.read`, `users.read`) + audit.

Phase 3:
- Preview/commit orchestrator + write endpoints.

Phase 4:
- Settings UI + activity ledger + revoke/lock controls.

## 14. MCP Compatibility Hook (No Implementation Yet)
Future adapter path:
- Keep use-cases transport-agnostic.
- Add MCP server adapter mapping MCP tools to the same application layer.
- Reuse policy and audit components unchanged.

## 15. Go/No-Go Gates Before Coding
1. Approve capability matrix and risk classifier.
2. Approve session TTL and lockout defaults.
3. Approve error code catalog and envelope schema.
4. Approve UI consent flows for strict/balanced/automation.
