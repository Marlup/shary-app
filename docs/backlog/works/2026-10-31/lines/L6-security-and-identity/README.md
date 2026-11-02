# L6 — Security & Identity

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-identity-adr.md](phase-1-identity-adr.md)
Phase 1 companion: [phase-1-independent-review-rfp.md](phase-1-independent-review-rfp.md)

## 1. Purpose

Close the dominant security gaps identified in the 2026-08-30 audit, make
identity and erasure trustworthy, and prove the result with an independent
review. This Line supplies the behavior that L2 and L3 disclose.

## 2. Scope

### Owned components

- C-L6-01 Identity architecture decision record.
- C-L6-02 Authentication strength and password-derived identity hardening.
- C-L6-03 Erasure when keys are lost.
- C-L6-04 Key lifecycle: rotation, revocation, recovery boundaries.
- C-L6-05 Rate-limit and deduplication atomicity.
- C-L6-06 App Check and abuse controls.
- C-L6-07 Least-privilege IAM and secrets management.
- C-L6-08 Independent security review or penetration test.
- C-L6-09 Remediation closure and SCR re-audit.
- C-L6-10 Security evidence for MCP grants and agent access.
- C-L6-11 Android backup and device-transfer policy.

### Explicit exclusions

- Privacy Policy, ROPA, DPIA text belong to L2.
- Store deletion disclosure belongs to L3.
- Backups, DR, and incident response belong to L5.
- Pricing and entitlement quotas belong to L4.
- Accessibility and insurance belong to L7.

### Interfaces

- Consumes SC2 data map from L2 to define the deletion scope.
- Consumes SC5 retention/TTL from L5 for deletion scope.
- Provides SC6 erasure and deletion behavior to L2 and L3.
- Provides SC13 Android backup and device-transfer policy to L2 and L5.
- Consumes SC7 breach procedure from L2 for incident escalation.

## 3. Audit baseline

The 2026-08-30 audit scored Shary 4.3/9 overall (Fragile). The dominant
findings for this Line are:

- password-derived identity (PBKDF2 200k) is the highest-risk design choice;
- cloud identity cannot be erased when keys are lost;
- rate-limit and deduplication logic are not proven atomic;
- MCP grant revocation needs an end-to-end test;
- no independent security review has been performed.

The target is no critical findings and an SCR re-audit score of at least 7.

## 4. Viability definition

L6 is viable when:

1. the identity ADR is approved and the authentication path is hardened;
2. email-verified erase plus offline-secret rotate works end to end
   (option C from the account-recovery analysis);
3. rate limits and deduplication hold under concurrency;
4. an independent review finds no critical issue, and high findings are closed;
5. the SCR re-audit reaches at least 7;
6. the Android backup and device-transfer policy is decided and reflected in the
   manifest, the policy, and the continuity plan.

## 5. Section 8 coverage

No direct Section 8 item. This Line owns the audit-driven security work that
supports 8.12 (record integrity) and the data-loss disclosures in L2/L3.

## 6. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Identity architecture decision | L6-V1, L6-V2, L6-V11 |
| P2 | Erasure and key lifecycle | L6-V3, L6-V4 |
| P3 | Abuse and atomicity | L6-V5, L6-V6, L6-V7 |
| P4 | Independent verification | L6-V8, L6-V9, L6-V10 |

## 7. Official sources

- OWASP Application Security Verification Standard (ASVS).
- NIST SP 800-63B, digital identity guidelines.
- Google Cloud security best practices and IAM documentation.
- GDPR Art. 32, security of processing.
- ENISA threat landscape and guidance.
- The 2026-08-30 security and compliance audit and the account-recovery options
  note in this repo.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 8. Single-owner notes

- The independent review is mandatory evidence. A self-review does not satisfy
  L6-V8.
- Schedule the reviewer early; good reviewers have lead times.
- If a full penetration test is out of budget, use a focused independent review
  of identity, erasure, and MCP authorization, and record the reduced scope.
- The erasure path must not depend on the founder being available; document the
  trustee path with L5.
- Do not claim "zero-knowledge" in public copy until the implementation and the
  independent review support it.

## 9. Open decisions

- Choose the identity redesign: replace password-derived identity, add a
  device-bound factor, or keep it with compensating controls.
- Choose the independent reviewer and the review scope.
- Decide whether to target OWASP ASVS Level 1 or Level 2 at launch.
- Decide whether the SCR re-audit is internal or external.
- Decide whether to disable Android backup entirely (recommended) or exclude
  every sensitive file and disclose the rest.
