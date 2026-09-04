# Email Verification Options and Robust Identity Binding

Short answer: in your current architecture, impersonation is not just an edge case, it is realistically possible.

Why in current app:
- You use Firebase anonymous auth for backend access, but user identity in your crypto/cloud flow is based on app-provided email strings.
- If backend accepts `emailHash + pubkey` without a verified email claim, an attacker can register another person’s email hash first and bind their own key.
- Then downloads/decrypt flows can fail or be hijacked (exactly the kind of mismatch you saw).

## Options

| Case | Feasibility | Simplicity | Cost-effectiveness | Difficulty | Notes |
|---|---|---:|---:|---:|---|
| 1) Firebase + anonymous only | Low (for true email ownership) | High | High | Low | Anonymous users have no verified email claim. You cannot robustly prove mailbox ownership in pure anonymous mode. |
| 2) Firebase non-anonymous (Email/Password or Email Link) | High | High | High | Low-Medium | Best Firebase-native path. Send verification email and enforce `email_verified == true` before allowing key registration/upload. |
| 3) Non-Firebase alternative for “anonymous-like” flow | Medium | Medium | Medium | Medium | Use your own backend + email OTP/magic link provider (SendGrid/Postmark/SES+custom token). User starts pseudonymous, then must complete email challenge before key binding. |
| 4) Non-Firebase alternative for non-anonymous flow | High | Medium | Medium | Medium | Use Auth0 / Clerk / Supabase Auth / Cognito with verified-email claims, then bind crypto keys only after verified claim. |

## Recommended robust design

1. Require verified email proof before first `uploadUser` key binding.
2. Backend must validate auth token claim for that exact email.
3. Store immutable binding: `verified_email_hash -> current_pubkey` with rotation rules.
4. Reject conflicting first-write attempts for same verified email hash.
5. Require signed key-rotation challenge (old key signs nonce) to prevent takeover.

---

## Backend Contract Draft (for your current architecture)

### Security goals
- Bind one verified mailbox to one active crypto identity (KEX key) at a time.
- Prevent third-party pre-registration and key hijacking.
- Allow safe key rotation without account takeover.
- Keep existing payload/request endpoints mostly unchanged.

### Identity model
- Canonical email: `email_canonical = trim(lowercase(email))`.
- User id in DB: `user_hash = base64(SHA-256(email_canonical))`.
- Auth source:
  - Firebase path: backend verifies Firebase ID token and reads `email`, `email_verified`, `sub`.
  - Non-Firebase path: backend verifies provider JWT/session and reads equivalent verified-email claim.

### Core invariants
- `email_verified` must be true for register/rotate/delete identity operations.
- `token.email` (canonicalized) must match request email used to derive `user_hash`.
- First binding of `user_hash` is exclusive.
- Any key change requires proof of possession of previous key (or privileged recovery flow).

## 1) Register Identity

### Endpoint
- `POST /v2/identity/register`

### Auth
- Bearer token (Firebase or alternative), verified server-side.

### Request body
```json
{
  "email": "user@example.com",
  "pub_kex_b64": "...",
  "pub_sign_b64": "...",
  "client_ts": 1710000000,
  "proof": {
    "verification_b64": "base64(sha256(canonical_fields))",
    "signature_b64": "ed25519_sign(verification)"
  }
}
```

Canonical fields for proof (exact order):
- `"register" + "." + user_hash + "." + pub_kex_b64 + "." + pub_sign_b64 + "." + client_ts`

### Server checks
1. Verify bearer token and extract verified email claim.
2. Canonicalize token email and body email, ensure equality.
3. Compute `user_hash` from canonical email.
4. Verify signature with `pub_sign_b64`.
5. If `user_hash` not registered: create identity.
6. If already registered with same keys: idempotent success.
7. If already registered with different keys: return conflict requiring rotate flow.

### Responses
- `201 Created` new binding.
- `200 OK` idempotent.
- `409 Conflict` identity already bound to different keys.
- `401/403` auth or email_verified failure.

## 2) Rotate Identity Key

### Endpoint
- `POST /v2/identity/rotate`

### Auth
- Verified bearer token.

### Request body
```json
{
  "email": "user@example.com",
  "new_pub_kex_b64": "...",
  "new_pub_sign_b64": "...",
  "nonce": "server-issued-nonce",
  "old_key_signature_b64": "sig_by_old_sign_key",
  "new_key_signature_b64": "sig_by_new_sign_key",
  "client_ts": 1710000100
}
```

Signatures are over canonical string:
- `"rotate" + "." + user_hash + "." + new_pub_kex_b64 + "." + new_pub_sign_b64 + "." + nonce + "." + client_ts`

### Server checks
1. Auth + verified email checks as above.
2. Fetch current registered `pub_sign` for `user_hash`.
3. Verify `old_key_signature_b64` with current sign key.
4. Verify `new_key_signature_b64` with new sign key.
5. Enforce nonce freshness and one-time use.
6. Update keys atomically; write audit trail.

### Responses
- `200 OK` rotated.
- `409 Conflict` stale nonce or race.
- `401/403` auth failure.

## 3) Get Public Key

### Endpoint
- `GET /v2/identity/pubkey?user_hash=...`

### Behavior
- Return active `pub_kex_b64` plus key version and optional fingerprint.

### Response
```json
{
  "user_hash": "...",
  "pub_kex_b64": "...",
  "key_version": 3,
  "updated_at": 1710000200
}
```

## 4) Delete Identity

### Endpoint
- `POST /v2/identity/delete`

### Request body
```json
{
  "email": "user@example.com",
  "client_ts": 1710000300,
  "signature_b64": "sig_by_current_sign_key"
}
```

Signed canonical string:
- `"delete" + "." + user_hash + "." + client_ts`

### Server checks
- Auth + verified email + signature with current sign key.

## 5) Payload/Request Upload Guardrails

For existing upload endpoints:
- Keep `user` and `recipient` as hashes.
- Validate `user` equals hash of authenticated verified email (uploader cannot spoof sender).
- Keep AAD convention `"shary:{senderHash}:{recipientHash}"`.
- Optionally require `key_version` for sender to support future rotation handling.

## 6) Suggested migration plan

1. Add `v2` identity endpoints while keeping old ones read-compatible.
2. On first verified login, client calls `/v2/identity/register` with canonical email-derived keys.
3. Backend marks legacy entries and prefers v2 record on lookup.
4. For users with conflicting old bindings, require explicit rotate/recovery process.
5. After adoption window, reject non-verified legacy register calls.

## 7) Client-side requirements (Android)

- Always canonicalize email before:
  - `hashPassword(...)`
  - `initializeKeysWithUser(...)`
  - cloud identity registration and lookup hash generation.
- Block `uploadUser` until auth provider reports verified email.
- On `409 Conflict` during register, call rotate flow (with nonce challenge).
- Keep backward-compatible fetch hash candidates only during migration window.

## 8) Why this closes impersonation risk

- Third party cannot bind keys to someone else’s mailbox without controlling a verified auth session for that mailbox.
- Even with token theft window, long-term takeover is harder because key rotation requires old-key possession or explicit recovery.
- Sender spoofing on data upload is blocked by server-side sender hash validation against verified identity.
