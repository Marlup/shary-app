# Backend Final Decision Freeze Brief (Contract Authority + Frontend Translation)

Date: 2026-04-25  
Audience: Backend owner team (Firebase Functions + Firestore)  
Intent: Backend is the final decisor of requirements and fixed capabilities.

## 1) Objective

Freeze backend security/contract capabilities as the source of truth, then publish a final fixed-capabilities package that frontend must adapt to.

This document is the execution brief for that freeze process.

## 2) Decision Authority Model

- Final requirement and capability decisions: Backend team.
- Frontend role: adapt to backend-fixed contract and capabilities after freeze publication.
- If conflict exists between existing frontend behavior and backend freeze outcome, backend freeze outcome wins.

## 3) Current Status Awareness (Pre-Freeze)

Based on current handover and review docs:
- Backend P0 blockers are not fully closed yet.
- Highest-risk freeze blockers are:
  1. Decision endpoint parity (`/request_decision` vs `/payload_decision`)
  2. Idempotent recipient-scoped decision lifecycle
  3. Contract CI test gate across all app-used endpoints
  4. TTL lock alignment (7d default / 14d max)
  5. Full payload integrity verification (digest/signature canonicalization)

Evidence note:
- Backend code files are not present in this workspace, so backend status claims are currently doc-asserted and must be re-verified in backend repo/deploy.

## 4) Resource Package Backend Must Use

### A) Contract + status docs (this workspace)
- `docs/backend-freeze/HANDOVER_BACKEND_P0_P1_IMPLEMENTATION_2026-04-25.md`
- `docs/backend-freeze/HANDOVER_BACKEND_P0_P1_REVIEW_REPORT_2026-04-25.md`
- `docs/backend-freeze/SECURITY_AUTH_BACKEND_FIREBASE_CONTEXT_2026-04-25.md`
- `docs/frontend-freeze/HANDOVER_FRONTEND_P0_P1_REVIEW_REPORT_2026-04-25.md`
- `docs/backend-freeze/FIREBASE_FUNCTIONS_PYTHON_DEPLOY_CHECKLIST.md`
- `docs/backend-freeze/IDENTITY_VERIFICATION_AND_BACKEND_CONTRACT.md`

### B) Backend source-of-truth files (backend repo)
- `firebase/functions/main.py`
- `firebase/firestore.rules`
- `firebase/firestore.indexes.json`
- `firebase/firebase.json`
- backend tests/CI definitions for endpoint and security gates

### C) Runtime/deploy evidence
- Active deployed function exports/routes
- Active ingress/auth policy
- Active scheduler invoker/OIDC configuration
- Post-deploy smoke and contract test outputs

## 5) Mandatory Freeze Decisions (Backend)

Backend must finalize and publish explicit decisions for all items below.

1. Endpoint freeze
- Exact supported endpoint list and transition policy for compatibility endpoints.

2. ACK schema freeze
- Final response schema for decision endpoints, including required fields:
  - `status`
  - `reason_code`
  - `decision_id`

3. Error-code freeze
- Stable backend codes for auth/identity/validation and decision failures.

4. Payload integrity freeze
- Canonical digest and signature verification format.
- Server-side verification requirements before write/accept.

5. TTL freeze
- `default=7d`, `max=14d` (or explicitly approved alternative), with validation behavior.

6. Idempotency freeze
- Canonical immutable decision identity (`payload_id` or equivalent).
- Duplicate decision behavior and recipient-isolation guarantees.

7. Observability freeze
- `X-Request-Id` handling contract (accept/generate/echo/log).

8. Test-gate freeze
- Required CI contract tests and mandatory deployment gate criteria.

## 6) Required Backend Deliverables (Final Freeze Package)

Backend should produce a single versioned package with these artifacts:

1. `BACKEND_FIXED_CAPABILITIES_V1.md`
- Final decisions per capability (P0/P1), no open ambiguities.

2. `BACKEND_API_CONTRACT_V1` (OpenAPI/JSON schema or equivalent)
- Request/response schemas for all app-used endpoints.

3. `BACKEND_ERROR_CODES_V1.md`
- Stable code list, semantics, and mapping guidance.

4. `BACKEND_DECISION_LIFECYCLE_V1.md`
- Idempotency model, recipient scope rules, ACK semantics.

5. `BACKEND_TEST_GATE_V1.md`
- CI gates, blocking criteria, smoke protocol, required evidence.

6. `BACKEND_MIGRATION_AND_DEPRECATION_V1.md`
- Compatibility window, deprecation dates, rollback policy.

## 7) Frontend Translation Protocol (After Backend Freeze)

Once backend freeze package is published, frontend must perform translation in this order:

1. Contract sync
- Replace frontend assumptions with backend-fixed schemas and codes.

2. Compatibility cleanup
- Remove temporary fallback paths when backend deprecation criteria are met.

3. Parser and mapper alignment
- Bind UI state to backend codes and fixed ACK semantics.

4. Policy alignment
- Match TTL, decision lifecycle, and request-id propagation behavior.

5. Frontend test adaptation
- Update/add tests to enforce backend contract compatibility.

6. Final adaptation artifact
- Publish `FRONTEND_ADAPTATION_FROM_BACKEND_V1.md` with implementation diff and evidence.

## 8) Acceptance and Sign-Off Protocol

### Backend freeze completion payload

- `ACK_BACKEND_FINAL_DECISOR=true`
- `ACK_BACKEND_CONTRACT_VERSION=<version>`
- `ACK_ENDPOINT_FREEZE=true`
- `ACK_ERROR_CODE_FREEZE=true`
- `ACK_DECISION_LIFECYCLE_FREEZE=true`
- `ACK_TTL_POLICY_FREEZE=true`
- `ACK_TEST_GATE_FREEZE=true`
- `ACK_FALLBACK_DEPRECATION_DATE=<date>`

### Frontend adaptation completion payload

- `ACK_FRONTEND_ADAPTED_TO_BACKEND_VERSION=<version>`
- `ACK_FRONTEND_ENDPOINTS_ALIGNED=true`
- `ACK_FRONTEND_ERROR_MAPPING_ALIGNED=true`
- `ACK_FRONTEND_DECISION_SEMANTICS_ALIGNED=true`
- `ACK_FRONTEND_TEST_GATE_UPDATED=true`

## 9) Operational Sequence (Recommended)

1. Backend verifies current source/deploy status against this brief.
2. Backend closes P0 lock-critical blockers.
3. Backend publishes final freeze package (versioned).
4. Frontend executes adaptation protocol to that exact version.
5. Joint sign-off with both ACK payloads.

## 10) Non-Negotiable Rule

No frontend capability or behavior is considered final unless it is aligned to the backend freeze package version acknowledged by backend as final decisor.
