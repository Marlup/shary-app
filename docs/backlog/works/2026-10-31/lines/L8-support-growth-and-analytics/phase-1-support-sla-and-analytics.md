# L8 Phase 1 — Support, SLA, Analytics, and Growth Workbook

Status: In progress
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L8 — Support, Growth & Analytics](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. Support model

### 1.1 Channels

- In-app "Contact support" link (mailto or a hosted form).
- Support email: `support@[domain]`.
- Security reports: `security@[domain]` (see L7).
- Status page for incidents.

One monitored inbox is enough at launch. Do not add a helpdesk the founder
cannot maintain.

### 1.2 Hours and SLA

Support hours: Monday-Friday, 10:00-18:00 CET, excluding Madrid holidays.

| Tier | First response | Resolution target | Notes |
|---|---|---|---|
| Free | 5 business days, best effort | Best effort | No paid SLA |
| Plus | 2 business days | 5 business days for normal issues | Paid support |
| Security report | 3 business days acknowledge | Triage within 10 business days | L7 policy |
| Data-subject request | Acknowledge within 5 business days | Fulfil within 1 month | GDPR |
| Billing/refund | 2 business days Plus; 5 Free | Route to Play/refund flow | L3/L4 |

No 24/7 on-call. Incidents outside hours are handled on a best-effort basis,
with the status page as the first communication.

### 1.3 Triage categories

| Priority | Examples | First action |
|---|---|---|
| P0 | Data loss, security incident, account takeover, service-wide outage | Contain, preserve evidence, notify L2/L5, use the incident runbook |
| P1 | Cannot sign in, cannot delete account, billing charged but no access | Verify identity, reproduce, route to L4/L6 |
| P2 | Bug, sync failure, MCP connection issue | Reproduce, log, schedule |
| P3 | Feature request, pricing question, documentation | Acknowledge, log, batch |

### 1.4 Macros

- Acknowledgement and expected response time.
- Request for app version, Android version, request ID, and screenshots with
  secrets redacted.
- Refund/withdrawal instructions and the Play path.
- Account deletion confirmation and what remains.
- Key-loss explanation and the erasure path.
- Security report acknowledgement.
- Incident status update.

### 1.5 Escalation

- Legal/tax: external abogado/asesor.
- Security: independent reviewer and L6.
- Continuity: L5 trustee and technical executor.
- Insurance: broker and policy incident line.

### 1.6 Metrics

- First response time.
- Resolution time.
- Open ticket count.
- Refund/withdrawal count.
- Security reports.
- Support hours per week.
- Top five issue categories.

## 2. Refund and withdrawal intake

1. The user contacts support or uses the Play refund flow.
2. Support records the order ID and purchase date.
3. If the statutory withdrawal applies, follow the L3 process.
4. If Play handles the refund, confirm that entitlement is revoked by L4.
5. Record the outcome and the reason code.
6. Never ask for the user's password or recovery secret.

## 3. Analytics specification

### 3.1 Recommendation

Do not add a third-party analytics SDK at launch. Use:

- Google Play Console: installs, store conversion, ratings, crashes/ANRs,
  Android vitals.
- Server-side aggregate counters: sends, failures, MCP grants, account deletions.
- Support ticket categories.

This avoids a consent banner, a shadow profile, and another subprocessor. If a
product analytics SDK is added later, it needs a privacy review and an L2 DPIA
update.

### 3.2 Event list

| Event | Source | Fields | Prohibited |
|---|---|---|---|
| Cloud send started | Server | channel, tier, timestamp | Email, field key/value |
| Cloud send completed | Server | outcome, recipient count, timestamp | Recipient address |
| Cloud send failed | Server | reason code, timestamp | Payload, email |
| MCP grant created | Server | scope count, tier, timestamp | Agent name, owner email |
| MCP grant revoked | Server | reason code, timestamp | Agent name |
| Subscription started | Play/server | plan, country, timestamp | Payment details |
| Subscription cancelled | Play/server | plan, reason code, timestamp | Payment details |
| Account deleted | Server | reason category, timestamp | Email, identity |
| Support ticket created | Support | category, priority, timestamp | Message content |
| Crash/ANR | Play Console | device class, app version | User ID, email |

### 3.3 Prohibited data

- Email addresses, contact names, field keys, field values, message content.
- IP addresses in analytics (rate-limit hashes are security data, not analytics).
- Advertising ID, precise location, device fingerprint.
- Recovery secrets, tokens, or payloads.

### 3.4 ePrivacy and consent

- The app itself does not need a cookie banner for local storage that is
  essential to the service.
- If the website uses analytics or cookies, add a cookie notice and consent
  where required (ePrivacy Art. 5(3), AEPD guidance).
- If a third-party analytics SDK is added, review consent, data location, and
  retention before enabling it.
- Disclose server logs and aggregate counters in the Privacy Policy.

### 3.5 Retention

- Server aggregate counters: 30 days unless needed for security.
- Support tickets: propose 12 months after resolution; L2 confirms.
- Play Console data: Google-controlled.
- Crash data: Play Console retention; no separate SDK at launch.

## 4. Localization checklist

- [ ] Spanish and English app strings reviewed by a native speaker.
- [ ] Spanish and English Privacy Policy and ToS (L2/L3).
- [ ] Spanish store listing, screenshots, and short description.
- [ ] Spanish support macros and auto-replies.
- [ ] Spanish error messages and empty states.
- [ ] Date, number, and currency formats for Spain/EU.
- [ ] Legal links and withdrawal text.
- [ ] Accessibility labels in both languages.
- [ ] No machine translation without review.
- [ ] Language switch does not lose state.

## 5. Pricing research plan

### 5.1 Segments

- Privacy-conscious Android users.
- Freelancers and small teams handling client secrets.
- Existing Bitwarden/1Password/Proton Pass users.
- Users who share fields by email today.

### 5.2 Method

- 20-30 short interviews.
- Price ladder: EUR 2.99 / 3.99 / 4.99 monthly; EUR 24.99 / 29.99 / 34.99 annual.
- Ask about willingness to pay, must-have features, and switching friction.
- Do not use fake scarcity or dark patterns.
- Record the segment, current tool, price choice, and objection.

### 5.3 Hypothesis

- Annual plan at EUR 29.99 is the primary offer.
- Monthly at EUR 3.99 is an entry option.
- MCP is a Plus differentiator, not a separate purchase.
- Bitwarden is the price anchor; 1Password and Proton Pass are the trust anchors.

## 6. CAC and growth model

### 6.1 Channels

- Privacy and Android communities.
- SEO for "encrypted vault", "secure field sharing", "MCP vault".
- Product Hunt and similar launch channels.
- Referrals from existing users.
- Partnerships with privacy-focused tools.

### 6.2 Funnel

Impression -> store visit -> install -> onboarding -> first field -> first share
-> paywall -> purchase -> renewal.

### 6.3 Guardrails

| Metric | Target | Kill criterion |
|---|---|---|
| Store conversion | At least 25% | Below 15% after two iterations |
| Activation (first field) | At least 60% | Below 40% |
| Free-to-paid conversion | 3-5% | Below 2% after 90 days |
| Monthly churn | At most 5% | Above 7% |
| CAC | EUR 15-20 | Above EUR 30 |
| CAC payback | At most 12 months | Above 18 months |
| LTV/CAC | At least 3 | Below 2 |

### 6.4 Dashboard

- Installs and store conversion (Play Console).
- Activation and first-share rate (server aggregates).
- Free-to-paid conversion and MRR (Play + server).
- Churn and renewals.
- CAC by channel.
- Support tickets per 100 users.

## 7. External actions

| Action | Owner | Status |
|---|---|---|
| Create support@ and security@ mailboxes | Founder | Pending |
| Publish the SLA page | Founder | Pending |
| Create support macros | Founder | Pending |
| Configure Play Console vitals | Founder | Pending |
| Add server aggregate counters | Founder | Pending |
| Run 20-30 pricing interviews | Founder | Pending |
| Build the CAC dashboard | Founder | Pending |
| Native review of Spanish copy | Founder/reviewer | Pending |

## 8. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L8-V1 Support channels | Section 1 | Pending |
| L8-V2 SLAs published | Section 1.2 | Draft |
| L8-V3 Support drill | Not run | Pending |
| L8-V4 Refund/withdrawal intake | Section 2 | Draft |
| L8-V5 Analytics spec | Section 3 | Draft |
| L8-V6 ePrivacy review | Section 3.4 | Pending |
| L8-V7 PII-free payload | Not tested | Pending |
| L8-V8 Spanish copy | Section 4 | Pending |
| L8-V9 Pricing research | Section 5 | Pending |
| L8-V10 CAC model | Section 6 | Draft |
| L8-V11 Dashboard | Not built | Pending |
| L8-V12 Contact consistency | Pending L2/L3 | Pending |
