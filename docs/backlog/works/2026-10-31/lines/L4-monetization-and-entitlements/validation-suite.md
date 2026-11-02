# L4 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Billing criteria require a license-test purchase or a Play Console record.
Enforcement criteria require automated tests against the server, not the UI.
Economics criteria require a signed model with the assumptions visible.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L4-V1 | Tier spec is frozen | Document review | Free/Plus/Teams limits, MCP allowance, and fair use are versioned and approved | EV-L4-01 | Business draft; L4 decision |
| L4-V2 | Price sheet is complete | Document review | EUR price, regional prices, tax display, and renewal terms are recorded | EV-L4-02 | Play Billing docs; AEAT |
| L4-V3 | Billing products exist | Document review | Subscription products, base plans, and offers match the price sheet | EV-L4-03 | Play Console |
| L4-V4 | Purchase grants entitlement | Manual drill | A license-test purchase grants Plus in the entitlement source of truth | EV-L4-04 | Play Billing integration |
| L4-V5 | Restore and plan changes work | Manual drill | Restore, upgrade, and downgrade produce the correct entitlement | EV-L4-05 | Play Billing docs |
| L4-V6 | Grace, hold, and revoke work | Manual drill | Entitlement follows Play grace, account hold, and cancellation states | EV-L4-06 | Play Billing docs |
| L4-V7 | Server quotas cannot be bypassed | Automated test | Free limits and Plus fair use are enforced server-side; a modified client cannot exceed them | EV-L4-07 | Backend contract |
| L4-V8 | MCP metering is accurate | Automated test | Calls and value-reads are counted accurately; the v1 hard cap is enforced; the ledger is ready for a v1.1 overage | EV-L4-08 | Backend contract |
| L4-V9 | Refund and withdrawal revoke access | Manual drill | A refund or withdrawal revokes Plus and records the reason | EV-L4-09 | TRLGDCU; Play policy |
| L4-V10 | Unit economics are signed off | Document review | Price, conversion, CAC, and breakeven are modeled with explicit assumptions | EV-L4-10 | Business draft Section 6 |
| L4-V11 | Tax display is correct | External sign-off | Prices display the correct VAT treatment for Spain and the EU | EV-L4-11 | AEAT OSS; EU VAT rules |
| L4-V12 | Abuse controls are linked to entitlements | Automated test | Rate limits and App Check cannot be bypassed to obtain paid capacity | EV-L4-12 | L6 controls |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L4-T1 | Manual drill | License-test purchase of Plus | Entitlement granted within the expected propagation window |
| L4-T2 | Manual drill | Cancel and let the subscription expire in the test environment | Entitlement revoked at the correct time |
| L4-T3 | Manual drill | Refund the test purchase | Entitlement revoked and the refund recorded |
| L4-T4 | Automated test | Send requests above the Free quota from a modified client | Server rejects or meters the excess; no paid capacity leaks |
| L4-T5 | Automated test | Run concurrent MCP calls above the allowance | Metering is accurate; the hard cap rejects the excess once; the ledger is ready for v1.1 |
| L4-T6 | Automated test | Replay a purchase token or entitlement check | No duplicate grant or replay gain |
| L4-T7 | Document review | Recompute the breakeven model from source assumptions | Model is internally consistent and signed |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L4-01 | Frozen tier spec | External secure folder | Founder | Pending | Each pricing change |
| EV-L4-02 | Price sheet and regional table | External secure folder | Founder | Pending | Each price change |
| EV-L4-03 | Play product configuration | Play Console | Founder | Pending | Each product change |
| EV-L4-04 | License-test purchase record | External secure folder | Founder | Pending | Each billing change |
| EV-L4-05 | Restore/upgrade/downgrade record | External secure folder | Founder | Pending | Each billing change |
| EV-L4-06 | Grace/hold/revoke record | External secure folder | Founder | Pending | Each billing change |
| EV-L4-07 | Quota enforcement test results | CI plus external folder | Founder | Pending | Each release |
| EV-L4-08 | MCP metering reconciliation | External secure folder | Founder | Pending | Monthly |
| EV-L4-09 | Refund/withdrawal record | External secure folder | Founder | Pending | Each refund |
| EV-L4-10 | Unit-economics model | External secure folder | Founder | Pending | Quarterly |
| EV-L4-11 | Tax display sign-off | External secure folder | Founder | Pending | Annual |
| EV-L4-12 | Abuse-control test results | CI plus external folder | Founder | Pending | Each release |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L4-01 to C-L4-10 appear only here |
| LIS-2 Boundary integrity | Pending | Tax remittance, contract wording, growth, and security code are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T4-T6 are automated; T1-T3 are manual drills |
| LIS-5 Operability | Pending | Founder owns; L8 handles support exceptions |
| LIS-6 Public-claim consistency | Pending | Price and quota claims must match L3 and L8 |
| LIS-7 Independence | Pending | SC1, SC5, and SC6 are consumed |
| LIS-8 Regression | Pending | Any price or quota change triggers L3/L5/L8 review |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 tier freeze
