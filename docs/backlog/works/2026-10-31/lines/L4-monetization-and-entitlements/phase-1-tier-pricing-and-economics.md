# L4 Phase 1 — Tier, Pricing, and Unit-Economics Freeze

Status: In progress (planning freeze; needs L3/L4 review)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L4 — Monetization & Entitlements](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

This freezes the commercial offer for planning. It is not tax advice. The Play
fee, VAT treatment, and Firebase prices must be re-verified before the final
L4 gate.

## 1. Frozen tier specification (v1)

| Limit | Free | Plus |
|---|---|---|
| Price | EUR 0 | EUR 3.99/month or EUR 29.99/year, VAT included |
| Cloud sends | 15/month hard cap | 1,000/month included; hard stop at 1,500 |
| Expiry | 7 days hard | Up to 14 days |
| Inbox items | 50/month hard cap | 5,000/month included; hard stop at 7,500 |
| MCP/agent | Disabled | 5,000 calls + 500 value-reads/month, hard cap |
| MCP overage | Not available | Deferred to v1.1; v1 hard-caps |
| Storage | 10 MB hard cap | 1 GB included; hard stop at 1.5 GB |
| Biometric gate | On-device | On-device |
| Teams/Family | Not available | Deferred |
| Free trial | None at launch | None at launch |

Decisions:

- **D3 applied:** MCP is included in Plus, not sold as a separate base tier.
- **D3a:** the usage overage add-on is deferred to v1.1. V1 enforces a hard cap
  so there is no metered billing, invoice reconciliation, or overage dispute
  surface at launch.
- **D3b:** Teams/Family is deferred; the schema should not block it later.
- **D3c:** no free trial or introductory offer at launch. It adds refund,
  withdrawal, and abuse complexity for a solo founder.
- **D3d:** "unlimited fair use" is replaced by numeric included allowances and
  hard caps. This is enforceable and easier to explain.

## 2. Price sheet

| Market | Monthly | Annual | Notes |
|---|---|---|---|
| Spain | EUR 3.99 | EUR 29.99 | Launch market; 21% VAT included |
| EU | EUR 3.99 | EUR 29.99 | EUR anchor at launch; no PPP pricing yet |
| UK, CH, US | Deferred | Deferred | Not launch markets |

Price rules:

- Play displays the local tax-inclusive price; Google handles consumer VAT for
  Play-distributed digital goods. Verify with the asesor.
- No hidden fees; the renewal period and cancellation path are shown before
  purchase.
- Price changes require advance notice and a new L3/L4 review.
- Refunds and withdrawal follow L3; Play-initiated refunds revoke entitlement.

## 3. Revenue per paying user

Assumptions:

- VAT 21% included in the Spanish price.
- Google Play subscription service fee 15%. Verify the current Play Console
  terms; if the fee is 30%, use the sensitivity in Section 6.
- No payment-processing fee beyond Play.
- No overage revenue in v1.

| Plan | Gross | Ex-VAT | After 15% Play fee | Monthly equivalent |
|---|---|---|---|---|
| Monthly | EUR 3.99 | EUR 3.30 | EUR 2.80 | EUR 2.80 |
| Annual | EUR 29.99 | EUR 24.79 | EUR 21.07 | EUR 1.76 |

Blended at 70% monthly / 30% annual:

`0.70 x 2.80 + 0.30 x 1.76 = EUR 2.49 per paying user per month`

## 4. Cost assumptions

Variable cloud cost is negligible at launch scale. The 2026-08-30 draft
estimated below EUR 0.001 per user per month through 100k users. Use EUR 0.002
per user per month as a conservative planning value.

Fixed cash costs for a solo founder:

| Item | Monthly | Notes |
|---|---|---|
| Gestoría/accounting | EUR 100 | S.L. bookkeeping and annual accounts |
| GCP/Firebase base | EUR 20 | Before variable usage |
| Domain, email, tools | EUR 30 | Support inbox, status page, repo |
| Insurance | EUR 50 | Cyber/PI annual premium amortized |
| Legal/one-time costs | EUR 300 | EUR 3,600/year amortized |
| Support/helpdesk | EUR 0 | Founder time |
| **Cash total** | **EUR 500** | Excludes founder opportunity cost |

Fully loaded scenario adds EUR 3,000/month for the founder's opportunity cost:

| Scenario | Monthly fixed |
|---|---|
| Cash only | EUR 500 |
| Fully loaded | EUR 3,500 |

## 5. Breakeven

Breakeven paying users = fixed cost / blended net revenue per paying user.

| Scenario | Fixed | Net per payer | Paying users | Total users at 5% conversion |
|---|---|---|---|---|
| Cash only | EUR 500 | EUR 2.49 | 201 | 4,020 |
| Fully loaded | EUR 3,500 | EUR 2.49 | 1,406 | 28,120 |

This reconciles with the 2026-08-30 draft's 20-30k range: the difference is
whether the founder's labor is treated as a cash cost. The cash-only breakeven
is about 4k users; the fully loaded breakeven is about 28k users.

## 6. Sensitivity

Total users required at 5% conversion, blended net EUR 2.49:

| Fixed cost | 3% conversion | 5% conversion | 7% conversion |
|---|---|---|---|
| EUR 500 | 6,700 | 4,020 | 2,871 |
| EUR 3,500 | 46,867 | 28,120 | 20,086 |

If the Play fee is 30% instead of 15%:

- Monthly net: EUR 3.30 x 0.70 = EUR 2.31.
- Annual net: EUR 24.79 x 0.70 = EUR 17.35/year, or EUR 1.45/month.
- Blended at 70/30: EUR 2.05 per payer per month.
- Fully loaded breakeven at 5%: 3,500 / 2.05 / 0.05 = 34,146 users.

## 7. CAC and payback guardrails

Assumptions:

- Monthly churn 5% (base), 3% (good), 7% (bad).
- Blended net revenue EUR 2.49 per payer per month.
- Target LTV/CAC at least 3.
- Target CAC payback at most 12 months.

| Churn | Average lifetime | LTV | CAC ceiling at LTV/CAC 3 | CAC ceiling at 12-month payback |
|---|---|---|---|---|
| 3% | 33 months | EUR 82 | EUR 27 | EUR 30 |
| 5% | 20 months | EUR 50 | EUR 17 | EUR 30 |
| 7% | 14 months | EUR 35 | EUR 12 | EUR 30 |

Working launch ceiling: **CAC <= EUR 15-20**, with a kill criterion if CAC
exceeds EUR 30 or payback exceeds 12 months. This is a planning guardrail, not
a market fact.

## 8. Price positioning

Reference points from the 2026-08-30 draft:

| Product | Reference price | Positioning |
|---|---|---|
| Bitwarden Premium | About USD 10/year | Cheapest vault premium |
| 1Password Individual | About EUR 36/year | Premium brand |
| Proton Pass | About EUR 5/month | Privacy brand subscription |
| Standard Notes | About USD 90/year | Encrypted notes premium |
| Shary Plus | EUR 29.99/year or EUR 3.99/month | Mid-market; below 1Password/Standard Notes, above Bitwarden |

The annual plan is the primary conversion target: it improves cash flow and
reduces churn. The monthly plan remains for trial-resistant users.

## 9. Open decisions

1. Confirm the Play subscription fee and VAT treatment with the asesor.
2. Confirm whether Google remits all consumer VAT or the S.L. must register for
   OSS.
3. Confirm the hard-cap behavior: block, queue, or require an upgrade.
4. Confirm whether an annual-only launch is simpler for the first cohort.
5. Confirm the refund/withdrawal mechanics with L3.
6. Confirm the Firebase Standard vs Enterprise pricing before the final model.
7. Confirm whether to offer a 7-day trial after the first 100 paying users.

## 10. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L4-V1 Tier spec | Section 1 | Frozen for planning |
| L4-V2 Price sheet | Section 2 | Draft |
| L4-V3 Billing products | Not created | Pending |
| L4-V4 Purchase grants entitlement | Not tested | Pending |
| L4-V5 Restore/plan changes | Not tested | Pending |
| L4-V6 Grace/hold/revoke | Not tested | Pending |
| L4-V7 Server quotas | Not implemented | Pending |
| L4-V8 MCP metering | Not implemented | Pending |
| L4-V9 Refund/withdrawal | Not implemented | Pending |
| L4-V10 Unit economics | Sections 3-7 | Draft |
| L4-V11 Tax display | Not confirmed | Pending |
| L4-V12 Abuse controls | L6 | Pending |
