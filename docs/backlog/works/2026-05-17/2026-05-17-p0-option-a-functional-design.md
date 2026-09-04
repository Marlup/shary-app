# P0 Option A Functional Design - Shary Local Connector (2026-05-17)

## 1. Scope
Define the functional behavior for P0 of Option A (local loopback connector in installed Shary app) before implementation.

In-scope P0:
- Assistant/agent pairing with explicit user action.
- Read access to fields and users.
- Controlled write access to add/edit fields and users.
- Dry-run preview and explicit commit for mutations.
- User-visible audit activity and connector revocation.

Out-of-scope P0:
- `agent2user` delivery execution.
- Cloud-brokered connector mode.
- Background autonomous bulk mutation without user policy.

## 2. Actors and Roles
- Device owner (primary user): approves, configures, revokes connectors.
- Connector client (assistant/agent runtime): performs scoped operations.
- Shary app policy engine: enforces permissions, consent, and safety controls.

## 3. Core User Stories
1. As a user, I can pair an assistant securely so it can access only allowed scopes.
2. As a user, I can allow read-only usage without enabling write access.
3. As a user, I can require "ask every time" for writes.
4. As a user, I can inspect all assistant actions in an activity ledger.
5. As a user, I can revoke or lock a connector immediately.
6. As an assistant, I can preview a planned mutation and receive a clear impact summary before commit.

## 4. Capability Matrix (P0)
- `fields.read`: list/read fields.
- `fields.write`: create/edit fields.
- `users.read`: list/read users.
- `users.write`: create/edit users.
- `actions.preview`: simulate mutations and return impact.
- `actions.commit`: execute approved mutation batch.

Rules:
- Deny by default.
- No wildcard/all-access capability.
- `actions.commit` requires relevant write scopes.

## 5. Pairing and Session Flow
1. User opens `Settings -> Assistant Connectors -> Pair new connector`.
2. App shows one-time pairing code/QR and scope selection.
3. Client submits pairing challenge.
4. App issues short-lived access token + renewable session.
5. Connector appears in list with state `paired-limited` or `paired-full`.

Failure behaviors:
- Invalid code: reject with cooldown suggestion.
- Expired pairing token: require re-init by user.
- Too many failed attempts: temporary lock.

## 6. Consent and Policy Modes
Per connector policy mode:
- `Strict`: read allowed if scoped; every write requires explicit confirmation.
- `Balanced`: low-risk writes allowed, sensitive writes confirm.
- `Automation`: scoped writes auto-allowed with limits and full audit.

Global override:
- `Ask every time` toggle can force per-operation confirmation.

## 7. UX States and Screens
Main screen: `Assistant Connectors`
- States: disconnected, paired-limited, paired-full, revoked, locked.
- For each connector show:
  - display name and client id
  - enabled scopes
  - last activity timestamp
  - status badge

Detail screen per connector:
- Scope toggles and policy mode selector.
- Session controls: rotate key, revoke, temporary lock.
- Activity quick link.

Activity ledger screen:
- List operations with time, connector id, action, target, result.
- Filters: all/success/denied/error and operation type.

## 8. Functional API Contract (Behavioral)
- Pairing: create/validate connector session.
- Read operations: return scoped data only.
- Preview operation: no mutation, includes impact summary.
- Commit operation: applies only if policy permits at execution time.

Common behavior:
- Every request carries `request_id`.
- Every response includes `schema_version`.
- Errors are structured and user-actionable.

## 9. Error Semantics (Functional)
- `unauthorized_scope`: scope missing.
- `session_expired`: session renewal needed.
- `policy_denied`: blocked by user policy.
- `confirmation_required`: user confirmation missing.
- `rate_limited`: request budget exceeded.
- `validation_failed`: payload/schema error.

## 10. Non-Functional Requirements (P0)
- Offline-first: all local connector operations must function without cloud.
- Safety-first: no silent writes beyond granted policy.
- Auditability: every mutation attempt recorded (allow/deny).
- Responsiveness: pairing and read flows should feel immediate on-device.

## 11. Acceptance Criteria (Functional)
1. User can pair/revoke connector from UI without developer tools.
2. Read-only connector cannot perform mutations.
3. Write operation without permission is denied and logged.
4. Preview never mutates data.
5. Commit follows policy mode and logs full outcome.
6. Activity ledger reflects success, deny, and error outcomes.

## 12. Open Decisions Before Implementation
- Exact classification of low-risk vs sensitive write actions.
- Session TTL defaults and lockout window.
- Final copy text for consent prompts and risk labels.
