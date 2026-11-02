# Lines of Refinement — Index and Operating Rules

Date: 2026-10-31
Status: Planned
Owner: Founder (single-owner project)
Source: Section 8 of [[docs/backlog/works/2026-08-30/business-and-monetization-draft.md#8. Other aspects I suggest you are missing]]

## 1. Purpose

This folder splits the 13 suggested missing aspects from the 2026-08-30
business/monetization draft into eight independent Lines. Each Line has:

- a bounded scope and an explicit list of owned components;
- a validation suite with pass/fail criteria and evidence;
- a phase plan ending in an "operable at launch" gate;
- a single owner and a single source of truth for every shared value.

The goal is parallel development without component duplication. A Line may
start Phase 1 on day one. Cross-line inputs gate publication, not start.

## 2. Independence model

1. **One owner per artifact.** No Line writes another Line's deliverable.
2. **One source of truth per shared value.** Entity identity, ROPA, pricing,
   TTL, deletion behavior, support contact, and the stewardship clause each
   have exactly one owning Line (see [cross-line.md](cross-line.md)).
3. **Shared inputs gate publication, not start.** Drafts may use placeholders;
   final viability gates consume the owning Line's approved artifact.
4. **No forked values.** A consumer Line reads a shared value; it never copies
   and edits it.
5. **Evidence over assertion.** A gate cannot close on verbal assurance.

## 3. Line map

| ID | Line | Owned components | Viability outcome | Phases | Folder |
|---|---|---|---|---|---|
| L1 | Entity & Payments | C-L1-01 to C-L1-10 | Can invoice, collect, remit tax, and retain records | 3 | [L1-entity-and-payments](L1-entity-and-payments/README.md) |
| L2 | Privacy & Data Governance | C-L2-01 to C-L2-13 | Rights requests and breach notification operable within statutory deadlines | 4 | [L2-privacy-and-data-governance](L2-privacy-and-data-governance/README.md) |
| L3 | Consumer Contracts & Store Compliance | C-L3-01 to C-L3-11 | Store listing approved; public claims match product behavior | 4 | [L3-consumer-contracts-and-store-compliance](L3-consumer-contracts-and-store-compliance/README.md) |
| L4 | Monetization & Entitlements | C-L4-01 to C-L4-10 | Purchase grants and revokes entitlement; server quotas cannot be bypassed | 4 | [L4-monetization-and-entitlements](L4-monetization-and-entitlements/README.md) |
| L5 | Reliability & Continuity | C-L5-01 to C-L5-12 | Restore drill meets RTO/RPO; five continuity protocols exercised | 4 | [L5-reliability-and-continuity](L5-reliability-and-continuity/README.md) |
| L6 | Security & Identity | C-L6-01 to C-L6-11 | No critical findings; erasure path proven; SCR re-audit at least 7 | 4 | [L6-security-and-identity](L6-security-and-identity/README.md) |
| L7 | Trust, Brand & Risk Transfer | C-L7-01 to C-L7-10 | Trademark filed; insurance bound; disclosure live; accessibility closed or accepted | 3 | [L7-trust-brand-and-risk](L7-trust-brand-and-risk/README.md) |
| L8 | Support, Growth & Analytics | C-L8-01 to C-L8-11 | SLA live; analytics lawful; Spanish launch ready; CAC model approved | 3 | [L8-support-growth-and-analytics](L8-support-growth-and-analytics/README.md) |

The union of these ranges is the full Line scope. A component not listed in a
Line README must be added to [cross-line.md](cross-line.md) before work starts;
otherwise LIS-1 fails.

## 4. Section 8 coverage

Every suggested item has one primary owner and, where needed, a secondary
consumer. No item is orphaned and no item has two primary owners.

| Section 8 item | Primary | Secondary |
|---|---|---|
| 8.1 Consumer-protection edge cases | L3 | L4 (refund execution) |
| 8.2 Google Play policy surface | L3 | — |
| 8.3 Dispute resolution | L3 | — |
| 8.4 Support and SLAs | L8 | — |
| 8.5 Pricing and regional pricing | L4 | L8 (research) |
| 8.6 Localization | L3 (Spanish consumer information) | L8 (product copy) |
| 8.7 Insurance | L7 | — |
| 8.8 IP and branding | L7 | — |
| 8.9 Privacy-safe analytics | L8 | — |
| 8.10 Growth and CAC | L8 | L4 (pricing model) |
| 8.11 Accessibility (EAA) | L7 | L3 (store/e-commerce surface) |
| 8.12 Business-records backup | L2 (ROPA/DPA/breach register) | L1 (accounting) |
| 8.13 Succession and data stewardship | L5 | L2/L3 (disclosure) |

Audit-driven work that is not in Section 8 is mapped as follows:

- password-derived identity, erasure when keys are lost, rate-limit/dedup
  atomicity, and pen-test/re-audit belong to **L6**;
- backups, monitoring, incident response, and existential-risk controls belong
  to **L5**;
- tier and entitlement enforcement belongs to **L4**;
- Privacy Policy, ToS, ROPA, DPA, and DPIA belong to **L2** and **L3**.

## 5. Validation standard

Every Line uses the same eight Line Integrity Suite checks, recorded in its
`validation-suite.md` sign-off block:

| ID | Check | Pass condition |
|---|---|---|
| LIS-1 | Scope integrity | Every owned component has exactly one owner; no duplicates across Lines |
| LIS-2 | Boundary integrity | Exclusions are respected; every interface is listed |
| LIS-3 | Evidence traceability | Every claim maps to an artifact plus an official source with retrieval date |
| LIS-4 | Behavioral proof | Code criteria have automated test IDs; operational criteria have drill records |
| LIS-5 | Operability | Named owner, runbook, and last-executed date exist |
| LIS-6 | Public-claim consistency | Policy, ToS, and store claims match implementation values |
| LIS-7 | Independence | The gate passes with only declared shared inputs |
| LIS-8 | Regression | Shared values are unchanged or change-approved |

Each `validation-suite.md` also contains:

1. a viability-criteria table (`Lx-V1`, `Lx-V2`, ...) with method, pass
   condition, evidence, and official source;
2. a test/drill matrix (automated tests, manual drills, document review);
3. an evidence register with artifact, location, owner, date, and review
   cadence;
4. a gate decision block.

Evidence policy:

- Canonical identifiers and URLs live in [sources.md](sources.md).
- Official sources first: EUR-Lex, BOE, AEPD, EDPB, Google Play Policy Center,
  Firebase/Google Cloud documentation, OEPM, EUIPO, AEAT.
- Each source records a retrieval date.
- Signed documents and personal data stay outside the repository; the repo
  stores only the evidence register and the storage location.
- A legal or tax criterion requires external sign-off (asesor/abogado); a
  security criterion requires an independent review; a code criterion requires
  a named automated test.

## 6. Global decisions applied

| ID | Decision | Applied value |
|---|---|---|
| D1 | Legal structure | S.L. in Madrid; single-shareholder S.L.U. is acceptable |
| D2 | Launch geography | Madrid/Spain first, then EU; Spanish-first consumer information |
| D3 | MCP packaging | Included in Shary Plus with a bounded allowance; optional usage overage add-on |
| D4 | Budget and timeline | Lean envelope of roughly EUR 8k-18k external spend; 20-week gate-driven target (2027-03-20); quotes required |
| D5 | Evidence storage | Sensitive evidence outside the repo; register only in the repo |
| D6 | Ownership | Founder owns all Lines; external asesor, abogado, insurance broker, and security reviewer provide independent sign-off |
| D7 | Store publishing | Production and paid launch wait for the S.L., NIF, and organization Play account; closed testing may precede only with a documented transfer plan |
| D8 | Product scope | Android-only MVP; iOS and web deferred |

D4 is a planning envelope, not a quote. L1, L3, L6, and L7 must replace the
ranges with actual quotes before their final gates.

## 7. Single-owner constraints

The project has one person. This changes the operating assumptions:

- **Bus factor 1.** L5 must name an external trustee (abogado or asesor) for
  data stewardship, document the handover instructions, and escrow the
  credentials needed to act.
- **No separation of duties.** External sign-off replaces internal dual control
  for legal, tax, insurance, and security.
- **No 24/7 support.** L8 sets realistic single-person SLAs: Free best effort
  within 5 business days, Plus within 2 business days. No always-on on-call
  promise.
- **EAA microenterprise exemption likely.** With fewer than 10 employees and
  turnover at or below EUR 2M, the service-provider exemption probably applies,
  but L7 must document the Spanish transposition and the exemption decision
  rather than assume it.
- **Play organization account.** An organization account may require a D-U-N-S
  number; L1 must verify the current requirement before P2.
- **IP ownership.** Trademark and other IP should be held by the S.L., not the
  founder personally. If filed before incorporation, assign them to the company.
- **Corporate accounting.** An S.L. requires annual accounts and Registro
  Mercantil filings; L1 budgets for a gestoría.

## 8. Phase and gate conventions

- A Line has 3-4 phases. Phase count varies with legal and technical risk.
- A phase closes only when its named exit criteria pass and evidence is stored.
- A Line reaches viability when every `Lx-V*` criterion passes and the LIS
  sign-off is complete.
- Phases can overlap across Lines. Only the blocking gates in
  [cross-line.md](cross-line.md) force sequencing.
- Dates are indicative and gate-driven, not contractual.

## 9. Folder layout

```
docs/backlog/works/2026-10-31/
  lines-of-refinement.md
  lines/
    README.md
    cross-line.md
    sources.md
    L1-entity-and-payments/
      README.md
      validation-suite.md
      phase-plan.md
    L2-privacy-and-data-governance/
    L3-consumer-contracts-and-store-compliance/
    L4-monetization-and-entitlements/
    L5-reliability-and-continuity/
    L6-security-and-identity/
    L7-trust-brand-and-risk/
    L8-support-growth-and-analytics/
```

## 10. How to run a Line

1. Read the Line `README.md` and confirm the owned components.
2. Freeze the scope and list any new component in `cross-line.md` before work.
3. Execute the current phase in `phase-plan.md`.
4. Collect the evidence named in `validation-suite.md`.
5. Run the LIS checks and record pass/fail.
6. Sign the gate and update the status board.
7. Record any shared-value change in `cross-line.md`.

## 11. Status board

| Line | Status | Current phase | Next gate | Indicative target |
|---|---|---|---|---|
| L1 | In progress | P1 workbook complete; external actions pending | L1-V1, L1-V2, L1-V3 | 2026-12-15 |
| L2 | In progress | P1 data map drafted; abogado review pending | L2-V1, L2-V2 | 2026-12-20 |
| L3 | In progress | P1 gap review drafted; abogado review pending | L3-V1, L3-V2, L3-V3 | 2027-01-15 |
| L4 | In progress | P1 tier/price/economics frozen for planning | L4-V1, L4-V2 | 2027-01-15 |
| L5 | In progress | P1 runbook ready; console actions pending | L5-V1, L5-V2, L5-V3, L5-V13 | 2027-01-31 |
| L6 | In progress | P1 identity ADR proposed | L6-V1, L6-V2, L6-V11 | 2027-02-15 |
| L7 | In progress | P1 search/RFQ/policy drafts ready | L7-V1, L7-V2 | 2027-02-28 |
| L8 | In progress | P1 support/analytics/growth workbook ready | L8-V1, L8-V2 | 2027-02-28 |

Phase 1 artifacts:

- L1: [entity, tax, and merchant checklist](L1-entity-and-payments/phase-1-entity-tax-and-merchant-checklist.md)
- L2: [data map and ROPA delta](L2-privacy-and-data-governance/phase-1-data-map-and-ropa.md)
- L3: [ToS and store gap review](L3-consumer-contracts-and-store-compliance/phase-1-tos-and-store-gap-review.md)
- L4: [tier, pricing, and economics](L4-monetization-and-entitlements/phase-1-tier-pricing-and-economics.md)
- L5: [backup, TTL, and monitoring](L5-reliability-and-continuity/phase-1-backup-ttl-and-monitoring.md)
- L6: [identity ADR](L6-security-and-identity/phase-1-identity-adr.md)
- L7: [trademark, insurance, disclosure, accessibility](L7-trust-brand-and-risk/phase-1-trademark-insurance-disclosure.md)
- L8: [support, SLA, analytics, growth](L8-support-growth-and-analytics/phase-1-support-sla-and-analytics.md)

The execution log, findings, and external action register are in
[[docs/backlog/works/2026-10-31/phase-1-execution.md]]. Deferred infrastructure
and external steps, costs, and triggers are in
[[docs/backlog/works/2026-10-31/deferred-actions.md]].

Overall launch-critical target: 2027-03-20, subject to L1-L5 gates. L6-L8
close in parallel; the L6 pen-test and L7 trademark filing must be scheduled by
week 4 to avoid tail risk.
