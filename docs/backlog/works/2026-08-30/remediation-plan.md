# Shary Remediation Plan — v1 (2026-08-30)

Goal: raise the SCR score from **4.3 (Fragile)** to a defensible
**7+ (Strong)** and make the app legally and operationally businessable.

Companion: [[docs/backlog/works/2026-08-30/security-compliance-audit.md]].
Business requirements:
[[docs/backlog/works/2026-08-30/business-and-monetization-draft.md]].

> **Guiding principles.** (1) Legal floor first — nothing else matters if you
> cannot operate. (2) Fix the dominant single-point-of-failure (password-derived
> identity) before scaling. (3) Non-transient data must be backed up and
> restorable. (4) Ship low-regression changes first (matches repo policy).

## Stored decisions (2026-08-30)

1. **No Madrid asesor retained yet.** Legal drafts are prepared in-repo as
   working templates and are **not** a substitute for lawyer review before
   publication.
   - `privacy-policy-draft.md`, `terms-of-service-draft.md`, `ropa-draft.md`.
2. **Account recovery / erasure design: option C** (email-verified **erase** +
   offline-secret **rotate**) selected as the recommendation. See
   [[docs/backlog/works/2026-08-30/account-recovery-options.md]].

## Phase 0 — Legal & data-subject floor (blocks launch)

| # | Item | Target | Fixes |
|---|---|---|---|
| 0.1 | Bilingual **Privacy Policy + ToS** (GDPR/LOPDGDD/LSSI/TRLGDCU), incl. 14-day withdrawal + zero-knowledge key-custody disclosure | B1 1→7 | Draft with Madrid asesor; host at a stable URL; link from Settings |
| 0.2 | **DPA** with Google/Firebase; pin Firestore/Functions to **EU region** (`europe-southwest1` already set) | B1 | Formalize processor contract; document data location |
| 0.3 | **ROPA** (GDPR Art. 30) + **breach procedure** (Art. 33/34) + breach register | B1 | Operational artifact |
| 0.4 | **DPIA** for biometric gate + MCP/agent access (Art. 35) | B1 | Document on-device biometric (no template egress) + E2E residual risk |
| 0.5 | **Data-subject rights**: in-app export + account deletion (Play requirement); **fix identity erasure when keys are lost** | B3 3→6 | Add an erasure path that does not require the old signing key (e.g., email-verified challenge or a signed grace token issued at registration and stored under user control) |
| 0.6 | Play Data Safety form + in-app account deletion | B1/B3 | Mandatory for Play listing |

**Phase 0 exit:** B1 ≥ 6, B3 ≥ 5. *Do not launch paid subscriptions before this
phase completes.*

## Phase 1 — Reliability & continuity floor

| # | Item | Target | Fixes |
|---|---|---|---|
| 1.1 | **Firestore scheduled backups + PITR** + a written, **tested restore runbook** | C1 2→8 | Enable in GCP; run a quarterly restore drill; back up `identities_v2` and MCP state (the non-transient collections) |
| 1.2 | **Monitoring/alerting**: error rate, latency, cold starts, auth failures, scheduler failures, **quota-burn**, **budget alerts** | C2 3→7 | Cloud Monitoring/Sentry + billing alerts + a second payment method |
| 1.3 | **Incident/continuity runbooks**: outage, breach, solvent wind-down, insolvency, key/credential compromise (the 5 protocols from the business draft §9) | C4 3→6 | Operationalize the protocols into runbooks + a data-stewardship ToS clause |
| 1.4 | **Backup billing + project ownership** (MFA on admin, backup payment method) | C1 | Eliminates the "billing lapse = service death" failure mode |
| 1.5 | Fix **rate-limit counter atomicity** (Firestore transaction) and **upload-dedup atomicity** (transaction or unique key) | A4 6→7, A3 | Prevent burst over-limit and concurrent duplicates |

**Phase 1 exit:** C1 ≥ 6, C2 ≥ 5, C4 ≥ 5.

## Phase 2 — Security uplift (the dominant risk)

| # | Item | Target | Fixes |
|---|---|---|---|
| 2.1 | **Raise PBKDF2 to 600k iterations** with a **key-derivation version + migration path** (re-derive on next login; never silently break existing vaults) | A1 5→7 | OWASP 2023-aligned; version the KDF params and store the version in the credential blob |
| 2.2 | **HKDF the X25519 shared secret** before AES-GCM (proper key separation) | A1 | Small, low-regression |
| 2.3 | **Entropy guard + passphrase policy** on sign-up (reject weak passwords; recommend passphrases) | A1/A2 | Raises the practical bar of the password-derived identity |
| 2.4 | **Account recovery** design: allow identity erasure and (optionally) key rotation via an offline recovery secret chosen at registration | A2 5→6, B3 | Complements 0.5; preserve zero-knowledge (recovery secret held by user) |
| 2.5 | **App Check / Play Integrity** on relay + MCP endpoints | A4 6→7 | Reduce abuse from non-app clients |

**Phase 2 exit:** A1 ≥ 7, A2 ≥ 6.

## Phase 3 — Monetization enforcement

| # | Item | Target | Fixes |
|---|---|---|---|
| 3.1 | **Server-side entitlement layer**: tier on the identity/custom-claims; enforce free/paid quotas (sends, expiry, inbox, storage) in `functions/main.py` | B4 1→7 | Never trust client for limits |
| 3.2 | **Play Billing** integration (subscriptions) + entitlement sync to backend | B4 | Google Play Billing requirement |
| 3.3 | **MCP metering**: tier-based call/value-read quotas on top of existing rate limits, with usage-based overage | B4 | Reuse `rate_limit` module; add tier dimension |

**Phase 3 exit:** B4 ≥ 7 (the business draft's free/paid limits are now
enforced, not aspirational).

## Phase 4 — Hardening & hygiene

| # | Item | Target | Fixes |
|---|---|---|---|
| 4.1 | Remove dead in-memory auth simulation; **reconcile the stale `main.py`** in the Android repo or delete it (point to backend repo as source of truth) | C5 4→6 | Eliminates contract-drift risk |
| 4.2 | Make the smoke/contract test **blocking** on protected branches | C3 5→6 | Freeze brief's test-gate requirement |
| 4.3 | Pen test + responsible-disclosure policy | A | Independent verification |
| 4.4 | ISO 27001 / SOC 2 readiness (B2B/MCP enterprise) | B1 | Later; prerequisite for enterprise |
| 4.5 | Vendor-lock-in mitigation: document export format + migration path behind the `CloudService` interface | C5 | Continuity |

## 5. What is near-certain (evidence-based judgments)

- **You cannot legally charge EU users until Phase 0 completes.** The absence of
  a Privacy Policy/ToS/ROPA/DPA is a hard blocker, not a preference.
- **The single dominant technical risk is the password-derived identity** (A1/A2):
  it is the root of both local vault security and cloud identity, and it is only
  as strong as the user's passphrase. Fix it (2.1–2.4) before scaling.
- **Non-transient data has no backup today** (C1): `identities_v2` and MCP state
  are irrecoverable on a project/billing failure. This is the highest-reliability
  risk to a paid service.
- **The hard part (crypto + backend contract) is already strong** (A3/A5/B2 ≈ 7–8).
  Most remaining work is operational and compliance, not algorithm replacement.

## 6. Sequencing summary

```
Phase 0 (legal floor) ──► Phase 1 (reliability floor) ──► Phase 2 (security uplift)
        │                          │                            │
        └── launch gate ───────────┴── paid-service gate ───────┴── scale gate
Phase 3 (monetization) runs in parallel with Phase 1–2; Phase 4 is continuous.
```

Lowest-regression changes first: 1.5, 2.2, 4.1 are small and safe; 0.x and 1.1
are blockers; 2.1/2.4 and 3.1 are the highest-value design decisions to confirm
with the owner before implementing.
