# L3 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Legal wording requires abogado sign-off. Store declarations require a Play
Console submission or a documented pre-submission review. Consumer flows require
a drill. Every claim about TTL, price, deletion, or encryption must be traced to
the owning Line's source.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L3-V1 | ToS is published and complete | Document review + external sign-off | Bilingual ToS identifies the entity, service, price/tiers, acceptable use, IP, liability, termination, law, and venue | EV-L3-01 | TRLGDCU; LSSI Art. 10 |
| L3-V2 | Withdrawal and refund terms are operable | Document review + manual drill | 14-day withdrawal, digital-content waiver, and refund path are drafted and executable | EV-L3-02 | TRLGDCU Arts. 102-108 and 103(m); Directive 2011/83/EU |
| L3-V3 | Zero-knowledge data-loss disclosure is clear | Document review | Key loss is disclosed before purchase and at key creation, in plain language | EV-L3-03 | TRLGDCU; GDPR Art. 13 |
| L3-V4 | Complaint and ADR path is live | Manual drill | A complaint channel is monitored and the current ADR/consumer-authority route is published | EV-L3-04 | TRLGDCU; ODR status verified |
| L3-V5 | Data Safety answers are traceable | Document review | Every Data Safety answer maps to the ROPA, SDK list, and actual data flows | EV-L3-05 | Google Play Data Safety policy |
| L3-V6 | Account deletion is complete | Manual drill | In-app deletion path and functional web link both work; Data Safety deletion questions are answered | EV-L3-06 | Google Play Account Deletion policy |
| L3-V7 | Billing declaration is correct | Document review | Digital goods and subscriptions declare Play Billing and match the actual purchase flow | EV-L3-07 | Google Play Payments policy |
| L3-V8 | Content rating and listing disclosures are complete | Document review | Rating questionnaire, ads declaration, and privacy link are submitted and consistent | EV-L3-08 | Google Play policy |
| L3-V9 | Spanish consumer information is available | Document review | LSSI Art. 10 information and consumer terms are available in Spanish | EV-L3-09 | LSSI-CE Art. 10 |
| L3-V10 | Public claims match behavior | Cross-check | TTL, price, deletion, encryption, and support claims match L4/L5/L6/L8 sources | EV-L3-10 | All owning Lines |
| L3-V11 | Store listing is approved | Document review | Play Console shows the listing approved with no open policy issues | EV-L3-11 | Google Play Console |
| L3-V12 | Store/e-commerce surface accessibility is addressed | Document review | L7 audit covers the store surface, or the exemption decision is recorded | EV-L3-12 | EAA; L7 report |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L3-T1 | Manual drill | Purchase Plus, then request withdrawal within the window | Refund path executes and entitlement is revoked by L4 |
| L3-T2 | Manual drill | Request account deletion in-app and through the web link | Both paths work; confirmation is recorded |
| L3-T3 | Manual drill | Submit a complaint through the published channel | Acknowledged within the L8 SLA and routed to the ADR path if unresolved |
| L3-T4 | Cross-check | Compare every public claim with the owning Line's source | No mismatch; corrections tracked |
| L3-T5 | Document review | Abogado reviews ToS, withdrawal, refund, and Spanish information | Written sign-off or a tracked correction list |
| L3-T6 | Document review | Review the Data Safety form against the ROPA and SDK inventory | Every answer has a traceable source |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L3-01 | ToS URL and abogado sign-off | Public URL plus external folder | Founder | Pending | Each version |
| EV-L3-02 | Withdrawal/refund clause and drill record | External secure folder | Founder | Pending | Annual |
| EV-L3-03 | Key-loss disclosure and screenshot | External secure folder | Founder | Pending | On flow change |
| EV-L3-04 | Complaint channel and ADR route | Public URL | Founder | Pending | Annual |
| EV-L3-05 | Data Safety mapping table | External secure folder | Founder | Pending | On data-flow change |
| EV-L3-06 | Deletion E2E test and web link | External secure folder plus public URL | Founder | Pending | On flow change |
| EV-L3-07 | Billing declaration review | External secure folder | Founder | Pending | On billing change |
| EV-L3-08 | Content rating and listing disclosures | Play Console | Founder | Pending | Each release |
| EV-L3-09 | Spanish consumer information | Public URL | Founder | Pending | Each version |
| EV-L3-10 | Consistency cross-check table | External secure folder | Founder | Pending | Each release |
| EV-L3-11 | Store approval record | Play Console | Founder | Pending | Each release |
| EV-L3-12 | Accessibility report or exemption decision | External secure folder | Founder | Pending | Annual |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L3-01 to C-L3-11 appear only here |
| LIS-2 Boundary integrity | Pending | Refund execution, deletion code, and accessibility audit are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T3 are drills; T5-T6 are reviews |
| LIS-5 Operability | Pending | Founder owns; abogado and L8 support channel |
| LIS-6 Public-claim consistency | Pending | TTL, price, deletion, encryption, and support claims cross-checked |
| LIS-7 Independence | Pending | SC1, SC2, SC3, SC4, SC5, SC6, SC8, and SC11 are consumed |
| LIS-8 Regression | Pending | Any price, TTL, or deletion change triggers a re-run |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 ToS draft
