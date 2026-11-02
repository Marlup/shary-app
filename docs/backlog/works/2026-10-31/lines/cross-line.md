# Cross-Line Register

Date: 2026-10-31
Status: Planned
Owner: Founder (single-owner project)
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]

## 1. Purpose

This file centralizes only the shared components, blocking gates, and change
rules that cannot belong to a single Line. It prevents duplicated ownership
and keeps the eight Lines independently developable.

## 2. Shared component register

Each shared component has one owner. Consumers read it; they never fork it.

| ID | Shared component | Owner | Consumers | Source of truth | Change trigger |
|---|---|---|---|---|---|
| SC1 | Entity identity: legal name, NIF, address, contact | L1 | L2, L3, L4, L7 | L1 documents and README | Name, NIF, address, or representative change |
| SC2 | ROPA and data map | L2 | L3, L8 | L2 ROPA | New data flow, purpose, processor, or retention change |
| SC3 | Public policy URLs | L2 and L3 | All public surfaces | Published Privacy Policy and ToS URLs | Policy version change |
| SC4 | Tier and pricing spec | L4 | L3, L5, L8 | L4 spec | Price, quota, expiry, or packaging change |
| SC5 | Retention and TTL values | L5 | L2, L3 | L5 configuration and runbook | TTL, backup retention, or deletion change |
| SC6 | Erasure and account-deletion behavior | L6 | L2, L3 | L6 implementation and tests | Deletion flow or key-loss handling change |
| SC7 | Breach procedure | L2 | L5 | L2 runbook and register | Contact, severity, or notification change |
| SC8 | Support contact and SLA | L8 | L2, L3 | L8 SLA page | Support model or response target change |
| SC9 | Data-stewardship clause | L5 | L2, L3 | L5 clause and trustee acceptance | Trustee or dissolution process change |
| SC10 | Business-records register | L1, L2, L5 by record type | L1, L2, L5 | Records register | New record type or retention period |
| SC11 | Accessibility report | L7 | L3 | L7 audit report | App or store-surface change |
| SC12 | Subprocessor list | L2 | L7 | L2 list | Vendor or region change |
| SC13 | Android backup and device-transfer policy | L6 | L2, L5 | L6 decision plus manifest and backup rules | Manifest, backup rule, or OS behavior change |

## 3. Blocking gates

Only these dependencies force sequencing. Everything else is a publication
gate, not a start gate.

| Gate | Requires | Blocks | Why |
|---|---|---|---|
| G1 | L1 P1: NIF and entity identity | L2 P4 publication, L3 P3 store package, L7 P2 insurance | Public policies, contracts, and insurance need a legal person |
| G2 | L1 P2: merchant and Play payments | L4 P2 billing integration | Billing needs a live merchant account and payments profile |
| G3 | L2 P1: ROPA and data map | L3 P3 Data Safety answers, L6 P2 deletion scope, L8 P2 analytics spec | Store declarations, erasure scope, and analytics must trace to the data map |
| G4 | L4 P1: frozen pricing and tier spec | L3 P1 price terms, L8 P3 price research | Consumer terms and research need final prices and limits |
| G5 | L5 P1: TTL and backup retention | L2 P1 retention fields, L2 P3 retention statement, L3 P1 data-retention terms, L4 P3 tier expiry | Public and enforced retention must match configuration |
| G6 | L6 P2: erasure and deletion behavior | L2 P3 rights evidence, L3 P3 deletion claim | The policy and store claim must match the implemented flow |
| G7 | L6 P3: abuse controls | L4 P3 quota enforcement | Entitlement quotas must not be bypassable through missing attestation or rate limits |
| G8 | L8 P1: support contact and SLA | L2 P4 published contact, L3 P3 complaint channel, L5 P2 incident communication | Rights requests, complaints, and incidents need a live, monitored channel |
| G9 | L2 P2 privacy artifacts and L6 P3 security evidence | L7 P2 insurance questionnaire, L7 P3 trust page | Insurance and public trust claims need reviewed privacy and security evidence |
| G10 | L2 P4: published Privacy Policy URL | L3 P3 store package, L7 P3 trust page | The store listing and trust page need a live privacy policy link |
| G11 | L6 P1: Android backup and device-transfer decision | L2 P2 policy/ROPA, L5 P4 local continuity plan | The backup exposure must match the policy and the continuity plan |

All Lines can start Phase 1 immediately with placeholders. Only the named phase
gate waits for the shared artifact; the placeholder must be replaced before that
gate. A cycle is not permitted at phase level: L2 P1 precedes L6 P2, and L6 P2
precedes L2 P3.

## 4. Change protocol

A shared-value change follows this sequence:

1. The owner records the change in the change log below: date, component ID,
   old value, new value, reason.
2. Every consumer confirms whether its validation suite or public artifact is
   affected.
3. Affected Lines update their evidence and re-run LIS-6 (public-claim
   consistency) and LIS-8 (regression).
4. The owner signs the change; the gate is not closed until all consumers
   confirm.
5. A consumer may not create a local variant to avoid the change.

## 5. Single-owner implications

Because one person owns every Line:

- **No separation of duties.** L1 uses an asesor for tax sign-off, L2 and L3 use
  an abogado for legal review, L6 uses an independent security reviewer, and L7
  uses an insurance broker. These external sign-offs are mandatory evidence,
  not optional advice.
- **Bus factor 1.** L5 names an external trustee for data stewardship, documents
  the handover procedure, and escrows the credentials required to notify users,
  export records, and delete data. The trustee must accept the role in writing.
  If the trustee cannot operate Firebase, name a technical executor and keep the
  legal authority with the trustee.
- **No 24/7 operations.** L8 publishes realistic SLAs. L5 defines support hours
  and an incident communication path that one person can sustain.
- **Capacity is the critical path.** Legal review, pen-test scheduling, and
  trademark examination have external lead times. Start them in week 1-4 even
  though their gates close later.
- **EAA exemption.** The microenterprise service-provider exemption likely
  applies, but L7 must document the Spanish transposition, the employee and
  turnover test, and the decision. An exemption is a documented decision, not an
  assumption.
- **Play organization account.** Verify the D-U-N-S requirement and transfer
  path before creating the production account. Do not publish paid production
  under a personal account.
- **IP ownership.** File trademarks and hold licenses in the S.L.'s name. If a
  filing predates incorporation, execute an assignment to the company.

## 6. Conflict resolution

- If two Lines claim the same component, LIS-1 fails. Assign the component to
  one owner and record the decision here.
- If a shared value is disputed, the owner decides after hearing consumers. The
  decision and rationale go in the change log.
- If the owner is unavailable, the L5 trustee acts only within the written
  handover instructions; the trustee cannot change product scope or pricing.

## 7. Change log

| Date | Component | Old value | New value | Reason | Consumers confirmed |
|---|---|---|---|---|---|
| 2026-10-31 | — | — | Baseline register created | Initial planning pass | n/a |
| 2026-10-31 | SC13 | Android backup not in scope | New shared component: Android backup/device-transfer policy | Phase 1 code review found `allowBackup="true"` with incomplete exclusions | L2, L5, L6 acknowledged |

## 8. Cross-line validation checklist

Before any Line closes its viability gate, confirm:

- [ ] The Line's owned components appear in exactly one Line.
- [ ] Every shared input is consumed from the owner's source, not copied.
- [ ] Every blocking gate in Section 3 is satisfied or explicitly waived.
- [ ] LIS-6 and LIS-8 pass for every affected consumer.
- [ ] The change log has an entry for every shared-value change since the last
      gate.
