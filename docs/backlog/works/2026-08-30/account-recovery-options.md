# Account Recovery / Erasure Options — Implications & Scoring (2026-08-30)

Decision needed for remediation items 0.5 and 2.4: how to let a user erase (and,
separately, rotate) their cloud identity **without** the old signing key, which
is lost when they forget their password or lose their device.

Scoring legend: 1 (worst) → 9 (best) on **Security**, **UX**, **Compliance**,
**Effort**; plus an overall recommendation.

## The key insight: erase ≠ rotate

- **Erasure (delete identity)** is low-risk to allow with weaker proof — the
  worst case is a denial-of-service on the victim (their identity is deleted),
  not data theft.
- **Rotation (change keys)** is high-risk — an attacker who rotates keys takes
  over the identity and would receive future payloads intended for the victim.

Therefore the two operations should be gated differently.

## Options

### A. Email-verified challenge for both erase and rotate
Send a one-time link/code to the already-verified email; on proof, allow both.

- **Implications:** lowest friction; ties identity control to email custody.
- **Pros:** simple UX, no new secret to store, low effort.
- **Cons:** an attacker who compromises the email can **rotate → take over** the
  identity (full account takeover). Reduces the zero-knowledge posture.
- **Mitigations:** rate-limit + short TTL on the challenge; email-mismatch
  defense already present; optionally require MFA on email.
- **Score:** Security 4 · UX 9 · Compliance 7 · Effort 6.

### B. Offline recovery secret for both erase and rotate
At sign-up, mint a random 128/256-bit secret shown once; user stores it offline.
Required for both erase and rotate.

- **Implications:** strongest; the recovery secret is user-held, not email-held.
- **Pros:** preserves zero-knowledge; email compromise does not enable takeover.
- **Cons:** UX cost (user must store a secret); secret loss re-creates the
  original problem.
- **Mitigations:** clear onboarding guidance; allow "print/store in password
  manager"; optional split into erase-only vs full.
- **Score:** Security 8 · UX 4 · Compliance 8 · Effort 6.

### C. Split gate — email-verified **erase**, offline-secret **rotate** (Recommended)
Erase via email challenge (low risk); rotate via the offline recovery secret
(minted at sign-up).

- **Implications:** the defensible middle ground. Honors Art. 17 cheaply; keeps
  takeover (rotate) protected by a user-held secret.
- **Pros:** erasure easy + compliant; rotation resistant to email compromise;
  minimal UX cost (secret only matters for recovery).
- **Cons:** still requires one recovery secret to be stored; two code paths.
- **Mitigations:** email challenge rate-limited/TTL-bound; secret derivable
  storage via Android Keystore on the current device (auto-fill on-device).
- **Score:** Security 7 · UX 7 · Compliance 9 · Effort 7.

### D. Status quo (no recovery)
Document that lost keys = orphaned identity.

- **Pros:** simplest; no new attack surface.
- **Cons:** cannot honor GDPR Art. 17 when keys are lost; consumer-trust risk;
  a paid service that cannot erase is a liability.
- **Mitigations:** none real; disclosure only.
- **Score:** Security 6 · UX 2 · Compliance 2 · Effort 9.

## Summary

| Option | Security | UX | Compliance | Effort | Verdict |
|---|---|---|---|---|---|
| A — email both | 4 | 9 | 7 | 6 | Reject (rotate takeover risk) |
| B — secret both | 8 | 4 | 8 | 6 | Acceptable |
| **C — email erase + secret rotate** | **7** | **7** | **9** | **7** | **Recommended** |
| D — status quo | 6 | 2 | 2 | 9 | Reject (non-compliant) |

**Recommendation:** implement **C**, with the offline recovery secret derived
and stored device-side (Android Keystore) so everyday users never see it, but
exportable for safekeeping. This is the highest-compliance option that does not
create an email-takeover path for identity rotation.
