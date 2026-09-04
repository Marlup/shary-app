# Frontend Handover: P0/P1 Security + Contract Implementation Plan

Date: 2026-04-25  
Owner domain: Frontend (Android app)  
Counterpart domain: Backend (Firebase Functions + Firestore)

## Purpose

This handover locks frontend-side implementation priorities (`P0`, `P1`) and the fixed behavior/frontend obligations expected by backend.

Primary source:
- `docs/backend-freeze/SECURITY_AUTH_BACKEND_FIREBASE_CONTEXT_2026-04-25.md`

## Review Delta (2026-04-25)

- Review report: `docs/frontend-freeze/HANDOVER_FRONTEND_P0_P1_REVIEW_REPORT_2026-04-25.md`
- Risk-status legend used in review:
  - `LOW`: implemented and stable with minor residual drift risk
  - `MEDIUM`: partially implemented or missing hardening/tests
  - `HIGH`: missing or contract-critical gap that can break lock goals
- Internal point updates from this review:
  1. Endpoint fallback (`/request_decision` -> `/payload_decision`) is currently always-on in code; it must be treated as transitional behavior until explicit backend parity ACK.
  2. Contract lock quality gate is not yet present in tests; current tests do not cover endpoint/schema compatibility.
  3. Correlation ID propagation is not implemented for outbound requests; backend-side trace fields cannot be consistently linked to client actions yet.

## Fixed Contract (Frontend Must Follow)

### Endpoint usage (no implicit alternatives)
- `/ping`
- `/v2/identity/register`
- `/v2/identity/rotate`
- `/v2/identity/delete`
- `/v2/identity/pubkey?user_hash=...`
- `/upload_payload`
- `/upload_request`
- `/fetch_payload?user=...`
- `/fetch_request?user=...`
- `/payload_decision`
- `/request_decision`

### Shared payload model and identity assumptions
- Envelope fields: `user`, `recipient`, `creation_at`, `expires_at`, `data`, `verification`, `signature`
- `user` and `recipient` are canonical email-hash identity references
- Frontend encryption/decryption context and AAD format must stay deterministic

### Required runtime behavior
- Cloud actions only when `cloudReady = isOnline && isUserValidated`
- Verified email required before identity writes
- No silent auto-import from cloud inbox; use review/accept/reject/cancel flow

## P0 Capability Plan (Frontend)

### 1) Verified Email Identity Lifecycle Mapping
- Status: Implemented.
- Evidence: Verified-email signin gating and identity validation in security context.
- Gap: Needs explicit parity checks against backend error code matrix.
- Prioritization rework: `P0`.
- Approaches:
  1. Map UI state to backend error codes only.
  2. Add deterministic auth-failure integration tests (unverified, mismatch, revoked).
- Feasibility of actions: High.
- Frontend implementation steps:
  1. Freeze auth error mapping table.
  2. Add tests for identity operations with mocked backend responses.
- Backend awareness requirement:
  - Backend must return stable code fields, not only free-text error strings.

### 2) Strict Authorization Contract Compliance
- Status: Implemented usage pattern.
- Evidence: App routes all cloud access through function endpoints and validated token state.
- Gap: Potential drift if legacy helper paths bypass centralized cloud service.
- Prioritization rework: `P0`.
- Approaches:
  1. Route all cloud calls through single cloud service adapter.
  2. Add static check/review rule that forbids direct endpoint ad hoc calls in UI layer.
- Feasibility of actions: High.
- Frontend implementation steps:
  1. Audit for any direct networking outside cloud service abstraction.
  2. Remove or mark deprecated paths as incompatible.
- Backend awareness requirement:
  - Backend must keep direct Firestore access denied and stable.

### 3) End-to-End Payload Integrity Compliance
- Status: Implemented core.
- Evidence: Client-side encryption + signature/verification envelope model in context file.
- Gap: Canonical digest/signature format must be explicitly locked with backend.
- Prioritization rework: `P0`.
- Approaches:
  1. Freeze canonical signing input format in shared contract.
  2. Add cross-language test vectors shared with backend tests.
- Feasibility of actions: Medium.
- Frontend implementation steps:
  1. Produce canonicalization helper tests.
  2. Validate generated payloads against backend test fixtures.
- Backend awareness requirement:
  - Backend must validate signature/digest with same canonical field order.

### 4) TTL Policy Alignment (7 default / 14 max)
- Status: Potential drift.
- Evidence: Security context proposes strict TTL bounds.
- Gap: Frontend may allow values outside final backend policy if not constrained in UI/business layer.
- Prioritization rework: `P0`.
- Approaches:
  1. Enforce TTL bounds in UI/model before request creation.
  2. Fetch TTL policy from backend metadata endpoint (if added) and enforce dynamically.
- Feasibility of actions: High.
- Frontend implementation steps:
  1. Apply fixed TTL defaults and max constraints in request builders.
  2. Add tests for boundary values.
- Backend awareness requirement:
  - Backend must expose and enforce same max policy.

### 5) Decision Endpoint Contract Parity + Fallback Exit Plan
- Status: Partial (fallback logic exists).
- Evidence: `request_decision` with fallback to `payload_decision` described in context.
- Gap: Fallback can hide backend drift and delay contract lock.
- Prioritization rework: `P0`.
- Approaches:
  1. Keep fallback only under explicit feature flag and telemetry.
  2. Remove fallback after backend parity stability window and signed ACK.
- Feasibility of actions: High.
- Frontend implementation steps:
  1. Instrument fallback trigger count.
  2. Define removal criteria date/version.
- Backend awareness requirement:
  - Backend must provide parity semantics for both endpoints during transition.

### 6) Idempotent Inbox Decision Lifecycle
- Status: Partial.
- Evidence: Controlled review UX exists.
- Gap: Need stable client-side idempotency keys/reference IDs aligned to backend decision model.
- Prioritization rework: `P0`.
- Approaches:
  1. Use immutable payload identity for accept/reject operations.
  2. Add local dedup guard to prevent duplicate decision submissions.
- Feasibility of actions: Medium.
- Frontend implementation steps:
  1. Add idempotency reference in decision call payload.
  2. Disable repeated taps while request in-flight.
- Backend awareness requirement:
  - Backend must treat duplicate decision submissions as idempotent ACK, not conflict.

### 7) Security + Contract Test Gate (Frontend Side)
- Status: Partial.
- Evidence: Rich behavior exists but endpoint contract snapshot test gate is not fully locked.
- Gap: No enforced test gate proving frontend contract compatibility per release.
- Prioritization rework: `P0`.
- Approaches:
  1. Add contract compatibility tests against backend schema snapshots.
  2. Add pre-release smoke script for all used endpoints.
- Feasibility of actions: High.
- Frontend implementation steps:
  1. Freeze endpoint request/response fixtures.
  2. Fail release build if fixtures drift without approved contract bump.
- Backend awareness requirement:
  - Backend publishes versioned schema/contract updates before rollout.

## P1 Capability Plan (Frontend)

### 8) Schema Version Awareness
- Status: Missing explicit handling.
- Evidence: Validation exists conceptually, but schema version contract is not explicit.
- Gap: Frontend cannot gracefully adapt across schema evolutions.
- Prioritization rework: `P1`.
- Approaches:
  1. Add `schema_version` in payloads and parser handling.
  2. Introduce compatibility parser branch by version.
- Feasibility of actions: High.

### 9) Abuse Resistance in UX/Client Flows
- Status: Partial.
- Evidence: cloud gating exists.
- Gap: No client-side submission throttling/anti-spam controls for repetitive actions.
- Prioritization rework: `P1`.
- Approaches:
  1. Add debounce/rate controls for upload/fetch/decision actions.
  2. Add cooldown display and retry-after handling for backend 429 responses.
- Feasibility of actions: High.

### 10) Scheduler/Internal Caller Awareness
- Status: N/A direct frontend implementation, required awareness.
- Evidence: Cleanup auth now OIDC/IAM-based in backend docs.
- Gap: Frontend docs may still imply client-triggered cleanup assumptions.
- Prioritization rework: `P1`.
- Approaches:
  1. Document cleanup as backend-only operational concern in frontend docs.
  2. Remove any UI wording implying local/manual cleanup triggers.
- Feasibility of actions: High.

### 11) Secret Hygiene in App Code and Build
- Status: Partial.
- Evidence: Context indicates secure local credential wrapping, but secret hygiene must remain enforced.
- Gap: Ensure no hardcoded operational credentials remain in app code/build scripts.
- Prioritization rework: `P1`.
- Approaches:
  1. Secret scan and remove hardcoded credentials.
  2. Build-time injection through secure CI secrets.
- Feasibility of actions: High.

### 12) Observability + Correlation ID Propagation
- Status: Missing explicit client propagation.
- Evidence: Backend intends correlation ID support.
- Gap: Frontend must generate/pass request IDs for support and traceability.
- Prioritization rework: `P1`.
- Approaches:
  1. Generate `X-Request-Id` per cloud action and include in headers.
  2. Persist request IDs in local diagnostics screen/log export.
- Feasibility of actions: High.

## Cross-Team Acknowledgment Protocol (Frontend to Backend)

Frontend handover is only complete when all statements are acknowledged by backend:
1. Backend acknowledges endpoint list and schema snapshots as shared source of truth.
2. Backend acknowledges stable error code fields for frontend mapping.
3. Backend acknowledges temporary fallback compatibility period and deprecation date.
4. Backend acknowledges deterministic ACK semantics for decision endpoints.

Suggested acknowledgment payload (to be posted by backend in PR/issue):
- `ACK_FRONTEND_CONTRACT_VERSION=<version>`
- `ACK_SCHEMA_SNAPSHOT=true`
- `ACK_ERROR_CODES=true`
- `ACK_DECISION_ACK_MODEL=true`
- `ACK_FALLBACK_DEPRECATION_DATE=<date>`
