# L6 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Automated criteria require a named test or CI job. The independent review
requires a written report. The re-audit requires a scorecard. A security claim
without a test or external report does not pass.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L6-V1 | Identity ADR is approved | Document review | Options, threat model, decision, and residual risk are recorded | EV-L6-01 | OWASP ASVS; NIST 800-63B |
| L6-V2 | Authentication is hardened | Automated test + review | Password-derived identity risk is addressed; rate limiting and lockout are tested | EV-L6-02 | NIST 800-63B |
| L6-V3 | Erasure when keys are lost works | Automated test + drill | Email-verified erase plus offline-secret rotate succeeds end to end | EV-L6-03 | GDPR Art. 17; option C note |
| L6-V4 | Key lifecycle is complete | Automated test | Identity and MCP keys rotate and revoke; old grants stop working | EV-L6-04 | Google Cloud IAM; OWASP |
| L6-V5 | Rate limits and dedup are atomic | Concurrency test | Concurrent requests cannot exceed limits or duplicate an operation | EV-L6-05 | Backend contract |
| L6-V6 | App Check and abuse controls are active | Automated test | Requests without valid attestation are rejected where required | EV-L6-06 | Firebase App Check docs |
| L6-V7 | Least privilege and secrets are clean | Review | IAM roles are minimal; no secrets in repo; rotation procedure works | EV-L6-07 | Google Cloud IAM; repo policy |
| L6-V8 | Independent review found no critical issue | External review | Report has no open critical finding; high findings have closure dates | EV-L6-08 | OWASP ASVS |
| L6-V9 | SCR re-audit reaches at least 7 | Scorecard | Overall score is at least 7 with evidence for each pillar | EV-L6-09 | 2026-08-30 audit scale |
| L6-V10 | MCP grant revocation is proven | Automated test | Revoked grants cannot read or write; audit record is retained | EV-L6-10 | Backend contract; GDPR Art. 32 |
| L6-V11 | Android backup policy is decided and applied | Document review + configuration check | Manifest and backup rules match the decision; plaintext data is not backed up unless explicitly accepted | EV-L6-11 | Android backup docs; GDPR Art. 32 |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L6-T1 | Automated test | Attempt authentication with a leaked password and repeated attempts | Lockout/rate limit triggers; no account enumeration |
| L6-T2 | Automated test | Erase an identity whose device keys are lost | Email verification succeeds; data erased; offline secret rotated |
| L6-T3 | Automated test | Rotate an identity key and revoke an MCP grant | Old key/grant fails; new key works |
| L6-T4 | Concurrency test | Fire 100 parallel share requests at the quota boundary | Quota holds; no duplicate delivery |
| L6-T5 | Automated test | Call the API without App Check attestation | Request rejected where required |
| L6-T6 | Review | Inspect IAM roles, secret storage, and rotation runbook | Least privilege confirmed; no secrets in repo |
| L6-T7 | External review | Reviewer tests identity, erasure, MCP authorization, and rate limits | Report with severity and reproduction steps |
| L6-T8 | Scorecard | Re-run the SCR audit with the new evidence | Score at least 7 |
| L6-T9 | Configuration review | Compare `AndroidManifest.xml` and backup rules with the SC13 decision | No unexpected plaintext or ciphertext backup path |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L6-01 | Identity ADR and threat model | External secure folder | Founder | Pending | On architecture change |
| EV-L6-02 | Authentication test results | CI plus external folder | Founder | Pending | Each release |
| EV-L6-03 | Erasure E2E test record | External secure folder | Founder | Pending | Each release |
| EV-L6-04 | Rotation/revocation test record | CI plus external folder | Founder | Pending | Each release |
| EV-L6-05 | Concurrency test results | CI plus external folder | Founder | Pending | Each release |
| EV-L6-06 | App Check configuration and test | External secure folder | Founder | Pending | Quarterly |
| EV-L6-07 | IAM and secrets review | External secure folder | Founder | Pending | Quarterly |
| EV-L6-08 | Independent review report and closure | External secure folder | Founder | Pending | Annual |
| EV-L6-09 | SCR re-audit scorecard | External secure folder | Founder | Pending | Annual |
| EV-L6-10 | MCP revocation test record | CI plus external folder | Founder | Pending | Each release |
| EV-L6-11 | Android backup decision and config evidence | External secure folder | Founder | Pending | On manifest change |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L6-01 to C-L6-11 appear only here |
| LIS-2 Boundary integrity | Pending | Policy text, store disclosure, backups, and pricing are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T5 are automated; T6 and T8 are reviews; T7 is external; T9 is a configuration review |
| LIS-5 Operability | Pending | Founder owns; independent reviewer and trustee named |
| LIS-6 Public-claim consistency | Pending | Zero-knowledge, deletion, and backup claims must match the implementation |
| LIS-7 Independence | Pending | SC2, SC5, and SC7 are consumed; SC13 is provided |
| LIS-8 Regression | Pending | Any identity or deletion change triggers L2/L3 review |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 ADR
