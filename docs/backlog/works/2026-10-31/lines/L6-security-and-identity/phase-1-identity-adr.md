# L6 Phase 1 — Identity Architecture Decision Record

Status: Proposed (needs founder approval and independent review)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L6 — Security & Identity](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. Context

Shary derives the cloud identity from the user's password. The same password
also protects the local vault. This is the dominant security risk in the
2026-08-30 audit: a weak or reused password can be brute-forced offline, and a
lost password or device orphans the cloud identity.

The decision must preserve the offline-first, zero-knowledge product while
making erasure possible under GDPR Art. 17.

## 2. Current implementation (code-verified)

| Step | Current behavior | Evidence |
|---|---|---|
| Master secret | PBKDF2-HMAC-SHA256, 200,000 iterations, salt `appId:email`, 32 bytes | `Kdf.kt`, `KeyDerivation.kt`, `Pbkdf2Kdf.kt` |
| Identity seeds | HKDF-SHA256 expands the master into `shary:id:sign` and `shary:id:kex` | `KeyDerivation.kt` |
| Identity keys | Ed25519 signing key and X25519 key-exchange key from those seeds | `CryptographyManagerImpl.kt` |
| Local vault keys | Per-purpose HKDF expansion `shary:local:<purpose>` | `LocalVault.kt` |
| Login | Derive identity, request a backend challenge, sign with Ed25519 | `LoginUseCase.kt` |
| Registration | Derive identity, upload the public keys | `RegisterUseCase.kt` |
| Server record | `user_hash = SHA256(canonical email)`, public keys, key version, timestamps | `main.py` (`_v2_register_identity`) |
| Rotation | Challenge nonce plus signatures from the old and new keys | `main.py` (`_v2_rotate_identity`) |
| Deletion | Signature from the current key | `main.py` (`_v2_delete_identity`) |
| Credentials blob | Inner AES-GCM with a password-derived key; outer AES-GCM with an Android Keystore key | `AuthenticationServiceImpl.kt`, `CryptographyManagerImpl.kt` |
| ECDH envelope | X25519 shared secret used directly as the AES-GCM key | `CryptographyManagerImpl.kt` (`encryptWithPeerPublic`) |
| Android backup (before 2026-11-01) | `allowBackup="true"`; only MCP/connector data excluded | `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` |

Consequences:

- The server never sees the password or private keys, but it can identify the
  account by the email hash.
- A leaked credentials blob or ciphertext can be attacked offline against the
  password.
- 200k PBKDF2 is below the current OWASP guidance of 600k for PBKDF2-HMAC-SHA256.
- There is no KDF version in the credential blob, so raising the iteration count
  needs a migration design.
- Rotation and deletion both require the old signing key. A lost device or
  forgotten password makes erasure impossible.
- The X25519 shared secret is not passed through HKDF before AES-GCM.
- Android backup can copy encrypted vault data and plaintext contacts/settings.

## 3. Threat model

| Threat | Impact | Current mitigation | Residual risk |
|---|---|---|---|
| Offline password guessing | Identity takeover and vault decryption | PBKDF2 200k, device sandbox | High: weak/reused passwords |
| Credential stuffing | Account takeover | Firebase Auth, verified email | Medium |
| Lost device/password | Orphaned identity; no erasure | None | High: GDPR Art. 17 gap |
| Email compromise | Erase/rotate abuse | None for recovery | High if email-only recovery is used |
| Malicious MCP token | Unauthorized reads/writes | OAuth scopes, grants, approval-gated writes | Medium |
| Local backup exfiltration | Plaintext contacts/settings; ciphertext | App sandbox | High: backup enabled |
| Clipboard leakage | Field/contact disclosure | Sensitive flag, auto-clear | Medium |
| Server compromise | Metadata, ciphertext, public keys | E2E, deny-all rules | Medium |
| Dependency/supply chain | Code execution | Minimal dependencies | Medium |
| Abuse/DoS | Service degradation | Rate limits (non-atomic) | Medium |

## 4. Options

### Option A — Minimal hardening

Raise PBKDF2 to 600k, add a KDF version, add a passphrase policy, and HKDF the
X25519 shared secret.

- Effort: medium.
- Security: improves offline guessing resistance and key separation.
- Compliance: does not solve erasure when keys are lost.
- UX: unchanged.
- Verdict: necessary, not sufficient.

### Option B — Split recovery (recommended)

Option A plus:

- an offline recovery secret or recovery keypair created at registration;
- email-verified erase for the cloud identity;
- recovery-secret-authorized rotation;
- UI and policy disclosure.

- Effort: medium-high.
- Security: erasure is low-risk; rotation remains protected by a user-held
  secret, so email compromise alone cannot take over the identity.
- Compliance: meets GDPR Art. 17 and the audit's B3 target.
- UX: one recovery secret to store; everyday users can keep it in a password
  manager or print it.
- Verdict: **recommended for launch.**

### Option C — Device-generated identity keys

Generate random Ed25519/X25519 keys on the device, wrap them with the Android
Keystore and a password-derived key, and store an encrypted key blob for
multi-device restore. Add an offline recovery secret.

- Effort: high.
- Security: removes the password-derived identity key; stronger against offline
  guessing, but the encrypted key blob is still password-protected.
- Compliance: good, if erasure and recovery are designed.
- UX: multi-device restore and migration complexity.
- Verdict: defer to a post-launch spike; it is a larger identity migration.

### Option D — Passkey/WebAuthn

Use a platform passkey as the primary credential.

- Effort: high.
- Security: strong phishing resistance.
- UX: platform-dependent; recovery and multi-device flows need design.
- Verdict: not now.

## 5. Decision

Adopt **Option B** for launch:

1. Raise PBKDF2 to 600k for new registrations, with a KDF version and an
   on-login migration for existing users.
2. HKDF the X25519 shared secret before AES-GCM, with an envelope version.
3. Add a passphrase policy and a weak-password warning.
4. Add an offline recovery secret/keypair at registration.
5. Add email-verified cloud-identity erase that does not require the old key.
6. Add recovery-secret-authorized rotation, while keeping the old-key path for
   normal rotation.
7. Disable Android cloud backup/device transfer for all Shary data, or exclude
   every Shary-owned file; add an explicit encrypted export/import path.
   **Applied 2026-11-01:** `allowBackup="false"`; backup and data-extraction
   rules exclude every app domain; `BackupPolicyTest` guards the configuration.
   **Applied 2026-11-02:** encrypted export/import for fields, contacts,
   requests, tags, and safe settings.
8. Update the Privacy Policy, ToS, and ROPA to match.

Option C remains a post-launch spike, gated on a migration design and a
multi-device recovery design.

## 6. Why Option B

- It is the lowest-regression path that closes the GDPR erasure gap.
- It preserves the zero-knowledge story: the recovery secret is user-held, not
  email-held.
- It aligns with remediation items 2.1-2.4 and the selected option C recovery
  design from 2026-08-30.
- It fits a single-founder launch better than a full identity migration.
- It raises the SCR target without pretending the password-derived risk is gone.

Residual risk: the identity key is still password-derived. A weak password
remains exploitable offline. The passphrase policy and 600k iterations reduce
the probability but do not eliminate it. This must be stated honestly in the
security posture and the trust page.

## 7. Implementation outline

### 7.1 KDF versioning and migration

1. Extend the credential blob with `kdf_version`, `kdf_iterations`, and
   `kdf_algorithm`.
2. New registrations use 600k.
3. On successful login with an older version, re-derive at 600k and rewrap the
   credentials blob and local data keys.
4. Never silently break an existing vault; keep the old derivation path until
   the migration succeeds.

### 7.2 HKDF for ECDH

1. Add an envelope version to the sealed payload.
2. Derive the AES key as `HKDF(shared_secret, info="shary:envelope:v2")`.
3. Decrypt old envelopes with the legacy direct-key path.
4. Test cross-version send/receive.

### 7.3 Recovery secret

1. Generate a 256-bit recovery secret at registration.
2. Store a recovery public key or a salted verifier server-side; never store the
   plaintext secret.
3. Show the secret once; require the user to confirm they stored it.
4. Allow the user to regenerate it with the current password and old secret.
5. Use it only for rotation, not for reading vault content.

### 7.4 Email-verified erase

1. Add `/v2/identity/erase` requiring a Firebase ID token with a verified email.
2. Rate-limit and log the request with a hashed actor key.
3. Delete `identities_v2`, MCP grants, devices, and agent profiles tied to the
   owner; schedule dependent records for deletion.
4. Retain only the minimum legal/security record, pseudonymized, with a defined
   period.
5. Return a confirmation and record the completion evidence.

### 7.5 Android backup

1. Decision: disable backup for all Shary data.
2. Applied: `allowBackup="false"`; `backup_rules.xml` and
   `data_extraction_rules.xml` exclude root, file, database, sharedpref, and
   external domains; `BackupPolicyTest` asserts the policy.
3. Applied: explicit encrypted export/import for fields, contacts, requests,
   tags, and safe settings. Credentials and MCP state are excluded.
4. Remaining: update the Privacy Policy, Data Safety answers, and L5 continuity
   plan to state that OS backup is disabled and the export passphrase is
   unrecoverable.

### 7.6 Tests

- KDF migration from 200k to 600k.
- Old and new envelope interoperability.
- Recovery-secret rotation and replay rejection.
- Email-verified erase with and without the old key.
- Erase deletes MCP grants/devices and leaves no orphaned records.
- Android backup configuration check.
- Rate-limit and concurrency tests for erase/rotate.

## 8. Consequences

Positive:

- GDPR Art. 17 can be honored when keys are lost.
- Rotation no longer depends on email alone.
- Offline guessing is harder.
- The security claims can be precise.

Negative:

- Users must store a recovery secret.
- Two rotation paths and a migration increase code complexity.
- Android backup changes may surprise users; the explicit export path is now
  available but needs manual device verification.
- The residual password-derived risk remains.

## 9. Review procurement

The reviewer scope and RFP are in
[phase-1-independent-review-rfp.md](phase-1-independent-review-rfp.md). Do not
send or book until the L6 implementation is testable.

## 10. Open questions

1. Is PBKDF2 600k enough, or should the app move to Argon2id? Android support
   and library risk need review.
2. Should the recovery secret be a keypair or a symmetric verifier?
3. How is the recovery secret stored on the device without weakening it?
4. Does email-verified erase need a cooling-off period to prevent malicious
   deletion?
5. Should erase also delete the Firebase Auth user, or only the Shary identity?
6. What legal/security records must be retained after erase, and for how long?
7. Is disabling Android backup acceptable for users who expect device restore?
8. Does the app need a multi-device story before launch?

## 11. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L6-V1 Identity ADR | This document | Proposed |
| L6-V2 Auth hardened | Not implemented | Pending |
| L6-V3 Erasure when keys lost | Option B selected | Pending |
| L6-V4 Key lifecycle | Not implemented | Pending |
| L6-V5 Rate-limit/dedup atomicity | Not implemented | Pending |
| L6-V6 App Check | Not implemented | Pending |
| L6-V7 Least privilege/secrets | Partial | Pending |
| L6-V8 Independent review | Not scheduled | Pending |
| L6-V9 SCR re-audit | Not started | Pending |
| L6-V10 MCP revocation | Partial | Pending |
| L6-V11 Android backup decision | Section 7.5; `BackupPolicyTest`; export/import tests | Applied; export implemented |
