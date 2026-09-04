# Connector Application Module

## Purpose

This module owns application-layer orchestration for pairing connectors,
refreshing sessions, evaluating connector policy, previewing mutating actions,
committing approved actions, and revoking connectors.

## Files and responsibilities

### `PairConnectorUseCase.kt`

- Creates connector client, policy, and session records.
- Enforces active-connector limits and owner-binding completeness.
- Returns pairing credentials such as access token, refresh secret, and expiry.

### `RefreshConnectorSessionUseCase.kt`

- Renews a connector session from a refresh-secret-based request.
- Produces a new session window while preserving connector ownership rules.

### `ManageConnectorsUseCase.kt`

- Revokes one connector or all active connectors.
- Marks related sessions revoked in the repository.

### `PreviewConnectorActionsUseCase.kt`

- Validates proposed field/user mutation actions.
- Stores a pending action bundle and returns a preview summary plus `pendingId`.

### `CommitConnectorActionsUseCase.kt`

- Applies previously previewed actions after confirmation.
- Writes field/user changes, updates pending-action state, and appends audit
  events.

### `ConnectorActionModels.kt`

- Defines the mutation-action payload model shared by preview/commit flows.

### `ConnectorOwnerResolver.kt`

- Resolves the current owner binding required for connector ownership.

### `ConnectorHashing.kt`

- Shared hashing helpers used by connector flows.

### `policy/ConnectorPolicyEvaluator.kt`

- Evaluates capability allowance and confirmation requirements by policy mode.

### `policy/SensitiveFieldMcpPolicy.kt`

- Blocks sensitive reads and updates unless the local per-field sharing flag is
  enabled.
- Denies MCP creation of sensitive fields and transitions to or from the
  sensitive type.
- Is applied by direct transport and preview/commit paths.

## Inputs and outputs

- Inputs:
  - connector repository state;
  - field/user repositories for commits;
  - owner binding and requested capabilities;
  - mutation proposals and confirmation signals.
- Outputs:
  - pair/refresh results;
  - preview summaries and pending IDs;
  - commit results and audit events;
  - revocation status.

## Dependencies

- `ConnectorRepository`
- `FieldRepository`
- `UserRepository`
- connector capability, policy, session, client, pending-action, and audit
  domain types

## Agent notes

- This module is the app-side orchestration layer for connector behavior; it
  should remain aligned with the supported MCP architecture rather than
  introducing a parallel security path.
- `PreviewConnectorActionsUseCase` and `CommitConnectorActionsUseCase` are the
  key retrieval surfaces for understanding preview/commit semantics.
- `ConnectorPolicyEvaluator` is the first stop for confirmation and scope
  behavior questions.

## Update triggers

Update this README when:

- a use case is added, removed, or renamed;
- connector ownership fields or pairing outputs change;
- preview/commit semantics, policy modes, or audit behavior change.
