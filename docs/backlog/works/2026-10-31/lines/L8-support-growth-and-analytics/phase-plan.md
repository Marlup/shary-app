# L8 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: SLA live, analytics lawful, Spanish ready, CAC model approved
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Support and SLA

Indicative duration: 1-2 weeks.

Objective: give users a real support path that one person can run.

Work items:

1. Choose the support inbox and optional lightweight helpdesk.
2. Publish the SLA: Free best effort within 5 business days; Plus within 2
   business days; support hours and holidays.
3. Write macros for common issues: billing, key loss, deletion, MCP access.
4. Define refund and withdrawal intake with L4.
5. Run a support drill and record the result.

Exit gate: L8-V1, L8-V2, L8-V3, L8-V4.

Evidence: EV-L8-01 to EV-L8-04.

Dependencies: L3 complaint terms; L4 refund flow. This phase satisfies G8.

Risks: support volume exceeds one person. Mitigation: self-service answers,
clear SLA, and a triage rule that protects launch-critical work.

## Phase 2 — Analytics and localization

Indicative duration: 2-3 weeks.

Objective: measure what matters without creating a shadow profile, and launch in
clear Spanish.

Work items:

1. Define the minimum event set: install, activation, share, subscription,
   churn, crash.
2. Confirm the no-third-party-SDK decision; use Play Console and server
   aggregate counters. Choose an EU-hosted stack only if the decision changes.
3. Review ePrivacy consent for the website and the app.
4. Inspect payloads for PII and remove identifiers.
5. Review the Spanish app, store, and support copy with a native reviewer.

Exit gate: L8-V5, L8-V6, L8-V7, L8-V8.

Evidence: EV-L8-05 to EV-L8-08.

Dependencies: G3 ROPA; L3 Spanish consumer information.

Risks: analytics creep. Mitigation: a closed event list and a PII test each
release.

## Phase 3 — Pricing research and CAC

Indicative duration: 2-3 weeks.

Objective: validate the price and acquisition assumptions before spending.

Work items:

1. Run Madrid/EU willingness-to-pay research.
2. Confirm the regional price table with L4.
3. Define launch channels and the CAC ceiling.
4. Model the funnel, payback, and kill criteria.
5. Build the growth dashboard from Play and billing data.
6. Cross-check the support contact and SLA across all public surfaces.

Exit gate: L8-V9, L8-V10, L8-V11, L8-V12.

Evidence: EV-L8-09 to EV-L8-12.

Dependencies: G4 pricing spec; L4 billing data.

Risks: the model assumes a conversion rate that the market does not support.
Mitigation: state the assumption, set a kill criterion, and test with a small
budget before scaling.

## Gate review

- All L8-V criteria pass.
- LIS-1 to LIS-8 pass.
- The support drill and PII test are recorded.
- SC8 is published to L2 and L3.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
