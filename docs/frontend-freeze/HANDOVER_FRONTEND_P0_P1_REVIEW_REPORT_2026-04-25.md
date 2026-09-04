# Frontend P0/P1 Review Report (Implementation vs Suggested Capabilities)

Date: 2026-04-25  
Scope reviewed: `docs/frontend-freeze/HANDOVER_FRONTEND_P0_P1_IMPLEMENTATION_2026-04-25.md`

## Summary

- P0 overall: strong baseline implementation, but lock-critical gaps remain in fallback control, idempotency formalization, and contract test gate.
- P1 overall: mostly planned/not implemented yet (schema versioning, correlation IDs, anti-abuse controls).

Risk-status scale:
- `LOW`: implemented and stable with minor residual drift risk
- `MEDIUM`: partially implemented or missing hardening/tests
- `HIGH`: missing or contract-critical gap that can break lock goals

## Capability Evaluation

### 1) Verified Email Identity Lifecycle Mapping
- Risk-Status of changes: `MEDIUM`
- Description:
  - Current implementation already enforces verified email in sign-in and identity operations.
  - Gap remains on deterministic backend error-code matrix parity and dedicated integration coverage.
- Current evidence:
  - `AuthenticationServiceImpl` verifies email at sign-in and blocks unverified access.
  - `CloudServiceImpl.ensureVerifiedIdentityForEmail()` enforces email/token ownership.
  - UI error mapping currently relies on exception types/messages, not stable backend code fields.
- Contrast to next suggestion:
  - Suggested "error-code-only mapping + deterministic auth failure integration tests" is not fully implemented.

### 2) Strict Authorization Contract Compliance
- Risk-Status of changes: `LOW`
- Description:
  - Cloud endpoints are centralized in `CloudServiceImpl`; no significant ad hoc UI-layer endpoint usage was found.
  - Residual drift risk is mostly future-maintenance risk, not current breakage.
- Current evidence:
  - Network calls and endpoint constants are concentrated in `infrastructure/services/cloud`.
- Contrast to next suggestion:
  - Suggested static enforcement rule for forbidding direct calls is still missing as a hard gate.

### 3) End-to-End Payload Integrity Compliance
- Risk-Status of changes: `MEDIUM`
- Description:
  - Core encryption/signature envelope flow is implemented.
  - Canonical digest/signature format is implemented in code but not locked via shared cross-language test vectors.
- Current evidence:
  - Envelope model and AAD are deterministic in `CloudServiceImpl` + `DataPayload`.
- Contrast to next suggestion:
  - Suggested canonicalization helper tests and backend fixture parity are not yet present.

### 4) TTL Policy Alignment (7 default / 14 max)
- Risk-Status of changes: `LOW`
- Description:
  - TTL is bounded in business logic and UI selection to 7d/14d.
  - Policy is consistently represented in settings and upload pipeline.
- Current evidence:
  - `CloudInboxPolicy`: default 7, max 14.
  - `CloudServiceImpl.buildLoad()` clamps expiry.
  - Settings UI exposes only 7d and 14d.
- Contrast to next suggestion:
  - Suggested dynamic backend metadata-driven TTL policy is not implemented (fixed policy only).

### 5) Decision Endpoint Contract Parity + Fallback Exit Plan
- Risk-Status of changes: `HIGH`
- Description:
  - Fallback exists and is functional, but is currently always active when `request_decision` returns 404/405.
  - This can hide backend drift and delays hard contract lock.
- Current evidence:
  - `CloudServiceImpl.sendRequestDecision()` falls back from `/request_decision` to `/payload_decision`.
- Contrast to next suggestion:
  - Suggested feature-flagged fallback + telemetry + dated removal criteria is not fully implemented.

### 6) Idempotent Inbox Decision Lifecycle
- Risk-Status of changes: `MEDIUM`
- Description:
  - Controlled review UX is implemented with one-by-one accept/reject.
  - No explicit idempotency reference/key is sent in decision payload; duplicate semantics are not contract-locked from client side.
- Current evidence:
  - Decision body currently includes `recipient`, `verification`, `decision`, `notify_sender`.
  - Dialogs switch to loading state, but no explicit dedup token strategy exists.
- Contrast to next suggestion:
  - Suggested idempotency key/reference alignment is missing.

### 7) Security + Contract Test Gate (Frontend Side)
- Risk-Status of changes: `HIGH`
- Description:
  - Existing tests cover UI state and local file behavior, but no endpoint contract snapshot/fixture gate is enforced.
- Current evidence:
  - Current tests: `CloudUiStateTest`, `SendMethodCardTest`, `AuthFilePersistenceTest`, basic examples.
- Contrast to next suggestion:
  - Suggested release-blocking contract compatibility tests are not implemented.

### 8) Schema Version Awareness
- Risk-Status of changes: `HIGH`
- Description:
  - No explicit `schema_version` handling for cloud payload contract was found.
- Current evidence:
  - `DataPayload` contains envelope fields but no schema version discriminator.
- Contrast to next suggestion:
  - Suggested versioned parser branches are not implemented.

### 9) Abuse Resistance in UX/Client Flows
- Risk-Status of changes: `MEDIUM`
- Description:
  - Some protective UX exists (loading states, wifi-only option, biometric gating for inbox).
  - Missing dedicated throttling/debounce policy and 429/retry-after client behavior.
- Current evidence:
  - No explicit 429/cooldown/retry-after handling found.
- Contrast to next suggestion:
  - Suggested anti-spam controls and cooldown UX are not implemented.

### 10) Scheduler/Internal Caller Awareness
- Risk-Status of changes: `LOW`
- Description:
  - No direct frontend-triggered cleanup flow was found in app code.
  - This is mostly documentation clarity work, not code-risk heavy.
- Current evidence:
  - Scheduler/cleanup concerns are documented as backend-side in security docs.
- Contrast to next suggestion:
  - Suggested explicit frontend docs wording cleanup should still be completed to avoid future misunderstanding.

### 11) Secret Hygiene in App Code and Build
- Risk-Status of changes: `MEDIUM`
- Description:
  - No obvious hardcoded operational credentials found in reviewed app code paths.
  - Base URL is hardcoded in build config for debug/release (environment config choice, not secret).
  - No automated secret scan gate is visible in this module.
- Current evidence:
  - `app/build.gradle.kts` defines `FIREBASE_BASE_URL` directly.
- Contrast to next suggestion:
  - Suggested secret scan + CI-backed injection policy is only partially realized.

### 12) Observability + Correlation ID Propagation
- Risk-Status of changes: `HIGH`
- Description:
  - Client currently reads correlation IDs from some responses but does not generate and propagate request IDs in outbound headers.
- Current evidence:
  - `CloudServiceImpl` reads `x-request-id` / `x-correlation-id` / `traceparent` from response.
  - `Utils.authBearerHeader()` only sets Authorization header.
- Contrast to next suggestion:
  - Suggested `X-Request-Id` generation and persistence is not implemented.

## Internal Points Updated From Handover

1. Primary source path was corrected to local repo path:
- from `shary-backend/firebase/docs/...`
- to `docs/backend-freeze/SECURITY_AUTH_BACKEND_FIREBASE_CONTEXT_2026-04-25.md`

2. Review-delta section was added to handover doc with:
- risk legend
- explicit fallback transitional note
- missing contract gate note
- missing correlation propagation note

## Recommended Next Lock Sequence

1. `P0` immediate: Capabilities `5`, `7`, `1`, `6`.
2. Then stabilize: Capability `3` with shared vectors.
3. `P1` hardening: Capabilities `12`, `8`, `9`, then `11` process automation.
