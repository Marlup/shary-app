# P0 Option A Implementation - Phase 3 and Phase 4 (2026-05-17)

## Scope
Execute requested phases:
- Phase 3: write endpoints + preview/commit orchestration.
- Phase 4: connector settings and activity UI surface.

## Phase 3 changes

### Application layer
- Added mutation action model:
  - `ConnectorMutationAction`
  - `ConnectorMutationType`
- Added preview orchestration use-case:
  - `PreviewConnectorActionsUseCase`
  - validates action payloads
  - stores pending action snapshot
  - returns deterministic summary counts
- Added commit orchestration use-case:
  - `CommitConnectorActionsUseCase`
  - applies pending actions to field/user repositories
  - records per-item success/failure
  - updates pending state and appends audit event

### Domain model/repository alignment
- Updated `ConnectorPendingAction` to carry typed `proposedActions` and structured `previewSummary`.

### Transport layer
Extended loopback router with write/orchestration endpoints:
- `POST /connector/v1/fields`
- `PATCH /connector/v1/fields/{fieldId}`
- `POST /connector/v1/users`
- `PATCH /connector/v1/users/{email}`
- `POST /connector/v1/actions/preview`
- `POST /connector/v1/actions/commit`

Added request body support in loopback request model and confirmation gating via `X-Confirm: true`.

## Phase 4 changes
- Added `ConnectorSettingsViewModel`:
  - exposes paired clients and audit events
  - provides quick pair actions (read-only and read-write)
- Extended `SettingsScreen` with an `Assistant Connectors` section:
  - pair buttons
  - paired connector summary
  - recent connector activity summary

## Notes
- Loopback transport remains in-process (`handle(request)`) and transport-agnostic; socket-bound localhost hosting remains a later binding step.
- Tag handling uses safe fallback (`Tag.fromString(..., null)` -> `Unknown`) because current `Tag` model is sealed and color-backed.

## Validation
- Command: `./gradlew :app:compileDebugKotlin`
- Result: BUILD SUCCESSFUL.

## Pending follow-up
- Persistent connector storage backend (replace in-memory scaffold).
- Endpoint tests for auth/policy matrix and preview/commit item-level behaviors.
- UI actions for revoke/lock/rotate session and full ledger filters.
