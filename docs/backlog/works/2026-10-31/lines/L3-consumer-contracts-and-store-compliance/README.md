# L3 — Consumer Contracts & Store Compliance

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-tos-and-store-gap-review.md](phase-1-tos-and-store-gap-review.md)

## 1. Purpose

Make the contract, the store listing, and the consumer-facing disclosures
consistent with what Shary actually does. This Line is the public legal surface
that a user sees before and after purchase.

## 2. Scope

### Owned components

- C-L3-01 Terms of Service, bilingual Spanish and English.
- C-L3-02 Withdrawal and refund terms, including the digital-content waiver.
- C-L3-03 Zero-knowledge data-loss disclosure.
- C-L3-04 Complaint and ADR/consumer-authority path.
- C-L3-05 Google Play Data Safety form.
- C-L3-06 Account-deletion disclosure and web deletion link.
- C-L3-07 Play Billing declaration.
- C-L3-08 Content rating and store-listing disclosures.
- C-L3-09 Privacy Policy link and store-listing consistency.
- C-L3-10 Spanish consumer information required by LSSI.
- C-L3-11 Pre-launch consistency checklist.

### Explicit exclusions

- Refund execution, entitlement revocation, and billing integration belong to
  L4.
- Deletion and erasure implementation belongs to L6.
- Accessibility audit and remediation belong to L7.
- Privacy Policy content and ROPA belong to L2.
- Complaint handling operations and SLAs belong to L8.

### Interfaces

- Consumes SC1 entity identity from L1.
- Consumes SC2 ROPA from L2 for Data Safety answers.
- Consumes SC3 Privacy Policy URL from L2.
- Consumes SC4 pricing and tier spec from L4.
- Consumes SC5 retention/TTL from L5.
- Consumes SC6 deletion behavior from L6.
- Consumes SC8 support contact and SLA from L8.
- Consumes SC11 accessibility report from L7.
- Provides the complaint channel to L8 operations.

## 3. Viability definition

L3 is viable when:

1. the Play listing is approved with complete Data Safety, deletion, billing,
   and privacy disclosures;
2. the ToS, Privacy Policy, and store listing agree with the actual TTL, price,
   deletion, encryption, and support behavior;
3. a user can exercise withdrawal, refund, complaint, and account deletion
   through a documented path;
4. Spanish consumer information is available and legally reviewed.

## 4. Section 8 coverage

- 8.1 Consumer-protection edge cases: primary owner.
- 8.2 Google Play policy surface: primary owner.
- 8.3 Dispute resolution: primary owner.
- 8.6 Localization: primary owner for Spanish consumer information.
- 8.11 Accessibility: secondary owner for the store/e-commerce surface; L7
  owns the audit.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | ToS and consumer terms | L3-V1, L3-V2, L3-V3 |
| P2 | Dispute and complaint operations | L3-V4 |
| P3 | Play compliance package | L3-V5 to L3-V9 |
| P4 | Pre-launch consistency gate | L3-V10, L3-V11, L3-V12 |

## 6. Official sources

- Real Decreto Legislativo 1/2007 (TRLGDCU), consumer contracts and the
  withdrawal right.
- Ley 34/2002 (LSSI-CE), information-society service duties and Spanish
  consumer information.
- Directive 2011/83/EU on consumer rights.
- Regulation (EU) 524/2013 on online dispute resolution. Note: Regulation (EU)
  2024/3228 repealed it and the European ODR platform closed on 20 July 2025;
  verify the current ADR/consumer-authority path with the abogado before P2.
- Google Play Policy Center: User Data, Data Safety, Account Deletion, and
  Payments policies.
- Directive (EU) 2019/882 (European Accessibility Act) and its Spanish
  transposition; verify the microenterprise exemption in L7.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- The 14-day withdrawal right and the digital-content waiver must be drafted by
  the abogado and implemented by L4; a clause without an executable refund path
  is not enough.
- The zero-knowledge model means key loss is permanent data loss. This must be
  disclosed before purchase and again at key creation.
- Play account deletion requires both an in-app path and a functional web link
  when account creation exists anywhere in the app. This is confirmed by Play
  Console Help and is a hard gate.
- Do not promise a support response time that one person cannot sustain; use
  the L8 SLA.

## 8. Open decisions

- Confirm the current ADR/consumer-authority route after the ODR platform
  closure.
- Confirm whether the digital-content waiver applies to the Plus subscription
  at purchase or at first use.
- Confirm the refund policy for partial periods and Google Play-initiated
  refunds.
- Confirm the Spanish consumer-information wording with the abogado.
