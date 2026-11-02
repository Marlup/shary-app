# L8 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Support criteria require a live channel and a drill. Analytics criteria require
payload inspection and a consent review. Pricing and CAC criteria require a
signed model with visible assumptions.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L8-V1 | Support channels are live | Manual drill | A monitored inbox and in-app contact path are published and tested | EV-L8-01 | Consumer-support practice |
| L8-V2 | SLAs are published | Document review | Free and Plus response targets and support hours are public and realistic | EV-L8-02 | L3 consumer terms |
| L8-V3 | Support drill passes | Manual drill | A test ticket is acknowledged and resolved within the SLA | EV-L8-03 | L8 runbook |
| L8-V4 | Refund/withdrawal intake works | Manual drill | A request is routed to L4 and the outcome is recorded | EV-L8-04 | TRLGDCU; L4 flow |
| L8-V5 | Analytics spec is privacy-safe | Document review | Events are aggregate or pseudonymous, purpose-limited, and retention-bound | EV-L8-05 | GDPR; ePrivacy |
| L8-V6 | ePrivacy review is complete | External review | Consent/storage rules are applied to the app and website | EV-L8-06 | ePrivacy Art. 5(3); AEPD |
| L8-V7 | Analytics payload is PII-free | Manual drill | Captured payloads contain no email, vault content, or stable device identifier | EV-L8-07 | GDPR Art. 5 |
| L8-V8 | Spanish launch copy is reviewed | Document review | Native review sign-off for app, store, and support copy | EV-L8-08 | LSSI; L3 |
| L8-V9 | Regional pricing research is complete | Document review | Madrid/EU willingness-to-pay evidence and price recommendation are recorded | EV-L8-09 | L4 price sheet |
| L8-V10 | CAC model is approved | Document review | Channels, CAC targets, funnel, payback, and kill criteria are explicit | EV-L8-10 | Business draft Section 6 |
| L8-V11 | Growth dashboard works | Manual drill | Conversion, churn, and CAC are visible from source data | EV-L8-11 | L4 billing data |
| L8-V12 | Support contact is consistent | Cross-check | Policy, ToS, store, and trust page show the same contact and SLA | EV-L8-12 | L2/L3 sources |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L8-T1 | Manual drill | Send a support request through the in-app path | Acknowledged within the SLA; ticket trace recorded |
| L8-T2 | Manual drill | Request a refund or withdrawal through support | Routed to L4; entitlement revoked; user informed |
| L8-T3 | Manual drill | Inspect analytics payloads for PII | No email, vault content, or stable device identifier |
| L8-T4 | Document review | Review the website and app consent behavior | Consent is requested where required; no pre-ticked tracking |
| L8-T5 | Cross-check | Compare support contact and SLA across policy, ToS, store, and trust page | Identical values |
| L8-T6 | Document review | Recompute CAC and breakeven from source assumptions | Model is consistent and signed |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L8-01 | Support channel configuration | External secure folder | Founder | Pending | Quarterly |
| EV-L8-02 | Published SLA page | Public URL | Founder | Pending | Annual |
| EV-L8-03 | Support drill record | External secure folder | Founder | Pending | Quarterly |
| EV-L8-04 | Refund/withdrawal drill record | External secure folder | Founder | Pending | Quarterly |
| EV-L8-05 | Analytics specification | External secure folder | Founder | Pending | On event change |
| EV-L8-06 | ePrivacy review | External secure folder | Founder | Pending | Annual |
| EV-L8-07 | PII-free payload test | External secure folder | Founder | Pending | Each release |
| EV-L8-08 | Spanish copy review | External secure folder | Founder | Pending | Each release |
| EV-L8-09 | Pricing research summary | External secure folder | Founder | Pending | Annual |
| EV-L8-10 | CAC and breakeven model | External secure folder | Founder | Pending | Quarterly |
| EV-L8-11 | Growth dashboard | External secure folder | Founder | Pending | Monthly |
| EV-L8-12 | Contact consistency check | External secure folder | Founder | Pending | Each release |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L8-01 to C-L8-11 appear only here |
| LIS-2 Boundary integrity | Pending | Pricing execution, legal terms, DPIA, and accessibility are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T3 are drills; T4 and T6 are reviews |
| LIS-5 Operability | Pending | Founder owns; one monitored inbox; realistic SLA |
| LIS-6 Public-claim consistency | Pending | Contact and SLA must match L2, L3, and the trust page |
| LIS-7 Independence | Pending | SC2 and SC4 are consumed; SC8 is provided |
| LIS-8 Regression | Pending | Any pricing or support change triggers a public-claim review |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 support drill
