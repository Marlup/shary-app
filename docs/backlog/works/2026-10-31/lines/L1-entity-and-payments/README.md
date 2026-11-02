# L1 — Entity & Payments

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-entity-tax-and-merchant-checklist.md](phase-1-entity-tax-and-merchant-checklist.md)

## 1. Purpose

Create the legal and financial container that lets Shary charge for a digital
subscription, remit Spanish/EU tax, and keep records that survive an audit.
This Line is the source of the entity identity used by every public artifact.

## 2. Scope

### Owned components

- C-L1-01 Entity structure decision: S.L. preferred; single-shareholder S.L.U.
  acceptable.
- C-L1-02 Incorporation: notary, estatutos, Registro Mercantil.
- C-L1-03 NIF/CIF, registered address, and legal representative.
- C-L1-04 Tax registrations: alta censal, IAE, IVA, and OSS where applicable.
- C-L1-05 Company bank account.
- C-L1-06 Google Play developer organization account.
- C-L1-07 Play payments profile and merchant account.
- C-L1-08 Google Cloud/Firebase billing ownership, backup payment method, and
  budget alerts.
- C-L1-09 Invoicing and accounting workflow, including gestoría handoff.
- C-L1-10 Finance-record retention and backup.

### Explicit exclusions

- Privacy Policy, ROPA, DPA, DPIA, and data-subject rights belong to L2.
- ToS, withdrawal, refund wording, and complaint path belong to L3.
- Prices, tier limits, and entitlement logic belong to L4.
- Insurance and trademark belong to L7.
- Ongoing tax filing is executed by the gestoría; this Line owns the setup,
  evidence, and retention, not the monthly filing itself.

### Interfaces

- Provides SC1 entity identity to L2, L3, L4, and L7.
- No shared inputs are consumed at the viability gate. The invoice dry run uses
  a placeholder price until L4 freezes the price sheet.

## 3. Viability definition

L1 is viable when one real test payment can travel end to end:

1. a customer is charged through the production payment path;
2. the S.L. receives the settlement;
3. the correct IVA treatment is applied and recorded;
4. an invoice is issued with the legal entity data;
5. the transaction and invoice are retained and restorable.

Passing the documentation checks alone is not enough; the dry run must execute.

## 4. Section 8 coverage

- 8.12 Business-records backup: secondary owner for accounting and invoicing
  records. L2 is the primary owner for data-protection records.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Entity and tax setup | L1-V1, L1-V2, L1-V3 |
| P2 | Merchant and Play readiness | L1-V4, L1-V5, L1-V7 |
| P3 | Finance operations and records | L1-V6, L1-V8 |

## 6. Official sources

- Real Decreto Legislativo 1/2010, de 2 de julio, Ley de Sociedades de Capital.
- Código de Comercio (Real Decreto de 22 de agosto de 1885), accounting and
  record-retention duties.
- Ley 58/2003, General Tributaria, record-retention periods.
- AEAT: IVA registration and One Stop Shop (OSS) guidance.
- Google Play Console Help: developer account types, payments profile, and
  merchant setup.
- Registro Mercantil and notarial incorporation guidance.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- A gestoría is required in practice for S.L. accounting and annual accounts.
  Budget for it in the planning envelope.
- The founder is the sole director and legal representative. The company bank
  account, Play organization account, and GCP billing must be in the S.L.'s
  name, not the founder's.
- Do not publish paid production under a personal Play account. If closed
  testing starts earlier, document the transfer plan to the organization
  account before launch.
- Verify the current D-U-N-S requirement for a Spanish organization account
  before opening P2.

## 8. Open decisions

- Confirm the exact S.L. incorporation route and notary with the asesor.
- Confirm whether OSS registration is needed at launch or only when selling
  outside Google Play Billing.
- Confirm the gestoría fee and the annual accounts calendar.
- Replace the D4 planning envelope with actual quotes before the final gate.
