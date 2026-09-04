# Fundamentals Tutorial — Key Terms (2026-08-30)

Plain-language explanations of the terms used in the audit and remediation
plan, each tied to how it applies to Shary specifically.

---

## 1. Password-derived identity

**What it is.** Shary has no stored private keys. Instead, the user's identity —
an Ed25519 signing key and an X25519 key-exchange key — is *deterministically
re-derived* every login from `(email, password)`:

```
master seed = PBKDF2-HMAC-SHA256(password, salt = "appId:email", 200_000)
sign seed   = HKDF(master, "shary:id:sign")
kex seed    = HKDF(master, "shary:id:kex")
```

The same password always produces the same keys, on any device. Nothing secret
is written to disk (only public keys are persisted in a "signature file").

**Why it exists.** Zero-knowledge and portability: the company never holds key
material, and a user can rebuild their identity on a new phone using only their
password. This is what makes the "offline vault, no online registration" promise
possible.

**The risk.** The strength of the *entire* identity equals the strength of the
password. If the password is weak, an attacker who obtains any encrypted
ciphertext (and knows the email) can brute-force the KDF offline, recover the
seed, and then (a) sign as the user and (b) decrypt every cloud payload.

**Mitigations.**
- High KDF iteration count (raise 200k → 600k per OWASP 2023).
- Passphrase/entropy policy at sign-up.
- Optionally a hardware-backed key + an offline recovery secret (see the
  account-recovery options note).

---

## 2. Privacy Policy / ToS / ROPA / DPA / DPIA

| Document | Audience | What it is | Source |
|---|---|---|---|
| **Privacy Policy** | End users | Public notice of what data is collected, why, the lawful basis, retention, rights, transfers, and who to contact | GDPR Art. 12–14 |
| **ToS (Terms of Service)** | End users | The contract governing use: paid tiers, withdrawal right, liability, the zero-knowledge key-loss disclosure, termination | LSSI-CE 34/2002; TRLGDCU 1/2007 |
| **ROPA** | Internal / regulator | *Record of Processing Activities* — an internal register listing every processing activity (purpose, data, recipients, retention, safeguards) | GDPR Art. 30 |
| **DPA** | Company ↔ processor | *Data Processing Agreement* with Google/Firebase: binding instructions, security, sub-processors, breach duties | GDPR Art. 28 |
| **DPIA** | Internal / regulator | *Data Protection Impact Assessment* for high-risk processing (biometric gate, MCP automation) | GDPR Art. 35 |

**Why all five matter.** Privacy Policy + ToS are what make collection lawful and
visible to the user; ROPA + DPA + DPIA are what prove compliance to the
supervisory authority (AEPD). Operating without them is not a minor gap — it is
a legal blocker.

---

## 3. Backup of non-transient data

**Transient vs non-transient.** Shary's data splits in two:

- **Transient** (auto-expires, safe to lose): `payloads`, `requests` — encrypted
  ciphertext with a 7–14 day TTL, hard-deleted by the scheduler.
- **Non-transient** (does not expire, irreplaceable): `identities_v2` (public
  keys + email), MCP devices/grants/operations, request decisions, rate-limit
  counters.

**Why backup matters.** Firestore does **not** back itself up. A project
deletion, billing lapse, or admin lockout would destroy `identities_v2`, so
recipients could no longer find a sender's public key and users could not rotate
or delete their identity. For a paid service, "no backup" is a severe
reliability failure even though the relay payloads themselves are transient.

**Mitigation.** Firestore **scheduled backups** and **point-in-time recovery
(PITR)** + a written, *tested* restore runbook (see remediation Phase 1).

---

## 4. Erasure and erasure when keys are lost

**Erasure** = the GDPR Art. 17 *right to erasure* ("right to be forgotten"): a
user can demand deletion of their personal data. Shary must be able to delete
the cloud identity and any stored records.

**The conflict.** Shary's identity operations are authenticated by the Ed25519
signing key that is *derived from the password*:

- `rotate_identity` and `delete_identity` both require a valid signature from
  the current signing key.

If the user **forgets the password or loses the device**, that key is
unrecoverable, so:
- the user cannot sign a delete/rotate, and
- the company cannot act either (zero-knowledge — it has no key material).

Result: the identity is orphaned and Art. 17 cannot be honored. This is the
core tension between "zero-knowledge" and "must be able to erase."

**Mitigation** — an erasure path that does not need the old key (email-verified
challenge, or a user-held offline recovery secret minted at sign-up). See
[[docs/backlog/works/2026-08-30/account-recovery-options.md]].

---

## 7. Non-atomic rate-limit counters and upload dedup

**Atomicity** = an operation either fully happens or does not, with no
interleaving by concurrent requests.

**The two current races (read-then-write is not atomic):**

1. **Rate-limit counter.** The backend does `read count → check limit → write
   count+1`. Two simultaneous requests both read the old count and both pass,
   letting a burst slip slightly over the limit.
2. **Upload dedup.** The backend does `query for duplicate → if none, insert`.
   Two simultaneous identical uploads both see "no duplicate" and both insert,
   creating duplicate documents.

**Why it matters.** These are not catastrophic, but they weaken two safeguards:
abuse control (A4) and idempotency (A3).

**Mitigation.** Firestore **transactions** (atomic read-modify-write) for the
counter, and a transaction or unique key for dedup (see remediation Phase 1,
item 1.5).

---

## Phase terms

### Reliability floor (Phase 1)
The minimum operational work to keep the service trustworthy: backups + PITR,
monitoring/alerting (error, latency, quota-burn, budget), incident/continuity
runbooks, backup billing/ownership, and fixing the atomicity races above.

### Security uplift (Phase 2)
Closing the dominant crypto risk: raise PBKDF2 to 600k (with a migration path),
HKDF the X25519 shared secret, add a passphrase/entropy policy, provide an
account-recovery path, and enable App Check / Play Integrity.

### Monetization (Phase 3)
Turning the business draft into enforced code: a server-side entitlement layer
(tier per identity), free/paid quota enforcement, Google Play Billing, and MCP
usage metering. The rule: **never trust the client** for limits.

### Hardening (Phase 4)
Removing drift and raising trust: delete dead code, reconcile the stale
`main.py` (or point to the backend repo), make the smoke/contract test blocking,
pen-test, responsible-disclosure policy, and ISO 27001/SOC 2 readiness.
