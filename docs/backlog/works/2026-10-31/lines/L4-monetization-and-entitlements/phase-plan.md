# L4 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: purchase grants entitlement, server quotas hold, model approved
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Pricing and tier specification

Indicative duration: 2-3 weeks.

Objective: freeze the commercial offer before building billing or writing
contract terms.

Work items:

1. Confirm Free and Plus limits from the baseline table.
2. Confirm MCP is included in Plus with the bounded allowance and defer overage
   billing to v1.1 (D3, D3a).
3. Confirm EUR prices, annual discount, and regional pricing.
4. Define fair use and the enforcement action for abuse.
5. Build the unit-economics model with conversion, CAC, and breakeven.
6. Confirm VAT display and the tax treatment with the asesor.

Exit gate: L4-V1, L4-V2, L4-V10, L4-V11.

Evidence: EV-L4-01, EV-L4-02, EV-L4-10, EV-L4-11.

Dependencies: L1 tax setup for the VAT treatment.

Risks: pricing changes after ToS drafting. Mitigation: freeze the spec before
L3 P1 and route changes through [cross-line.md](../cross-line.md).

## Phase 2 — Billing integration

Indicative duration: 3-4 weeks.

Objective: make Play Billing the reliable source of entitlement truth.

Work items:

1. Create subscription products, base plans, and offers in Play Console.
2. Implement purchase, restore, upgrade, and downgrade.
3. Handle grace period, account hold, and cancellation.
4. Implement refund and withdrawal revocation.
5. Run license-test purchases and record the results.

Exit gate: L4-V3, L4-V4, L4-V5, L4-V6, L4-V9.

Evidence: EV-L4-03 to EV-L4-06, EV-L4-09.

Dependencies: G2 merchant and Play payments; G4 pricing spec.

Risks: purchase-token replay and delayed propagation. Mitigation: server-side
verification and idempotent entitlement updates.

## Phase 3 — Entitlement enforcement

Indicative duration: 2-3 weeks.

Objective: enforce the limits on the server, not in the UI.

Work items:

1. Implement server-side Free quotas: cloud sends, inbox items, storage.
2. Implement Plus fair use and MCP call/value-read metering.
3. Enforce expiry by tier: 7 days Free, up to 14 days Plus.
4. Link rate limits and App Check to entitlement decisions.
5. Add automated tests for quota bypass, concurrency, and replay.

Exit gate: L4-V7, L4-V8, L4-V12.

Evidence: EV-L4-07, EV-L4-08, EV-L4-12.

Dependencies: G5 TTL from L5; G7 abuse controls from L6.

Risks: quota logic diverges between client and server. Mitigation: the server
is the only source of truth; the client displays, never decides.

## Phase 4 — Revenue validation

Indicative duration: 2-3 weeks.

Objective: decide whether the model is viable before launch spend.

Work items:

1. Recompute conversion, churn, CAC, and breakeven with current assumptions.
2. Test the Madrid/EU price points with L8 research.
3. Confirm the v1.1 overage design; v1 has no overage revenue.
4. Record a go/no-go recommendation and the conditions for revisiting.

Exit gate: L4-V10 final sign-off.

Evidence: EV-L4-10 updated with the go/no-go record.

Dependencies: L8 price research; actual Play fees and Firebase pricing.

Risks: the model relies on unverified conversion. Mitigation: state the
assumption and the kill criterion explicitly.

## Gate review

- All L4-V criteria pass.
- LIS-1 to LIS-8 pass.
- A license-test purchase, refund, and quota-bypass test are recorded.
- SC4 is published to L3, L5, and L8.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
