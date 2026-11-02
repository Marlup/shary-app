# Lines of Refinement — Eight Independent Viability Workstreams

Date: 2026-10-31
Status: Completed planning baseline
Owner: Founder (single-owner project)
Scope: Split Section 8 of the 2026-08-30 business and monetization draft into
eight independently developable Lines, each with a scoped component inventory,
a validation suite, and a phase plan to an operable launch gate.

## 1. Scope

The 2026-08-30 draft ended with 13 suggested missing aspects (Section 8). They
were woven through the business, legal, and continuity analysis but had no
standalone ownership, acceptance criteria, or delivery order. This task:

1. converts the 13 suggestions into eight Lines with one primary owner each;
2. defines the validation suite that proves a Line is operable, not merely
   documented;
3. produces a phase plan per Line with exit gates and official sources;
4. centralizes shared components and blocking gates in a cross-line register;
5. applies the Phase 1 decisions and the single-owner constraint.

## 2. Change list

Created:

- `docs/backlog/works/2026-10-31/lines-of-refinement.md` (this work log).
- `docs/backlog/works/2026-10-31/lines/README.md` (line map, coverage matrix,
  validation standard, decisions, status board).
- `docs/backlog/works/2026-10-31/lines/cross-line.md` (shared components,
  blocking gates, change protocol, single-owner implications).
- `docs/backlog/works/2026-10-31/lines/sources.md` (canonical identifiers,
  official URLs, retrieval status, and evidence flags).
- Eight Line folders, each with `README.md`, `validation-suite.md`, and
  `phase-plan.md`:
  - `L1-entity-and-payments`
  - `L2-privacy-and-data-governance`
  - `L3-consumer-contracts-and-store-compliance`
  - `L4-monetization-and-entitlements`
  - `L5-reliability-and-continuity`
  - `L6-security-and-identity`
  - `L7-trust-brand-and-risk`
  - `L8-support-growth-and-analytics`
- `docs/backlog/orders_draft/2026-10-31/raw-order-lines-of-refinement.md`
  (verbatim order and decisions).

Updated:

- `docs/backlog/works/works-index.md` (new 2026-10-31 entry).
- `docs/backlog/orders_draft/works-index.md` (new 2026-10-31 entry).

No application code, build files, or tests were changed. The uncommitted app
changes already in the worktree were left untouched.

## 3. Plan source

Plan source: [[docs/backlog/works/2026-08-30/business-and-monetization-draft.md#8. Other aspects I suggest you are missing]]

Supporting sources:

- [[docs/backlog/works/2026-08-30/security-compliance-audit.md]]
- [[docs/backlog/works/2026-08-30/remediation-plan.md]]
- [[docs/backlog/works/2026-08-30/privacy-policy-draft.md]]
- [[docs/backlog/works/2026-08-30/terms-of-service-draft.md]]
- [[docs/backlog/works/2026-08-30/ropa-draft.md]]
- [[docs/backlog/works/2026-08-30/account-recovery-options.md]]

## 4. Viability and complexity per Line

| Line | Viability | Complexity | Main external dependency |
|---|---|---|---|
| L1 Entity & Payments | High | Medium | Notary, Registro Mercantil, AEAT, bank, Play organization review |
| L2 Privacy & Data Governance | High | High | Abogado review and DPIA challenge |
| L3 Consumer Contracts & Store Compliance | High | Medium-high | Abogado review and Play policy review |
| L4 Monetization & Entitlements | High | High | Play Billing review and pricing evidence |
| L5 Reliability & Continuity | High | Medium | Firebase configuration and trustee acceptance |
| L6 Security & Identity | Medium | High | Independent reviewer and remediation time |
| L7 Trust, Brand & Risk Transfer | High | Medium | OEPM/EUIPO, insurance broker, accessibility auditor |
| L8 Support, Growth & Analytics | High | Medium | Support capacity and pricing evidence |

"High viability" means the work is understood and the main risk is execution
time. "Medium viability" for L6 reflects the unresolved identity redesign and
the possibility that the independent review finds material issues.

## 5. Design decisions

### D1 — Entity

S.L. in Madrid; a single-shareholder S.L.U. is acceptable. Rationale: limited
liability matches the insolvency and wind-down protocols, and an S.L. can own
the trademark, Play account, and cloud billing.

### D2 — Launch geography

Madrid/Spain first, then the EU. Spanish consumer information is required for
launch; English is maintained in parallel.

### D3 — MCP packaging

MCP is included in Shary Plus with a bounded allowance (5,000 calls plus 500
value-reads per month as the baseline). An optional usage overage add-on covers
calls beyond the allowance. MCP is not a separate base tier. Rationale: the
agent feature is part of the Plus value proposition, while overage protects
against genuine variable cost.

### D4 — Budget and timeline

Lean planning envelope of roughly EUR 8k-18k external spend, covering
incorporation, legal review, accounting setup, trademark, insurance, and a
focused security review. Target launch gate is 2027-03-20 (20 weeks), driven by
gate completion rather than a fixed date. These are planning ranges, not quotes;
L1, L3, L6, and L7 must replace them with actual quotes.

### D5 — Evidence storage

Sensitive evidence (deeds, NIF, DPAs, insurance policies, audit reports) stays
outside the repository in the founder's secured folder. The repository stores
the evidence register, location, owner, and date.

### D6 — Ownership

The founder owns all eight Lines. External asesor, abogado, insurance broker,
and independent security reviewer provide the sign-off that a second internal
person would otherwise provide.

### D7 — Store publishing

Production and paid launch wait for the S.L., NIF, and an organization Play
account. Closed testing may precede incorporation only with a documented
transfer plan to the organization account before launch.

### D8 — Product scope

Android-only MVP. iOS and web are deferred; the business model and billing
assumptions are Android/Play-first.

### Independence model

- One owner per artifact; no Line writes another Line's deliverable.
- One source of truth per shared value (SC1-SC12 in `cross-line.md`).
- Shared inputs gate publication, not start.
- A consumer reads a shared value; it never forks it.
- Evidence, not assertion, closes a gate.

### Validation standard

Every Line uses the eight Line Integrity Suite checks (LIS-1 to LIS-8): scope
integrity, boundary integrity, evidence traceability, behavioral proof,
operability, public-claim consistency, independence, and regression. Each Line
also has viability criteria, a test/drill matrix, an evidence register, and a
gate decision block.

## 6. Implementation plan

### Line map

| Line | Phases | Viability gate |
|---|---|---|
| L1 | 3 | One real test payment travels from charge to retained invoice |
| L2 | 4 | Rights and breach processes operable within statutory deadlines |
| L3 | 4 | Store listing approved; public claims match behavior |
| L4 | 4 | Purchase grants/revokes entitlement; server quotas hold |
| L5 | 4 | Restore meets RTO/RPO; five protocols exercised |
| L6 | 4 | No critical findings; erasure proven; SCR at least 7 |
| L7 | 3 | Trademark filed; insurance bound; disclosure live; accessibility closed |
| L8 | 3 | SLA live; analytics lawful; Spanish ready; CAC approved |

### Critical path

1. **Weeks 1-4:** start Phase 1 of all eight Lines. The external lead-time
   path is L1 incorporation, L2 data map, L4 pricing spec, L5 backup and TTL
   configuration, L6 identity ADR, and L7 trademark search. Schedule the
   abogado, reviewer, and insurance broker now; their lead times dominate. L3
   ToS drafting and L8 support setup also start here but are not on the
   external lead-time path.
2. **Weeks 4-10:** L1 tax and merchant setup; L2 artifacts; L3 ToS draft; L4
   billing integration; L5 monitoring; L6 erasure work; L7 filing and quotes.
3. **Weeks 8-16:** L3 store package; L4 enforcement; L5 protocols; L6
   independent review; L7 accessibility; L8 support and analytics.
4. **Weeks 16-20:** consistency gates, drills, final sign-offs, and launch
   decision.

### Publication gates

The only forced sequencing is in `cross-line.md`:

- L1 entity identity before L2/L3 publication, L4 merchant setup, and L7
  insurance.
- L2 ROPA before L3 Data Safety and L8 analytics.
- L4 pricing before L3 price terms and L8 research.
- L6 erasure before L2 rights evidence and L3 deletion claim.
- L5 TTL before L2 retention statements and L3 data-retention terms.
- L8 support contact before L2 published contact and L3 complaint channel.

All other work starts in parallel.

## 7. Validation plan

This pass is documentation-only, so no unit tests were run and no behavior
changed. The validation deliverable is the suite definition itself:

- Each Line has 8-13 viability criteria with method, pass condition, evidence,
  and official source.
- Each Line has a test/drill matrix: automated tests where code exists, manual
  drills for operations, and external review for legal, tax, and security.
- Each Line has an evidence register and an LIS sign-off block.
- The cross-line register defines the shared-component checks and the change
  protocol.

Verification performed in this pass:

- Session-init policy gate passed (`policy_version=2026-07-18.1`).
- All eight Line folders contain the three required files.
- Every Section 8 item maps to exactly one primary Line.
- Every Line references its validation suite and phase plan.
- The two backlog indexes were updated without altering existing entries.
- Existing uncommitted app changes were not touched.

## 8. Evidence flags

- **ODR platform.** Regulation (EU) 2024/3228 repealed Regulation (EU) 524/2013,
  and the European ODR platform closed on 20 July 2025. L3 P2 must verify the
  current ADR/consumer-authority route with the abogado and remove stale ODR
  references. EUR-Lex was not reachable during this session, so this is a
  verify-at-gate item.
- **EAA.** Directive (EU) 2019/882 applies from 28 June 2025. The
  microenterprise service-provider exemption (<10 employees and turnover or
  balance sheet at or below EUR 2M) likely applies to a one-person project, but
  L7 must document the Spanish transposition and the exemption decision.
- **Play account deletion.** Confirmed from Play Console Help: if account
  creation exists anywhere in the app, an in-app deletion path and a functional
  web deletion link are required, plus Data Safety deletion answers. This is a
  hard L3 gate.
- **Play Billing.** Digital subscriptions must use Play Billing; EEA
  external-offer rules change periodically, so L4 P1 re-verifies the current
  policy.
- **Firestore pricing.** Standard versus Enterprise edition pricing must be
  re-checked at L4 P4 before the business case is signed.

## 9. Result summary

Shary now has eight independent viability Lines with:

- bounded ownership and explicit exclusions;
- 8-13 validation criteria each, backed by evidence and official sources;
- 3-4 phase plans with exit gates;
- a cross-line register that prevents duplicated components and defines the
  only forced sequencing;
- applied decisions for entity, geography, MCP packaging, budget, evidence
  storage, ownership, store publishing, and product scope;
- single-owner adjustments, including a named trustee, realistic SLAs, and a
  documented EAA exemption decision.

The next action is to start Phase 1 of all eight Lines, with L1, L2, L4, L5,
L6, and L7 carrying the external lead times, and to book the external asesor,
abogado, insurance broker, and security reviewer before their lead times become
the critical path.

Phase 1 execution log: [[docs/backlog/works/2026-10-31/phase-1-execution.md]].
