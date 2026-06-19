# Backend P0/P1 Review Report (Implementation vs Suggested Capabilities)

Date: 2026-04-25  
Scope reviewed: `docs/backend-freeze/HANDOVER_BACKEND_P0_P1_IMPLEMENTATION_2026-04-25.md`

## Evidence Scope

- Reviewed artifacts in this workspace: backend handover doc, backend-context doc, deploy checklist doc, and Android-facing contract usage.
- Backend codebase files referenced by handover (`firebase/functions/main.py`, `firebase/firestore.rules`, `firebase/firebase.json`) are not present in this workspace, so backend implementation claims are treated as doc-asserted, not code-verified.

Risk-status scale:
- `LOW`: implemented and stable with low drift risk
- `MEDIUM`: partially implemented or unverified in this workspace
- `HIGH`: missing/partial and contract-critical for lock

## Capability Evaluation

### 1) Verified Email Identity Lifecycle
- Risk-Status of changes: `MEDIUM`
- Description:
  - Handover claims core implementation (`check_revoked`, `email_verified`) but matrix coverage is explicitly incomplete.
  - Without backend code access in this workspace, implementation is not directly verifiable.
- Contrast current vs suggestion:
  - Suggested stable error-code matrix and full `/v2/identity/*` tests are still pending and remain required for contract lock.

### 2) Strict Authorization + Default Deny
- Risk-Status of changes: `MEDIUM`
- Description:
  - Handover states implemented deny-all + ownership checks, but also notes guard logic repetition drift risk.
  - No direct rules/handler verification possible here.
- Contrast current vs suggestion:
  - Suggested central helper/decorator hardening is still advisable to prevent regression drift.

### 3) End-to-End Payload Integrity
- Risk-Status of changes: `HIGH`
- Description:
  - Document marks this as partial and identifies missing full server-side signature/digest verification.
  - This is contract-critical for tamper/spoof resistance.
- Contrast current vs suggestion:
  - Canonical digest definition + strict signature checks + tamper tests are not yet locked.

### 4) TTL Policy Lock (7 default / 14 max)
- Risk-Status of changes: `HIGH`
- Description:
  - Document explicitly reports policy drift between code/constants and lock proposal.
  - Frontend already enforces 7/14 behavior, so backend drift can cause hidden contract mismatch.
- Contrast current vs suggestion:
  - Suggested constants/validation update and boundary tests are still needed.

### 5) Decision Endpoint Contract Parity
- Risk-Status of changes: `HIGH`
- Description:
  - Document states missing backend exports/parity while frontend currently relies on fallback behavior.
  - This is a direct lock blocker for final endpoint contract stabilization.
- Contrast current vs suggestion:
  - Implementing both endpoints (or canonical + compatibility adapter) with stable ACK schema is still outstanding.

### 6) Idempotent Recipient-Scoped Decision Lifecycle
- Risk-Status of changes: `HIGH`
- Description:
  - Document marks as missing; invariants not guaranteed.
  - Without idempotency + recipient isolation, duplicate/replayed decisions can break consistency.
- Contrast current vs suggestion:
  - Payload identity model (`payload_id`) + transaction-safe state transitions + repeat-decision tests remain pending.

### 7) Security + Contract Test Gate
- Risk-Status of changes: `HIGH`
- Description:
  - Security tests are claimed to exist, but mandatory endpoint contract matrix gate is marked incomplete.
  - This is a release-quality lock requirement.
- Contrast current vs suggestion:
  - CI gate for all app-used endpoints and contract snapshot approval flow is still not closed.

### 8) Input/Schema Versioning
- Risk-Status of changes: `MEDIUM`
- Description:
  - Validation exists, but explicit schema-version governance is missing per document.
- Contrast current vs suggestion:
  - `schema_version` + published schema governance path is still missing.

### 9) Abuse Protection (Rate/Replay Beyond Identity Rotate)
- Risk-Status of changes: `MEDIUM`
- Description:
  - Rotation nonce protections exist, but broad endpoint rate limiting/replay controls are not yet defined as complete.
- Contrast current vs suggestion:
  - Token bucket / gateway rate controls remain a pending P1 hardening objective.

### 10) Scheduler/Internal Caller Hardening
- Risk-Status of changes: `MEDIUM`
- Description:
  - OIDC checks are documented as implemented for cleanup.
  - Document also states ingress posture remains broader than ideal.
- Contrast current vs suggestion:
  - Additional ingress tightening for internal paths is still pending.

### 11) Secret/Key Management Baseline
- Risk-Status of changes: `MEDIUM`
- Description:
  - ADC/env loading approach is documented, but full inventory/rotation proof is not yet locked.
- Contrast current vs suggestion:
  - Secret Manager binding + recurring verification/scan process remains incomplete.

### 12) Observability + Correlation IDs
- Risk-Status of changes: `MEDIUM`
- Description:
  - Logging exists, but mandatory end-to-end request correlation path is not contract-locked.
- Contrast current vs suggestion:
  - `X-Request-Id` pass-through/generation and response/log propagation standard still pending.

## Re-prioritized Lock-Critical Set

1. Immediate P0 blockers to resolve first:
- Capability 5 (decision endpoint parity)
- Capability 6 (idempotent recipient-scoped decisions)
- Capability 7 (contract test gate)
- Capability 4 (TTL policy lock)
- Capability 3 (full payload integrity verification)

2. Then close identity robustness:
- Capability 1 (error-code matrix + tests)
- Capability 2 (auth helper centralization)

3. P1 stabilization track:
- 8, 9, 10, 11, 12 after P0 lock completion.

## Internal Points Updated in Handover

1. Source context path was corrected to the local backend-context doc path and marked backend source files as external to this workspace.
2. Added explicit evidence-scope warning: backend claims are doc-asserted unless verified in backend repo/deploy.
3. Added lock-critical note emphasizing decision parity/idempotency/test-gate as unresolved completion criteria.
