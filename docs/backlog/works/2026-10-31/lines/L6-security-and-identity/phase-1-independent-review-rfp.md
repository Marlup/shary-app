# L6 Phase 1 — Independent Security Review RFP

Status: Draft (not sent)
Date: 2026-11-02
Owner: Founder (single-owner project)
Line: [L6 — Security & Identity](../README.md)
Related: [Identity ADR](phase-1-identity-adr.md)

This is a request for proposal, not a booking. Sending it and paying the reviewer
are deferred until the L6 implementation is ready.

## 1. Purpose

Obtain an independent, evidence-based review of Shary's highest-risk security
boundaries before public launch. The review must confirm that the identity,
erasure, MCP authorization, and rate-limit controls work as designed, and that
the Android backup boundary is fail-closed.

## 2. Scope

In scope:

1. **Identity and authentication**
   - PBKDF2 200k current state and the proposed 600k migration.
   - KDF versioning and on-login migration.
   - Passphrase policy and offline-guessing resistance.
   - Login challenge/signature flow and email verification.
2. **Erasure when keys are lost**
   - Email-verified erase without the old signing key.
   - Recovery-secret-authorized rotation.
   - Deletion of identity, MCP grants/devices/profiles, and dependent records.
   - Replay, rate-limit, and malicious-deletion resistance.
3. **MCP authorization and revocation**
   - OAuth 2.1 + PKCE, scopes, audience, refresh-token rotation.
   - Grant binding, device binding, owner approval, write confirmation.
   - Revocation propagation and operation queue behavior.
4. **Rate-limit and deduplication atomicity**
   - Concurrent upload/fetch/decision requests.
   - Counter and dedup updates under contention.
5. **Android backup boundary**
   - `allowBackup="false"` and all-domain exclusions.
   - No plaintext contacts/settings or vault ciphertext in backup/device transfer.
   - Explicit export path, once implemented.
6. **Key and secret handling**
   - Android Keystore usage, credential wrapping, KMS envelope keys.
   - No secrets in source, logs, or build artifacts.

Out of scope:

- Full infrastructure penetration test, physical attacks, social engineering,
  denial-of-service, or third-party Google/Firebase services.
- Legal, privacy, or tax review.
- Code style or general refactoring.

## 3. Deliverables

1. Written report with:
   - executive summary;
   - methodology and scope;
   - findings by severity (Critical / High / Medium / Low / Informational);
   - reproduction steps and evidence;
   - remediation guidance;
   - residual-risk statement.
2. A 30-minute walkthrough call.
3. One retest after fixes, with a closure statement.
4. Confirmation that no production user data was accessed.

## 4. Reviewer qualifications

- Experience with Android security, Kotlin, Firebase, and OAuth/MCP.
- Experience reviewing zero-knowledge or E2EE designs.
- Ability to test Android builds and backend APIs in a staging environment.
- References from comparable small-product reviews.
- Written confidentiality terms.

## 5. Access and logistics

- Test Firebase project and test accounts; no production user data.
- Debug or internal build with test keys; production signing keys are not shared.
- Backend API documentation and the relevant source paths.
- A named contact for triage.
- Findings shared only with the founder until closure.

## 6. Timeline

| Stage | Target |
|---|---|
| RFP sent | After L6 P2/P3 code is ready |
| Proposal and scope agreement | 1 week |
| Review | 2-3 weeks |
| Remediation | 2-4 weeks |
| Retest | 1 week |

## 7. Budget

Planning range: **EUR 1,500-5,000** for a focused review. A full penetration test
may cost more. The final quote must state the scope, days, and retest cost.

## 8. Evaluation criteria

| Criterion | Weight |
|---|---|
| Relevant Android/Firebase/E2EE experience | 30% |
| Clear scope and methodology | 20% |
| Quality of sample report | 20% |
| Timeline | 15% |
| Price | 15% |

## 9. Proposal format

Ask for:

1. Firm and reviewer profiles.
2. Relevant references.
3. Proposed scope and exclusions.
4. Methodology and tools.
5. Timeline and availability.
6. Fixed price and retest price.
7. Confidentiality and data-handling terms.
8. Sample report with findings redacted.

## 10. RFP email template

Subject: Independent security review — Shary Android/Firebase vault

Hello,

Shary is a small Android-first encrypted vault with a Firebase relay and an
OAuth-gated MCP gateway. We are looking for a focused independent review of
identity, erasure when keys are lost, MCP authorization/revocation, rate-limit
atomicity, and the Android backup boundary.

Scope: approximately 5-8 review days plus a one-day retest.
Deliverable: severity-ranked report with reproduction steps and remediation
guidance.
Environment: test Firebase project and test accounts; no production data.
Budget: EUR 1,500-5,000 depending on scope.

Could you send a proposal with scope, timeline, fixed price, retest price, and a
redacted sample report?

Thank you,
[Founder]
[Contact]

## 11. Next actions

- [ ] Finalize the scope after L6 P2/P3 code lands.
- [ ] Shortlist 3 reviewers.
- [ ] Send the RFP.
- [ ] Compare proposals.
- [ ] Book after the code is testable.
- [ ] Store the report and retest evidence in EV-L6-08.
