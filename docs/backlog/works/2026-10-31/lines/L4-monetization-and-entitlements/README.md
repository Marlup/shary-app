# L4 — Monetization & Entitlements

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-tier-pricing-and-economics.md](phase-1-tier-pricing-and-economics.md)

## 1. Purpose

Turn the freemium model into a billing and entitlement system that can charge,
meter, and enforce the published limits without client-side bypass.

## 2. Scope

### Owned components

- C-L4-01 Frozen tier specification.
- C-L4-02 Price sheet, including EUR launch prices and regional pricing.
- C-L4-03 Google Play Billing products, base plans, and offers.
- C-L4-04 Entitlement service and source of truth.
- C-L4-05 Server-side quota enforcement.
- C-L4-06 MCP/agent metering and v1 hard cap; v1.1 overage design.
- C-L4-07 Refund, grace period, account hold, and revocation handling.
- C-L4-08 Unit economics and breakeven model.
- C-L4-09 Tax display and VAT-inclusive pricing configuration.
- C-L4-10 Abuse controls tied to entitlements.

### Explicit exclusions

- Tax registration and remittance belong to L1.
- Contract wording, withdrawal, and refund terms belong to L3.
- Growth channels and CAC research belong to L8.
- Rate-limit and credential security implementation belongs to L6.
- Analytics implementation belongs to L8.

### Interfaces

- Consumes SC1 entity identity from L1.
- Consumes SC5 retention/TTL from L5.
- Consumes SC6 deletion behavior from L6 for subscription cancellation on
  account deletion.
- Provides SC4 tier and pricing spec to L3, L5, and L8.

## 3. Baseline tier shape

These values come from the 2026-08-30 draft and are frozen only after L4 P1.

| Limit | Free | Plus |
|---|---|---|
| Cloud sends per month | 15 hard cap | 1,000 included; hard stop at 1,500 |
| Expiry | 7 days hard | Up to 14 days |
| Inbox items per month | 50 hard cap | 5,000 included; hard stop at 7,500 |
| MCP/agent | Disabled | 5,000 calls plus 500 value-reads; hard cap |
| Storage | 10 MB hard cap | 1 GB included; hard stop at 1.5 GB |
| Biometric gate | On-device | On-device |

Decision D3: MCP is included in Plus with the bounded allowance above. MCP is
not a separate base tier. Decision D3a defers the usage overage add-on to v1.1;
v1 enforces a hard cap. See
[phase-1-tier-pricing-and-economics.md](phase-1-tier-pricing-and-economics.md).

## 4. Viability definition

L4 is viable when:

1. a real license-test purchase grants Plus and a refund or expiry revokes it;
2. the server enforces the Free and Plus quotas even if the client is modified;
3. MCP calls and value-reads are metered accurately, the v1 hard cap is
   enforced, and the ledger is ready for a v1.1 overage;
4. the price sheet, tax display, and unit economics are signed off.

## 5. Section 8 coverage

- 8.1 Consumer-protection edge cases: secondary owner for refund execution.
- 8.5 Pricing and regional pricing: primary owner.
- 8.10 Growth and CAC: secondary owner for the pricing side of the model.

## 6. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Pricing and tier spec | L4-V1, L4-V2, L4-V10, L4-V11 |
| P2 | Billing integration | L4-V3, L4-V4, L4-V5, L4-V6, L4-V9 |
| P3 | Entitlement enforcement | L4-V7, L4-V8, L4-V12 |
| P4 | Revenue validation | L4-V10 final, go/no-go |

## 7. Official sources

- Google Play Billing policy and integration documentation.
- Google Play subscription best practices and license-testing guide.
- Firebase pricing documentation for Firestore and Cloud Functions, including
  the Standard and Enterprise editions.
- AEAT OSS guidance and EU VAT rules for digital services.
- GDPR Art. 5(1)(b) purpose limitation for any usage data.
- The 2026-08-30 business and monetization draft for the baseline limits.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 8. Single-owner notes

- Billing support is a single-person burden. Use Play's self-service refund and
  subscription management wherever possible, and route exceptions through L8.
- Do not build a custom card flow for Android digital goods; use Play Billing.
- Re-verify the EEA external-offers rules before assuming any alternative
  billing path is allowed.
- The overage add-on adds metering and dispute complexity. V1 defers overage
  billing and enforces a hard cap; metering still counts calls so the cap is
  enforceable and a v1.1 overage design remains possible.

## 9. Open decisions

- Confirm the Plus price: EUR 3.99/month or EUR 29.99/year as the anchor.
- Confirm the v1.1 overage design and pricing; v1 has no overage revenue.
- Confirm regional pricing for Spain versus the rest of the EU.
- Confirm the fair-use definition and the enforcement action for abuse.
- Re-verify Firestore Standard versus Enterprise pricing before the final model.
