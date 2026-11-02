# L8 — Support, Growth & Analytics

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-support-sla-and-analytics.md](phase-1-support-sla-and-analytics.md)

## 1. Purpose

Support paying users at a pace one person can sustain, measure growth without
surveilling users, and validate pricing and acquisition assumptions before
scaling spend.

## 2. Scope

### Owned components

- C-L8-01 Support channels and intake.
- C-L8-02 Support SLAs by tier.
- C-L8-03 Refund and withdrawal intake.
- C-L8-04 Support macros and escalation runbook.
- C-L8-05 Privacy-safe analytics specification.
- C-L8-06 ePrivacy and consent review for analytics.
- C-L8-07 Spanish localization and launch copy.
- C-L8-08 Regional pricing research.
- C-L8-09 CAC, funnel, and kill-criteria model.
- C-L8-10 Growth dashboard and reporting.
- C-L8-11 Support-contact consistency across policy, ToS, and store.

### Explicit exclusions

- Pricing execution and entitlements belong to L4.
- Legal complaint terms and ADR path belong to L3.
- Analytics DPIA and ROPA belong to L2.
- Accessibility audit belongs to L7.
- Store listing compliance belongs to L3.

### Interfaces

- Consumes SC2 ROPA from L2 for the analytics data map.
- Consumes SC4 pricing spec from L4 for research and the funnel model.
- Consumes SC8 support contact and SLA as the source of truth.
- Provides SC8 to L2 and L3 for published contact and complaint intake.
- Consumes L3 complaint path for escalation.

## 3. Viability definition

L8 is viable when:

1. a support request is acknowledged and resolved within the published SLA;
2. refund and withdrawal intake works with L4;
3. analytics collect only aggregate, non-identifying events, with consent where
   required;
4. the Spanish launch copy is reviewed;
5. the CAC and breakeven model has explicit assumptions and kill criteria.

## 4. Section 8 coverage

- 8.4 Support and SLAs: primary owner.
- 8.5 Pricing and regional pricing: secondary owner for research.
- 8.6 Localization: secondary owner for product copy.
- 8.9 Privacy-safe analytics: primary owner.
- 8.10 Growth and CAC: primary owner.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Support and SLA | L8-V1, L8-V2, L8-V3, L8-V4 |
| P2 | Analytics and localization | L8-V5, L8-V6, L8-V7, L8-V8 |
| P3 | Pricing research and CAC | L8-V9, L8-V10, L8-V11, L8-V12 |

## 6. Official sources

- Directive 2002/58/EC (ePrivacy), especially Art. 5(3) for storage and access.
- AEPD guidance on cookies, analytics, and privacy-safe measurement.
- Ley 34/2002 (LSSI-CE) for information-society duties and Spanish consumer
  information.
- Google Play pricing and localization documentation.
- TRLGDCU price transparency rules and Directive (EU) 2019/2161 for price
  reductions.
- AEAT OSS guidance for EU digital-service pricing.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- Do not promise 24/7 support. Use Free: best effort within 5 business days;
  Plus: within 2 business days. Publish the support hours and holidays.
- Use one monitored inbox and a simple ticket view; do not add a helpdesk the
  founder cannot maintain.
- Analytics must not become a shadow profile. Prefer aggregate counters,
  crash reports without identifiers, and store-provided metrics.
- Spanish copy needs a native review, not machine translation.
- CAC is the real financial risk in a freemium product. Set a kill criterion
  before spending on acquisition.

## 8. Open decisions

- Choose the support inbox and whether to use a lightweight helpdesk.
- Confirm the no-third-party-SDK decision for launch; use Play Console and
  server aggregate counters. Revisit only with an L2 privacy review.
- Confirm whether a cookie banner is needed for the app (usually not) and for
  the website (likely yes).
- Confirm the launch channels and the CAC ceiling.
- Confirm the Spanish and English copy review process.
