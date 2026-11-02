# L2 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: rights and breach processes operable within statutory deadlines
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Data map and role model

Indicative duration: 2-3 weeks.

Objective: know exactly what data Shary processes, why, where it lives, and who
is responsible.

Work items:

1. Inventory every data flow: local vault, email share, cloud relay, MCP/agent,
   billing, support, analytics.
2. Record purpose, legal basis, retention, recipients, and region for each flow.
3. Confirm controller/processor roles and the Google/Firebase DPA.
4. Draft ROPA v1 and the legal-basis register.
5. List subprocessors and confirm EU-region pinning.

Exit gate: L2-V1, L2-V2.

Evidence: EV-L2-01, EV-L2-02.

Dependencies: G1 entity identity from L1; G5 TTL from L5 for retention fields.

Risks: hidden flows in the MCP layer or billing. Mitigation: trace code paths
and backend contracts, not just the UI.

## Phase 2 — Privacy artifacts

Indicative duration: 3-4 weeks.

Objective: produce the public and contractual privacy artifacts.

Work items:

1. Draft the bilingual Privacy Policy from the ROPA.
2. Accept the Google/Firebase DPA and publish the subprocessor list.
3. Write the DPIA for the biometric gate, MCP access, and E2E key custody.
4. Document age-14 handling and separate marketing consent.
5. Decide with L6 whether Android backup is disabled or disclosed; reflect it in
   the policy and ROPA.
6. Obtain abogado review and record corrections.

Exit gate: L2-V3, L2-V4, L2-V5, L2-V11, L2-V13.

Evidence: EV-L2-03, EV-L2-04, EV-L2-05, EV-L2-11.

Dependencies: P1 complete; G1 entity identity for the controller; SC13 Android
backup decision from L6.

Risks: policy overpromises that the implementation cannot meet. Mitigation:
write claims from the data map and run the LIS-6 consistency check.

## Phase 3 — Rights and breach operations

Indicative duration: 2-3 weeks.

Objective: make rights requests and breach response executable, not just
documented.

Work items:

1. Define the rights intake channel and identity-verification steps.
2. Implement or document export/delete surfaces, including the key-loss path
   owned by L6.
3. Create the breach register, severity matrix, and 72-hour template.
4. Set the response SLA and assign the single-owner responsibilities.
5. Run the rights drill and the breach tabletop.

Exit gate: L2-V6, L2-V7, L2-V8, L2-V9.

Evidence: EV-L2-06, EV-L2-07, EV-L2-08, EV-L2-09.

Dependencies: G5 retention from L5; G6 erasure behavior from L6; G8 support
contact from L8.

Risks: rights requests arrive through an unmonitored channel. Mitigation: use
the L8 support channel and test it end to end.

## Phase 4 — Publication and verification

Indicative duration: 1-2 weeks.

Objective: publish the artifacts, wire them into the product, and verify the
record trail.

Work items:

1. Publish the Privacy Policy at a stable URL and link it from the app and
   store listing.
2. Publish or make available the subprocessor list.
3. Brief the founder on rights and breach handling; store the runbook.
4. Back up the data-protection records and run a restore test.
5. Obtain final abogado sign-off.

Exit gate: L2-V10, L2-V12, plus final sign-off.

Evidence: EV-L2-10, EV-L2-12.

Dependencies: G1 entity identity; G6 erasure behavior; G8 support contact.

Risks: published policy drifts from the product. Mitigation: add the policy
version to the release checklist and re-run LIS-6 each release.

## Gate review

- All L2-V criteria pass.
- LIS-1 to LIS-8 pass.
- SC2, SC3, SC7, and SC12 are published to consumers.
- The breach tabletop and rights drill are recorded with dates.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
