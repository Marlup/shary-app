# Shary Security/Auth + Firebase Backend Context (Locked Roadmap Draft)

Date: 2026-04-25  
Target audience: Agent implementing Firebase Functions + Firestore services

## 1) Executive State Snapshot

This Android app is already in a hybrid state:
- Local auth is device-centric and can work offline.
- Cloud operations are gated by both online status and verified-email state.
- Identity registration/rotation/deletion is wired to a `/v2/identity/*` contract.
- Cloud download/import has moved to controlled inbox review (accept/cancel/reject), not immediate auto-import from UI.

Backend work should now focus on locking the contract and removing remaining ambiguity paths.

## 2) Current Implemented Security/Authentication Capabilities (Android)

### 2.1 Firebase auth and verification behavior

- Signup uses Firebase Email/Password and sends verification email if needed.
- Signin requires verified email; if not verified, login is blocked and a verification email is re-sent.
- Cloud identity operations enforce verified-email ownership and email-token consistency.

Implementation evidence:
- `AuthenticationServiceImpl.signUp/signIn`, verification flow, and Firebase exception mapping.
- `CloudServiceImpl.ensureVerifiedIdentityForEmail()` sets `isUserValidated=true` only after verified token/email checks.

### 2.2 Local credential protection and device binding

- Credentials are persisted encrypted in `credentials.v3` under app private storage.
- Credentials blob is wrapped with Android Keystore AES-GCM key (`com.shary.app.credentials.wrap.v1`), then unwrapped before decrypt.
- A lock marker `credentials.locked` exists for unusable credential states.
- Start routing prefers usable credentials over raw file existence.

Implementation evidence:
- `CryptographyManagerImpl`: wrap/unwrap + `isCredentialsBlobUsable`.
- `FileCredentialsStore`: `hasUsableCredentials`, lock marker set/clear logic.
- `SecurityUtils`: auth file paths + atomic writes.
- `AppNavigator`: start destination selection using `hasUsableCredentials`.

### 2.3 Cloud gating and safe UX behavior

- Cloud action readiness rule:
  - `cloudReady = cloudState.isOnline && cloudState.isUserValidated`
- Cloud options remain visible but disabled with specific reason:
  - `Cloud offline`
  - `Email not verified for cloud`
- Summary screens block cloud send when not ready.
- Field/Request screens block cloud inbox actions when not ready.
- Optional Wi-Fi-only and biometric-gated cloud inbox access exists in settings.

Implementation evidence:
- `CloudUiState.kt`, `SummaryFieldScreen.kt`, `SummaryRequestScreen.kt`, `FieldScreen.kt`, `RequestScreen.kt`.
- Settings flags in `AppSettings` + `SettingsRepositoryImpl`.

### 2.4 End-to-end payload/request transport model

- Sender/recipient identity references are email-hash based.
- Data/request payloads are encrypted client-side with peer public key.
- AAD format used by app encryption/decryption:
  - `shary:{senderHash}:{recipientHash}`
- Payload envelope includes verification + signature fields.

Implementation evidence:
- `CloudServiceImpl.buildLoad()` and `decryptPayloadWithRecipientHash()`.
- `DataPayload` model:
  - `user`, `recipient`, `creation_at`, `expires_at`, `data`, `verification`, `signature`.

### 2.5 Controlled inbox review (fields + requests)

- Cloud downloads are reviewed one-by-one in modal inbox dialogs.
- Actions per item:
  - `Accept`: decrypt + import + backend decision ACK.
  - `Reject`: local dismissal + backend decision ACK.
  - `Cancel`: close dialog, do not import.
- Request decisions try new endpoint first and fallback for compatibility:
  - Primary: `/request_decision`
  - Fallback on 404/405: `/payload_decision`
- Preview and sender-notify are feature-flagged and currently OFF.

Implementation evidence:
- `FieldViewModel` + `CloudInboxReviewDialog`.
- `RequestViewModel` + `RequestInboxReviewDialog`.
- `CloudServiceImpl.sendRequestDecision()` fallback logic.
- `CloudInboxPolicy`: `ENABLE_FIELD_NAME_PREVIEW=false`, `ENABLE_REJECT_NOTIFY_SENDER=false`.

### 2.6 App-side endpoint contract currently expected

Base URL configured in app build config:
- `https://europe-southwest1-shary-21b61.cloudfunctions.net`

Endpoints currently used by app:
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

## 3) Known Backend State (Last memory snapshot, needs live re-verify)

From prior backend hardening work (outside this repo, `shary-backend/firebase`):
- Firestore rules were set to backend-only deny-all (`allow read, write: if false`).
- Bearer verification was tightened with revoked-token checks.
- Scheduler cleanup auth moved from header trust to OIDC verification.
- Fetch/read paths were bounded and filtered by unexpired docs.
- Security-focused test suite replaced stale tests and passed.

Important: treat this as a strong prior state, not guaranteed current-live state. Re-verify against current backend code/deploy before locking final decisions.

## 4) Current Gaps / Ambiguities To Close Before Final Lock

1. Legacy/deprecated code paths still exist in app (compatibility paths), even if not primary UI paths.
2. Decision fallback (`/request_decision` -> `/payload_decision`) can mask backend rollout drift.
3. There is still mixed terminology around anonymous/cloud auth in some code comments and helper names.
4. `StartDestinationResolver` appears stale and not authoritative compared with `AppNavigator` routing logic.
5. Local recovery behavior is partially mixed: lock-marker model exists, but some error paths still purge credentials for recovery.

## 5) Locked Definitive Security + Backend Feature List (Proposed)

These should become the non-negotiable contract.

### A. Identity and auth invariants

1. All identity writes (`register`, `rotate`, `delete`) require verified Firebase token.
2. Token email (canonicalized) must match payload email and derived `user_hash`.
3. Registration is idempotent for same keys; conflicting key binding returns explicit conflict.
4. Rotation requires old-key proof + nonce freshness + audit event.
5. Delete requires signature by current sign key.

### B. Payload/request integrity invariants

1. Backend must verify sender ownership against token-derived user hash.
2. Accept only well-formed hash/signature/verification fields.
3. Enforce TTL bounds (7 default, 14 max) and reject out-of-range values.
4. Fetch only unexpired docs for recipient and enforce max limits.
5. Decision endpoints must be recipient-scoped and idempotent.

### C. Inbox lifecycle invariants

1. Accept/reject decisions must never delete unrelated recipient copies.
2. Decision result should return explicit ack outcome and reason code.
3. Optional sender notification remains feature-flagged until enabled.

### D. Firestore/Firebase security posture

1. Firestore client direct access remains deny-all for app SDK clients.
2. Functions ingress/auth must be least privilege (no spoofable internal headers).
3. Scheduler cleanup must remain OIDC/IAM enforced.
4. No token echo or sensitive token leakage in responses/logs.

### E. Observability and test gates

1. Structured request-id/correlation-id for all auth/identity endpoints.
2. Security test suite covering auth failure matrix and decision lifecycle.
3. Contract tests for every endpoint used by Android app.

## 6) Implementation Plan (Road To Definitive Lock)

### Phase 0: Contract freeze and drift audit (1 short iteration)

Deliverables:
- Freeze endpoint list + request/response schemas in one canonical backend contract doc.
- Mark compatibility endpoints as temporary, with deprecation criteria/date.
- Verify deployed functions match app endpoint map exactly.

Exit criteria:
- No endpoint ambiguity remains.
- App team and backend team agree on single source-of-truth contract.

### Phase 1: Identity hardening completion

Deliverables:
- Enforce all identity invariants listed above.
- Standardize error codes for: unverified, email mismatch, conflict, stale nonce.
- Add audit trail fields for identity writes/rotations/deletions.

Exit criteria:
- Identity abuse scenarios are blocked in tests.
- Android receives deterministic errors mapped to current UI messages.

### Phase 2: Payload/request and inbox decision lock

Deliverables:
- Finalize request and payload decision endpoints with identical ack semantics.
- Add idempotency guarantees for accept/reject.
- Ensure recipient-scoped cleanup only.

Exit criteria:
- Android can remove fallback path after backend reaches parity and stability window.

### Phase 3: Operational robustness and compliance hardening

Deliverables:
- Final IAM/OIDC verification audit.
- Log redaction pass and correlation tracing.
- Load/limit tests for fetch endpoints.

Exit criteria:
- Security regression suite green.
- Clear runbook for incidents (token failures, decision backlog, cleanup jobs).

## 7) Backend Agent Task List (Actionable)

1. Re-verify current deployed backend against section 2.6 endpoint contract.
2. Produce a mismatch report (missing endpoint, wrong schema, weak auth check, drifted Firestore index/rule).
3. Implement Phase 0 and Phase 1 hard requirements.
4. Add/update tests for identity + decision endpoint matrix.
5. Return a lock proposal PR that includes:
   - definitive endpoint schemas,
   - migration/deprecation notes,
   - proof of tests,
   - rollout and rollback notes.

## 8) Quick File Map (Android side)

Auth + local security:
- `app/src/main/java/com/shary/app/infrastructure/security/auth/AuthenticationServiceImpl.kt`
- `app/src/main/java/com/shary/app/infrastructure/security/manager/CryptographyManagerImpl.kt`
- `app/src/main/java/com/shary/app/infrastructure/persistance/credentials/FileCredentialsStore.kt`
- `app/src/main/java/com/shary/app/infrastructure/security/helper/SecurityUtils.kt`

Cloud contract + transport:
- `app/src/main/java/com/shary/app/infrastructure/services/cloud/CloudServiceImpl.kt`
- `app/src/main/java/com/shary/app/infrastructure/services/cloud/Constants.kt`
- `app/src/main/java/com/shary/app/core/domain/interfaces/services/CloudService.kt`
- `app/src/main/java/com/shary/app/core/domain/types/valueobjects/DataPayload.kt`
- `app/src/main/java/com/shary/app/core/domain/types/valueobjects/IdentityV2Payloads.kt`

Cloud gating and inbox UX:
- `app/src/main/java/com/shary/app/ui/utils/CloudUiState.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryFieldScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryRequestScreen.kt`
- `app/src/main/java/com/shary/app/viewmodels/field/FieldViewModel.kt`
- `app/src/main/java/com/shary/app/viewmodels/request/RequestViewModel.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/components/CloudInboxReviewDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/request/utils/RequestInboxReviewDialog.kt`

## 9) Notes for next lock decision meeting

If the backend agent confirms parity for `/request_decision` and stable semantics, remove Android fallback to `/payload_decision` in a follow-up hardening pass and close that migration window explicitly.
