# L3 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: store listing approved and public claims match product behavior
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — ToS and consumer terms

Indicative duration: 3-4 weeks.

Objective: produce a contract that matches the product and can be enforced.

Work items:

1. Draft the bilingual ToS from the entity identity and the L4 tier spec.
2. Draft the 14-day withdrawal right and the digital-content waiver.
3. Draft the refund policy for partial periods and Play-initiated refunds.
4. Add the zero-knowledge key-loss disclosure and the liability position.
5. Obtain abogado review and record corrections.

Exit gate: L3-V1, L3-V2, L3-V3.

Evidence: EV-L3-01, EV-L3-02, EV-L3-03.

Dependencies: G1 entity identity; G4 pricing spec; G5 TTL.

Risks: the waiver is drafted but the purchase flow does not capture it.
Mitigation: specify the purchase-flow consent text for L4.

## Phase 2 — Dispute and complaint operations

Indicative duration: 1-2 weeks.

Objective: give consumers a working complaint path and the correct ADR route.

Work items:

1. Verify the current ADR/consumer-authority route after the ODR platform
   closure; record the official source and date.
2. Define the complaint intake channel and response times with L8.
3. Create acknowledgement, escalation, and unresolved-complaint templates.
4. Run a complaint drill.

Exit gate: L3-V4.

Evidence: EV-L3-04.

Dependencies: G8 support contact from L8.

Risks: publishing a dead ODR link. Mitigation: verify the route at the gate
and remove stale references.

## Phase 3 — Play compliance package

Indicative duration: 2-3 weeks.

Objective: submit a complete, consistent store listing.

Work items:

1. Complete the Data Safety form from the ROPA and SDK inventory.
2. Implement and verify the in-app deletion path and the web deletion link.
3. Complete the Billing declaration and content rating.
4. Publish the Privacy Policy link and the Spanish consumer information.
5. Submit the listing and record any policy feedback.

Exit gate: L3-V5, L3-V6, L3-V7, L3-V8, L3-V9.

Evidence: EV-L3-05 to EV-L3-09.

Dependencies: G1 entity; G3 ROPA; G4 pricing; G6 deletion; G8 support; G10
published Privacy Policy URL.

Risks: Data Safety answers drift from the actual SDKs. Mitigation: trace every
answer to the ROPA and re-check after each dependency.

## Phase 4 — Pre-launch consistency gate

Indicative duration: 1-2 weeks.

Objective: prove that the public surface and the product agree.

Work items:

1. Run the consistency cross-check for TTL, price, deletion, encryption, and
   support.
2. Confirm the L7 accessibility report covers the store/e-commerce surface, or
   record the exemption decision.
3. Run the withdrawal/refund and account-deletion drills.
4. Confirm the store listing is approved.
5. Sign the launch checklist.

Exit gate: L3-V10, L3-V11, L3-V12.

Evidence: EV-L3-10, EV-L3-11, EV-L3-12.

Dependencies: L4 billing live; L5 TTL stable; L6 deletion live; L7 accessibility
decision; L8 support live.

Risks: late product changes invalidate the listing. Mitigation: freeze the
public claims before submission and rerun the cross-check after any change.

## Gate review

- All L3-V criteria pass.
- LIS-1 to LIS-8 pass.
- The store listing is approved.
- The complaint, withdrawal, and deletion drills are recorded.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
