# Backend Handover: P0/P1 Security + Contract Implementation Plan

Date: 2026-04-25  
Owner domain: Backend (Firebase Functions + Firestore)  
Counterpart domain: Frontend (Android app)

## Purpose

This handover locks backend-side implementation priorities (`P0`, `P1`) and the fixed behavior contract expected by frontend.

Source context:
- `docs/backend-freeze/SECURITY_AUTH_BACKEND_FIREBASE_CONTEXT_2026-04-25.md`
- Backend source-of-truth files (expected in backend repo, not present in this workspace):
  - `firebase/functions/main.py`
  - `firebase/firestore.rules`
  - `firebase/firebase.json`

## Review Delta (2026-04-25)

- Review report: `docs/backend-freeze/HANDOVER_BACKEND_P0_P1_REVIEW_REPORT_2026-04-25.md`
- Evidence scope used for this reevaluation:
  - Frontend repository + local docs were reviewed.
  - Backend runtime code was not directly code-verified in this workspace.
- Internal point updates from reevaluation:
  1. Capability status lines that claim backend implementation should be treated as `doc-asserted` until verified against live backend source/deploy.
  2. Decision parity and idempotent lifecycle (`/request_decision`, ACK schema, recipient isolation) remain lock-critical P0 blockers until backend exports + tests are confirmed.
  3. Contract-test gate remains incomplete without endpoint-level CI fixtures for all app-used endpoints.

## Fixed Contract (Backend Must Keep Stable)

### Endpoint contract currently expected by frontend
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

### Shared data model constraints
- `DataPayload` envelope fields: `user`, `recipient`, `creation_at`, `expires_at`, `data`, `verification`, `signature`
- Hash identity: base64(SHA-256(email canonicalized))
- Recipient-scoped reads/writes only

### Error behavior stability
- Deterministic auth errors for: missing token, invalid token, unverified email, email mismatch, sender/recipient mismatch
- Deterministic validation errors for: invalid hash, invalid TTL/range, malformed payload
- Decision endpoints must return explicit ACK outcome + reason code

## P0 Capability Plan (Backend)

### 1) Verified Email Identity Lifecycle
- Status: Implemented core, missing full contract matrix tests.
- Evidence: `verify_id_token(..., check_revoked=True)` and `email_verified` checks in `functions/main.py`.
- Gap: No complete identity error matrix test suite aligned to app messages.
- Prioritization rework: `P0`.
- Approaches:
  1. Add endpoint-level identity contract tests for all auth failure states.
  2. Standardize backend error-code map (`UNVERIFIED_EMAIL`, `EMAIL_MISMATCH`, `NONCE_STALE`, `KEY_CONFLICT`).
- Feasibility of actions: High.
- Backend implementation steps:
  1. Add test matrix for `/v2/identity/*`.
  2. Add stable error code field in JSON response body.
  3. Freeze response schema in contract doc.
- Frontend awareness requirement:
  - Frontend must map error codes, not raw message strings.

### 2) Strict Authorization + Default Deny
- Status: Implemented.
- Evidence: Firestore deny-all rules and ownership checks in function handlers.
- Gap: Guard logic repetition can drift.
- Prioritization rework: `P0`.
- Approaches:
  1. Keep explicit checks but centralize helper usage.
  2. Add thin auth/ownership decorator layer for handlers.
- Feasibility of actions: High.
- Backend implementation steps:
  1. Refactor repeated checks to single helper path.
  2. Add regression tests for unauthorized access paths.
- Frontend awareness requirement:
  - Frontend must not assume any direct Firestore SDK access path.

### 3) End-to-End Payload Integrity
- Status: Partial.
- Evidence: Identity signature checks are present; payload write path validates shape and ownership.
- Gap: Payload signature semantics are not fully verified server-side against stored sender key.
- Prioritization rework: `P0`.
- Approaches:
  1. Validate payload signature with sender public signing key.
  2. Validate canonical `verification` digest server-side before write.
- Feasibility of actions: Medium.
- Backend implementation steps:
  1. Define canonical payload digest format.
  2. Reject payload if digest/signature verification fails.
  3. Add tests for tampered `data`, `recipient`, and `signature`.
- Frontend awareness requirement:
  - Frontend must generate signature and verification digest exactly per canonical backend format.

### 4) TTL Policy Lock (7 default / 14 max)
- Status: Policy drift.
- Evidence: Backend currently allows broader TTL than lock proposal.
- Gap: Contract doc target and code constants are not aligned.
- Prioritization rework: `P0`.
- Approaches:
  1. Hardcode `default=7d`, `max=14d`.
  2. Env-configurable defaults with hard upper cap at 14d.
- Feasibility of actions: High.
- Backend implementation steps:
  1. Update constants and validation logic.
  2. Add boundary tests (6d/7d/14d/15d).
- Frontend awareness requirement:
  - Frontend must not send TTL above fixed max and should display policy bounds in UI copy.

### 5) Decision Endpoint Contract Parity
- Status: Missing in backend exports vs frontend expectation.
- Evidence: Frontend context expects `/request_decision` and `/payload_decision`.
- Gap: Contract drift currently masked by frontend fallback.
- Prioritization rework: `P0`.
- Approaches:
  1. Implement both endpoints with identical ACK semantics.
  2. Implement one canonical endpoint and maintain compatibility adapter endpoint.
- Feasibility of actions: High.
- Backend implementation steps:
  1. Implement endpoint handlers + auth/recipient ownership checks.
  2. Return stable ACK schema (`status`, `reason_code`, `decision_id`).
  3. Add compatibility/deprecation headers if needed.
- Frontend awareness requirement:
  - Frontend keeps fallback only until parity window is completed and signed off.

### 6) Idempotent Recipient-Scoped Decision Lifecycle
- Status: Missing.
- Evidence: Required invariants defined in security context, but no live endpoint logic yet.
- Gap: Accept/reject idempotency and recipient isolation not guaranteed.
- Prioritization rework: `P0`.
- Approaches:
  1. Upsert decision record keyed by `(recipient, payload_id)`.
  2. Transactional recipient-copy state transition model.
- Feasibility of actions: Medium.
- Backend implementation steps:
  1. Define canonical `payload_id` derivation.
  2. Enforce recipient scope in every decision mutation.
  3. Add tests: repeat accept, accept->reject, wrong recipient attempts.
- Frontend awareness requirement:
  - Frontend must send stable decision identity (`payload_id` or equivalent immutable reference).

### 7) Security + Contract Test Gate
- Status: Partial.
- Evidence: Security tests exist; full endpoint contract matrix is not yet locked.
- Gap: Missing mandatory gate for all app-used endpoints.
- Prioritization rework: `P0`.
- Approaches:
  1. Endpoint contract tests in CI as required deploy gate.
  2. Integration smoke suite run post-deploy.
- Feasibility of actions: High.
- Backend implementation steps:
  1. Add tests for every endpoint listed under fixed contract.
  2. Fail build when contract snapshot changes without approval.
- Frontend awareness requirement:
  - Frontend receives versioned contract snapshots and updates parser/mappers only after approved version bump.

## P1 Capability Plan (Backend)

### 8) Input/Schema Versioning
- Status: Implemented validation, missing schema version governance.
- Evidence: Strict type/hash/size checks exist.
- Gap: No explicit request schema versioning.
- Prioritization rework: `P1`.
- Approaches:
  1. Add `schema_version` field to request/response.
  2. Publish JSON Schema/OpenAPI contract and validate in CI.
- Feasibility of actions: High.

### 9) Abuse Protection (Rate/Replay Beyond Identity Rotate)
- Status: Partial.
- Evidence: Nonce protection exists for key rotation flow.
- Gap: No broad rate limiting for upload/fetch/decision endpoints.
- Prioritization rework: `P1`.
- Approaches:
  1. Firestore-based per-uid/per-IP token bucket.
  2. Upstream rate controls via gateway/armor layer.
- Feasibility of actions: Medium.

### 10) Scheduler/Internal Caller Hardening
- Status: Implemented OIDC verification.
- Evidence: OIDC audience + service account checks in cleanup endpoint.
- Gap: Broader ingress hardening still permissive.
- Prioritization rework: `P1`.
- Approaches:
  1. Keep public ingress + strict token checks.
  2. Migrate cleanup/internal paths toward stricter ingress boundaries.
- Feasibility of actions: Medium.

### 11) Secret/Key Management Baseline
- Status: Partial.
- Evidence: ADC/env credential loading path is active.
- Gap: Need end-to-end secret inventory + rotation evidence across all app/backend components.
- Prioritization rework: `P1`.
- Approaches:
  1. Secret Manager + environment binding.
  2. Rotation checklist with periodic verification and secret scanning.
- Feasibility of actions: High.

### 12) Observability + Correlation IDs
- Status: Partial.
- Evidence: Logging exists.
- Gap: No mandatory `request_id` correlation path across handlers.
- Prioritization rework: `P1`.
- Approaches:
  1. Accept/pass-through `X-Request-Id`.
  2. Generate UUID when absent and include in all logs/responses.
- Feasibility of actions: High.

## Cross-Team Acknowledgment Protocol (Backend to Frontend)

Backend handover is only complete when all statements are acknowledged by frontend:
1. Frontend acknowledges endpoint contract list and payload schemas are the only supported contract.
2. Frontend acknowledges backend error codes are authoritative for UI state mapping.
3. Frontend acknowledges fallback compatibility paths are temporary and date-bounded.
4. Frontend acknowledges TTL policy values and decision ACK model are fixed.

Suggested acknowledgment payload (to be posted by frontend in PR/issue):
- `ACK_BACKEND_CONTRACT_VERSION=<version>`
- `ACK_ENDPOINTS=true`
- `ACK_ERROR_CODES=true`
- `ACK_TTL_POLICY=true`
- `ACK_DECISION_SEMANTICS=true`
