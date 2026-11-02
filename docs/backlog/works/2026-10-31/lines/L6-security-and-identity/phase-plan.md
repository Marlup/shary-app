# L6 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: no critical findings, erasure proven, SCR re-audit at least 7
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Identity architecture decision

Indicative duration: 2-3 weeks.

Objective: decide how Shary authenticates a cloud identity without making a
lost password unrecoverable or a leaked password catastrophic.

Work items:

1. Document the current password-derived identity flow and its threats.
2. Compare options: replace it, add a device-bound factor, or keep it with
   compensating controls.
3. Define the recovery boundary and the zero-knowledge claim.
4. Decide the Android backup and device-transfer policy with L2/L5 (SC13).
5. Record the ADR, including residual risk and migration impact.
6. Add tests for authentication rate limiting and lockout.

Exit gate: L6-V1, L6-V2, L6-V11.

Evidence: EV-L6-01, EV-L6-02.

Dependencies: audit and account-recovery notes from 2026-08-30.

Risks: the redesign expands into a full auth rewrite. Mitigation: bound the ADR
to the dominant risk and keep the migration path incremental.

## Phase 2 — Erasure and key lifecycle

Indicative duration: 2-3 weeks.

Objective: make erasure and revocation real, including when keys are lost.

Work items:

1. Implement option C: email-verified erase plus offline-secret rotate.
2. Implement identity key rotation and MCP grant revocation.
3. Ensure old keys and grants stop working immediately.
4. Add end-to-end tests for the lost-key path and revocation.
5. Document the user-facing behavior for L2 and L3.

Exit gate: L6-V3, L6-V4.

Evidence: EV-L6-03, EV-L6-04.

Dependencies: G3 data map from L2; L2 rights runbook; L5 trustee path for the
manual fallback.

Risks: erasure deletes the wrong scope or leaves ciphertext orphaned.
Mitigation: define the deletion scope from the data map and test it against the
ROPA.

## Phase 3 — Abuse and atomicity

Indicative duration: 2-3 weeks.

Objective: make quotas, rate limits, and deduplication hold under concurrency.

Work items:

1. Review the rate-limit and deduplication implementation.
2. Make the limit checks atomic at the datastore boundary.
3. Enable App Check where required.
4. Review IAM roles and secret storage; rotate any stale secret.
5. Add concurrency tests at the quota boundary.

Exit gate: L6-V5, L6-V6, L6-V7.

Evidence: EV-L6-05, EV-L6-06, EV-L6-07.

Dependencies: L4 quota definitions; L5 monitoring for abuse alerts. This phase
satisfies G7 for L4 P3.

Risks: client-side checks create a false sense of enforcement. Mitigation: the
server is the only enforcement point; tests use a modified client.

## Phase 4 — Independent verification

Indicative duration: 3-4 weeks, including reviewer lead time.

Objective: prove the result and close the audit gap.

Work items:

1. Select the independent reviewer and agree the scope.
2. Run the review against identity, erasure, MCP authorization, and rate limits.
3. Triage findings and close critical and high issues.
4. Re-run the SCR audit with the new evidence.
5. Publish the security posture summary for L7.

Exit gate: L6-V8, L6-V9, L6-V10.

Evidence: EV-L6-08, EV-L6-09, EV-L6-10.

Dependencies: P1-P3 complete. L7 publishes the trust page from this report after
the review.

Risks: reviewer findings expand scope late. Mitigation: schedule the review in
week 4 and reserve a remediation buffer.

## Gate review

- All L6-V criteria pass.
- LIS-1 to LIS-8 pass.
- The independent report has no open critical finding.
- SC6 is published to L2 and L3.
- SC13 is published to L2 and L5.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
