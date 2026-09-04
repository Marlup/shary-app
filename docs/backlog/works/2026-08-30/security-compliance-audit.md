# Shary Security & Compliance Audit — v1 (2026-08-30)

Status: Assessment. Companion to
[[docs/backlog/works/2026-08-30/business-and-monetization-draft.md]].
Remediation plan: [[docs/backlog/works/2026-08-30/remediation-plan.md]].

> **Method.** Evidence-based audit of the Android repo (`sharydemo`) and the
> Firebase backend (`shary-backend`) against the requirements derived from the
> business draft. Scores are honest and unforgiving; a "not started" item scores
> as severely as it deserves, not charitably.

## 1. Scoring scale — Security & Compliance Readiness (SCR)

| Score | Label | Meaning |
|---|---|---|
| 1 | **Critical** | Actively unsafe / legally non-compliant / absent |
| 2 | **Severe** | Major gap, high real risk |
| 3 | **Deficient** | Significant gap, partial coverage |
| 4 | **Fragile** | Works but brittle / drifted |
| 5 | **Moderate** | Acceptable baseline, known weaknesses |
| 6 | **Adequate** | Solid baseline + partial hardening |
| 7 | **Strong** | Well-hardened, evidence-backed |
| 8 | **Hardened** | Defense-in-depth, enforced |
| 9 | **Robust** | Exemplary / audit-ready |

## 2. Scorecard

### Pillar A — Security

| # | Dimension | Score | Status | Evidence | Key risk |
|---|---|---|---|---|---|
| A1 | Cryptography & key management | **5 Moderate** | Present | AES-256-GCM w/ random 96-bit IV (`AesGcmCipher`); PBKDF2-HMAC-SHA256 at **200k** iterations (`Kdf.kt`); Ed25519 sign + X25519 kex; Android Keystore AES-GCM wraps credential blob (`CryptographyManagerImpl`) | Master secret is **password-derived and password-strength-bound**; identity keys are not hardware-backed; PBKDF2 200k is below current OWASP guidance (600k); X25519 shared secret used directly as AES key (no HKDF) |
| A2 | Authentication & identity lifecycle | **5 Moderate** | Present | Verified-email enforced server-side with `check_revoked=True`; anonymous session + email identity; key rotation/deletion endpoints | **No recovery/erasure if keys are lost** — rotate and delete both require the old-key signature; a forgotten password orphans the identity (see B3) |
| A3 | Server-side authorization & input validation | **7 Strong** | Hardened | Firestore rules deny-all (`allow if false`); strict base64 (`validate=True`), hash-format, signature-size, 128KB payload cap, 5-min clock skew, 14-day TTL validation; canonical digest + Ed25519 verification against sender's registered key (`functions/main.py`) | Upload dedup is non-atomic (query-then-set) — concurrent duplicates possible |
| A4 | Abuse controls / rate limiting | **6 Adequate** | Present | `rate_limits_v1` collection; per-user + per-IP keys; per-endpoint limits (upload 60/min, fetch 120/min, decision 60/min) | Counter read-modify-write is **non-atomic** (burst window); no Firebase App Check / Play Integrity |
| A5 | MCP / agent gateway security | **7 Strong** | Hardened | OAuth 2.0 + PKCE S256, scopes, revocation/refresh; KMS envelope encryption w/ 90-day key rotation; token-signing secret in Firebase secrets; CSP/nosniff/Referrer-Policy headers; browser-origin rejection; per-IP 240/min, per-agent 120/min, per-owner 180/min | Token-signing key rotation is manual; no automated key-rotation audit |

**Pillar A mean: 6.0 (Adequate).**

### Pillar B — Compliance & Legal

| # | Dimension | Score | Status | Evidence | Key risk |
|---|---|---|---|---|---|
| B1 | Privacy/compliance artifacts (policy, ToS, ROPA, DPA, DPIA) | **1 Critical** | Absent | No Privacy Policy, ToS, ROPA, DPA, DPIA, or consent artifacts found in either repo | **Illegal to operate commercially in the EU** without these (GDPR/LOPDGDD/LSSI/TRLGDCU); AEPD sanctions up to €20M or 4% turnover |
| B2 | Data minimization & TTL enforcement | **8 Hardened** | Enforced | E2E ciphertext only; 7–14-day TTL; hard-delete scheduler (`clean_expired_docs`) covering payloads, requests, decisions, nonces, rate-limit counters | Minor: reliance on scheduler cadence; no proof of TTL field config in `firebase.json` (configured via console) |
| B3 | Data-subject rights (access/erasure/portability) | **3 Deficient** | Partial | Local vault is exportable on-device; local delete exists | **Cloud identity cannot be erased when keys are lost** (GDPR Art. 17 breach); no self-service export/delete surface; no account-deletion in-app (Google Play requirement) |
| B4 | Monetization / tier enforcement | **1 Critical** | Not started | No subscription, no tier quotas, no entitlement enforcement anywhere (only global rate limits, not tier-based) | Free/paid limits from the business draft are unimplemented; no revenue path enforceable |

**Pillar B mean: 3.25 (Deficient).**

### Pillar C — Reliability & Continuity

| # | Dimension | Score | Status | Evidence | Key risk |
|---|---|---|---|---|---|
| C1 | Backups & DR | **2 Severe** | Absent | No Firestore scheduled-backup/PITR config or restore runbook; `OPERATIONS_RUNBOOK.md` covers TTL/KMS/functions/secrets but **no backup/restore** | `identities_v2`, MCP state, and decisions are **not transient** and are not backed up; project/billing loss = unrecoverable identities |
| C2 | Monitoring & alerting | **3 Deficient** | Partial | Structured `event=` logs, `X-Request-Id`, `mcp_health` endpoint | No automated alerting, budget alerts, quota-burn alerts, or uptime monitoring evidenced |
| C3 | Testing & CI gates | **5 Moderate** | Partial | Backend `pytest` CI + manual smoke contract workflow; Android 51 JVM + 21 device test source files; lint | Smoke gate is manual-dispatch, not blocking; freeze brief's "contract test gate on protected branches" not evidenced as enforced |
| C4 | Continuity / incident protocols | **3 Deficient** | Partial | MCP kill-switch + secret/KMS rotation documented | No outage, breach, wind-down, or insolvency runbooks; no data-stewardship clause (business draft §9) |
| C5 | Codebase health & repo drift | **4 Fragile** | Drifted | Well-layered architecture; good docs | Android repo's `main.py` is **stale** vs the real backend (missing decision endpoints, rate limiting, scheduler OIDC, input validation); dead in-memory auth simulation remains |

**Pillar C mean: 3.4 (Deficient).**

## 3. Overall score

| Pillar | Mean |
|---|---|
| Security | 6.0 Adequate |
| Compliance & Legal | 3.25 Deficient |
| Reliability & Continuity | 3.4 Deficient |
| **Overall (balanced)** | **4.3 / 9 — Fragile** |

**Interpretation:** the cryptography and backend contract are genuinely strong
(the hard part is done well); the app is *not ready to be a business* because the
legal floor, data-subject rights, backups, monitoring, incident protocols, and
monetization enforcement are absent or partial. The gap is operational and
compliance, not core crypto.

## 4. Top findings (ranked by risk)

1. **Password-derived identity = total account compromise.** The Ed25519
   signing key and X25519 key-exchange key derive from `PBKDF2(password)`. A
   weak passphrase, once brute-forced offline against any leaked ciphertext,
   yields the full identity: impersonation + decryption of all cloud payloads.
   Compounded by PBKDF2 at 200k (below OWASP's 600k) and no hardware-backed key.
2. **No privacy/compliance artifacts (B1 = Critical).** Cannot operate
   commercially in Spain/EU. This alone blocks launch.
3. **No backup of non-transient data (C1 = Severe).** `identities_v2`, MCP
   grants/operations, and decisions are not backed up. A project/billing loss is
   unrecoverable and would strand users' identities.
4. **No erasure when keys are lost (B3).** GDPR Art. 17 cannot be honored for
   cloud identity once the signing key is gone (rotate/delete both require it).
5. **No tier/entitlement enforcement (B4).** The entire monetization strategy
   from the business draft has no code to enforce it.
6. **No monitoring/alerting or incident/continuity runbooks (C2, C4).**
7. **Repo drift (C5).** The stale `main.py` in the Android repo will mislead a
   future implementer into building against a weaker contract.
8. **Rate-limit and upload-dedup atomicity (A4/A3).** Non-atomic counters and
   duplicate checks allow small bursts/duplicates under concurrency.

## 5. Requirements traceability (business draft vs codebase)

| Draft requirement | State |
|---|---|
| Freemium subscription + MCP usage add-on | Not started (B4) |
| Hard-frozen per-tier limits (15 sends/7d free, etc.) | Not started (B4) |
| E2E encryption always on; no plaintext at server | **Met** (A3, B2) |
| TTL hard-delete 7–14 days | **Met** (B2) |
| Privacy Policy/ToS/ROPA/DPA/DPIA | **Missing** (B1) |
| Data-subject rights (export/erase) | Partial (B3) |
| Backups + tested restore | **Missing** (C1) |
| Monitoring/alerting + incident runbooks | Partial (C2, C4) |
| Abuse controls / rate limiting | Present, minor gaps (A4) |
| Continuity/termination protocols (5) | Partial (C4) |
