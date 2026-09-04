# Shary Business, Monetization & Continuity Draft (v1)

Date: 2026-08-30
Status: Draft for planning. Not legal, tax, or accounting advice.
Scope: Monetization, legal/commercial setup (Spain/Madrid), data-usage limits by
user type, cost model by scale, cloud-infrastructure maintenance, and
continuity/termination protocols.

> **Disclaimer.** Legal and tax figures are summarized from the named EU and
> Spanish regulations and are accurate to the best of current knowledge, but
> they must be validated by a Spanish *asesor* / abogado before commercial
> launch. Cloud cost figures are planning estimates based on published list
> prices; verify current pricing (Firestore now has Standard and Enterprise
> editions) before finalizing a business case.

---

## 1. Product intent recap

Shary is an Android-first, offline-first encrypted vault for structured
"Fields" (small sensitive data items) with selective sharing via **Email**
(free, local share sheet) or an **encrypted cloud relay** (Firebase: Firestore +
Cloud Functions), plus an **OAuth-gated MCP/agent** access layer. Cloud payloads
are end-to-end encrypted per recipient and expire after 7–14 days. Core vault
and email sharing require no online registration; the cloud relay and MCP
require a verified email identity.

This "local vault free / cloud relay + agent connectivity premium" shape is the
commercial foundation of this draft.

---

## 2. Monetization strategy

### 2.1 Recommendation

**Freemium subscription as the core, with a thin usage-based tier only for the
agent/MCP dimension.** Do not use pay-per-usage as the primary model: the
marginal cloud cost per user is near zero (Section 6), so per-event metering
adds friction without recovering meaningful cost. Usage-based pricing is
justified only where there is genuine variable cost or enterprise willingness to
pay — in Shary that is the MCP/agent API, not the relay.

| Tier | Price anchor | Includes |
|---|---|---|
| **Free** (anonymous local vault) | $0 | Full local vault (fields, contacts, tags, search, history), email sharing, offline-first, 7-day expiry only, capped cloud relay (~15 sends/mo), no MCP |
| **Shary Plus** | ~$3.99/mo or ~$29.99/yr | Unlimited cloud relay, 14-day expiry, larger inbox, biometric-gate extras, bounded MCP/agent access |
| **Shary Teams/Family** (later) | ~$7.99/mo | Shared recipient groups, admin, per-member grants, audit |
| **Agent usage add-on** (optional) | pay-per-usage | Overage beyond the MCP call allowance (per 1K calls / active agent connection) |

### 2.2 Pros / cons of each model

| Model | Pros | Cons | Verdict |
|---|---|---|---|
| **Freemium subscription** | Recurring, predictable revenue; fits vault category (Bitwarden, Standard Notes); offline vault stays genuinely free/anonymous | Requires billing infra (Play Billing) and a clear free→paid upsell; churn management | **Recommended core** |
| **Pay-per-usage (relay)** | Fair, "pay what you use"; low entry | Marginal cost is ~$0; metering friction on a privacy product; unpredictable revenue | Reject as primary |
| **Usage-based (MCP/agent only)** | Matches real variable cost + enterprise value (Claude/OpenAI model); scales with automation | Needs accurate metering, quotas, rate limits; abuse surface | **Recommended add-on** |
| **One-time purchase / unlock** | Simple; no recurring billing | No recurring revenue; upgrade path limited; Play policy pressure toward subs | Reject as primary |
| **B2B / enterprise license** | High ARPU; SSO/teams | Needs sales motion, SLAs, SOC 2/ISO 27001; premature for V1 | Later |
| **Donations / pay-what-you-want** | On-brand for privacy (Signal/Proton model); zero billing friction | Unreliable revenue; not a business | Optional complement |
| **Data monetization / ads** | High theoretical revenue | Violates GDPR purpose limitation + the product's trust promise; brand-killer | **Forbidden** |

### 2.3 Anchor references (category theory)

| Product | Model | Why it maps to Shary |
|---|---|---|
| **Obsidian** | Free local app; paid **Sync/Publish** relay | Closest analog: local vault free; the cloud service is the paid line |
| **Bitwarden** | Free core vault; Premium ~$10/yr | Vault freemium with cheap premium works |
| **Standard Notes** | Free notes; subscription for encrypted sync | Tiering on encrypted relay + advanced features |
| **1Password** | Individual/Family subscription | Family/team expansion path |
| **Proton Drive/Pass** | Storage/feature quota tiers | Quota framing for a privacy brand |
| **Claude/OpenAI APIs** | Usage-based | Basis for the MCP/agent metering add-on |

Primary official references: Google Play Commerce / subscription best practices,
Google Play Billing policy (digital subscriptions must use Play Billing), the
Firestore billing docs (cost floor), and the GDPR purpose-limitation principle.

---

## 3. Legal framework — Spain / Madrid

### 3.1 Applicable regulation

| Regulation | What it governs for Shary |
|---|---|
| **GDPR — Reglamento (UE) 2016/679** | Core data-protection rules; Arts. 5 (principles), 6 (lawful basis), 9 (special categories), 15–22 (data-subject rights), 30 (records), 32 (security), 33/34 (breach notification), 35 (DPIA) |
| **LOPDGDD — Ley Orgánica 3/2018, de 5 de diciembre** | Spanish implementation; age of consent **14**, domestic sanctions regime, national specifics |
| **AEPD — Agencia Española de Protección de Datos** | Supervisory authority (registers DPOs, sanctions, guidance) |
| **LSSI-CE — Ley 34/2002, de 11 de julio** | Information-society services; disclosure duties, terms, advertising |
| **TRLGDCU — RDL 1/2007 (consumer law)** | B2C: contract transparency, **14-day right of withdrawal** for distance sales (subscriptions), complaint handling |
| **ePrivacy (Ley 34/2002 art. 22 + EU Reg 2017/679)** | Cookies/tracking — minimal for Shary, but relevant to any website/analytics |
| **NIS2 (Dir. UE 2022/2555; transposition ongoing)** | Cybersecurity duties — likely out of scope at small scale, but monitor |
| **TRLC — RDL 1/2020 (Ley Concursal)** | Insolvency — relevant to the bankruptcy protocol (Section 9) |

Territorial note: if the company is established in Spain, GDPR + LOPDGDD apply
to all data processing regardless of where users live (GDPR Art. 3).

### 3.2 Data-protection specifics that work in Shary's favor

- **End-to-end encryption** (server never sees plaintext) is a strong Art. 32
  security measure and reduces the Art. 34 breach-notification scope (encrypted
  data = often no obligation to notify affected users). This is a real, defensible
  legal advantage — preserve it as non-negotiable.
- **Data minimization / storage limitation** (Art. 5): the 7–14-day TTL
  auto-deletion already aligns with storage limitation; document it in the ROPA.
- **On-device biometric gate** (Android BiometricPrompt): on-device biometrics
  that never leave the device are generally not "processing" of biometric data
  by the app, but flag this in the DPIA to preempt AEPD scrutiny (Art. 9 is
  strict).
- **Roles**: Shary company = data **controller**; Google/Firebase = data
  **processor**. A Data Processing Agreement (DPA) is required (Firebase provides
  one); pin the Firestore/Functions to the **EU region** to simplify transfer
  compliance (avoid SCCs/TIA if possible).
- **Lawful basis (Art. 6)**: *contract* for the paid service, *consent* for
  marketing only, *legitimate interest* for security/anti-abuse.
- **Deliverables**: bilingual Privacy Policy + ToS, ROPA (Art. 30), breach
  procedure, data-subject rights mechanism (export/delete — easy because data is
  on-device), and a DPIA for biometric + MCP.

### 3.3 Company / business types (Spain)

| Type | Liability | Capital | Tax | Fit for Shary |
|---|---|---|---|---|
| **Autónomo (self-employed)** | Unlimited personal | none | IRPF (progressive) + RETA (social security) | Fast/cheap start; personal risk on closure/bankruptcy |
| **S.L. (Sociedad Limitada)** | Limited to capital | €1 min (post-2022) | Impuesto de Sociedades 25% (15% first two periods for new cos) | **Recommended** — limits personal liability, essential given the closure/bankruptcy protocols |
| **S.L.N.E. (Nueva Empresa)** | Limited | €1–€120k | IS as above | Simplified S.L. if single-shareholder startup |
| **S.A.** | Limited | €60k | IS 25% | Only for larger/raise scale |
| **Cooperativa / Sociedad Civil** | varies | n/a | varies | Rarely suited to a venture product |

Recommendation: **S.L.** registered in Madrid, single or few shareholders. The
limited liability directly protects the owner in the bankruptcy scenario the
user asks about (Section 9.4).

### 3.4 Tax basics (Spain)

- **IVA 21%** on digital subscriptions sold to Spanish/EU consumers; use the
  **OSS (One Stop Shop)** for EU cross-border VAT if you go through Stripe/web.
  If you use **Google Play Billing**, Google collects/handles most consumer VAT
  for Play Store sales.
- **Impuesto de Sociedades** 25% (15% for the first two periods of new
  companies) vs IRPF for autónomo.
- **IAE** (economic activities tax) — effectively exempt below thresholds.
- Retention of invoices/records (Código de Comercio / Ley 58/2003).

---

## 4. Minimal vs advanced aspects to be businessable

### 4.1 Minimal (must-have before charging money)

1. Legal entity (S.L.) + NIF + IVA registration (or OSS).
2. Bilingual Privacy Policy + ToS (GDPR/LSSI/TRLGDCU), including the 14-day
   withdrawal right and the zero-knowledge key-custody disclosure.
3. DPA with Google/Firebase; Firestore/Functions pinned to EU region.
4. ROPA + basic Art. 32 security baseline (already strong: E2E, on-device keys).
5. Breach-notification procedure (Art. 33/34).
6. Data-subject rights: in-app export/delete + support email.
7. Billing via **Google Play Billing** (digital subscriptions must use it).
8. Basic abuse controls: App Check / rate limiting (429 already handled in the
   backend contract).
9. Support channel + refund/withdrawal handling.

### 4.2 Advanced (as it scales / B2B)

1. DPIA for biometric gate and MCP/agent access (document, not just run).
2. DPO (appoint when processing is large-scale or special-category; otherwise
   AEPD guidance — good practice regardless).
3. ISO 27001 / SOC 2 (B2B trust, MCP enterprise).
4. Penetration test, bug bounty / responsible-disclosure policy.
5. Cyber-liability insurance.
6. Cookie/ePrivacy + analytics compliance for any web presence.
7. Trademark registration ("Shary") + brand/IP protection.
8. EU Accessibility Act (EAA) compliance for e-commerce surfaces.
9. CCPA if serving US users.
10. Localization (Spanish first — Madrid market).

---

## 5. Hard frozen limits & data usage by user type

### 5.1 Frozen limits (enforced server-side + client, not just UI)

| Limit | Free | Plus |
|---|---|---|
| Cloud sends / month | **15** | Unlimited (fair-use ~1,000) |
| Expiry | 7 days hard | Up to 14 days |
| Inbox items / month | 50 | Unlimited |
| MCP / agent | Disabled (or metadata-only 50) | 5,000 calls + 500 value-reads/mo |
| Storage cap | 10 MB fair-use | 1 GB (or unlimited fair-use) |
| Biometric gate | On-device, available | On-device, available |

**Hard-frozen principles (non-negotiable, encoded in contract):**
1. **E2E encryption always on** — the server never receives or stores plaintext.
2. **No sale / no ads / no profiling** of vault content — data is used only to
   deliver the service.
3. **Expiry is hard-deleted** by the scheduler (`clean_expired_docs`); no
   soft-retention of ciphertext beyond TTL.
4. **Zero-knowledge key custody** — the user's device holds the private keys; the
   company cannot decrypt or recover them. Key/device loss = data loss, disclosed
   in ToS (consumer-protection requirement, TRLGDCU).
5. **No cross-user access** — recipient isolation is enforced by the idempotent
   per-recipient decision lifecycle already in the backend freeze brief.

### 5.2 Data usage by user type

- **Free**: vault metadata only for local operation; cloud ciphertext stored
  transiently to relay shares, deleted on TTL. No marketing profiling beyond
  lawful analytics.
- **Plus/Teams**: same handling; the tier changes *quota* and *expiry*, not data
  use. No additional data collection beyond what billing requires (Play order
  data).
- **MCP/agent**: grant metadata and audit records are retained locally/by the
  owner; backend sees only encrypted operations. Writes remain approval-gated.

---

## 6. Cost model by scale

### 6.1 Assumptions

- Blended average usage between the optimistic/typical/power profiles from the
  earlier investigation: ~500 Firestore ops (reads+writes), ~100 Cloud Function
  invocations, ~60 KB steady-state storage per user/month.
- Firestore list prices: reads $0.06/100K, writes $0.18/100K, deletes
  $0.02/100K, storage $0.18/GiB-mo (US multi-region). Cloud Functions ~$0.40/M
  invocations + compute. **Verify current pricing (Standard vs Enterprise
  editions).**
- Fixed operational costs are Madrid-based planning estimates and **dominate**
  at every scale.

### 6.2 Consolidated monthly run cost (estimate)

| Users | Cloud (variable) | Ops/labor | Support | Legal/accounting | Insurance+infra | **Total/mo** | Per-user/mo |
|---|---|---|---|---|---|---|---|
| 1 | < $0.01 | $1,500 | $0 | $200 | $120 | **~$1,820** | $1,820 |
| 100 | ~$0.05 | $1,500 | $50 | $200 | $120 | **~$1,870** | ~$19 |
| 1,000 | ~$0.50 | $2,000 | $200 | $300 | $150 | **~$2,650** | ~$2.65 |
| 10,000 | ~$7 | $4,000 | $800 | $500 | $300 | **~$5,600** | ~$0.56 |
| 100,000 | ~$75 | $12,000 | $4,000 | $1,000 | $700 | **~$17,800** | ~$0.18 |
| 1,000,000 | ~$1,000 | $40,000 | $20,000 | $2,500 | $1,500 | **~$65,000** | ~$0.065 |

**Key finding:** cloud marginal cost stays below ~$0.001/user/mo through 100k
users; the real cost is **labor + operations + support**. Per-user total cost
falls from ~$1,820 (n=1) to ~$0.065 (n=1M) — strong economies of scale.

### 6.3 Breakeven note

At $3.99/mo Plus with ~4–5% free→paid conversion, ~10k users yield ~$1,600/mo
revenue against ~$5,600/mo cost (not breakeven). Breakeven is roughly **20–30k
users at 5% conversion**, or earlier with higher ARPU (Teams + MCP add-on) or
lower fixed cost. The MCP usage add-on and Teams tier are the levers that raise
ARPU before mass user growth.

---

## 7. Critical cloud-infrastructure maintenance (what to cover)

This is what *you* must own on top of (and independent of) the Google Cloud SLA.

1. **Backups + tested restore** — Firestore has scheduled backups and
   point-in-time recovery (PITR); enable them, set retention, and run a restore
   drill. Firestore does not back itself up by default.
2. **Monitoring & alerting** — error rate, latency, Function cold starts, auth
   failure rate, scheduler-job failures, and **quota burn** (near free-tier/daily
   caps) alerts.
3. **Observability** — structured logs, `X-Request-Id` propagation (already in
   the contract), tracing for decision endpoints.
4. **Capacity / quota management** — monitor reads/writes/deletes against
   Firestore quotas; plan index strategy for `recipient` + `expires_at` queries.
5. **Cost controls** — budget alerts, per-project billing, anomaly detection.
6. **Security ops** — least-privilege IAM, Firestore security rules review,
   secrets/keys rotation, no secrets in code (already policy), App Check to stop
   abuse.
7. **CI/CD + rollback** — staged deploys, contract-test gate (already in the
   freeze brief), canary, rollback runbook.
8. **Incident management** — on-call rotation, runbooks, status page.
9. **Vendor lock-in mitigation** — the `CloudService` interface already abstracts
   the relay; keep a documented export format and a migration path.
10. **Business-continuity plumbing** — TTL/scheduler correctness, DR plan,
    `X-CloudScheduler` guard (already present).

### 7.1 Existential risks (highest priority)

- **Losing the Android signing key / Play signing** — unrecoverable for app
  updates. Store it securely (Play App Signing + hardware key backup).
- **Billing lapse** — a failed credit card on the Firebase/GCP project silently
  stops the service. Keep a backup payment method + budget alerts.
- **Losing the Firebase project** — protect ownership/admin accounts with MFA.
- **Firestore region lock-in** — a region choice is hard to change later; choose
  the EU multi-region deliberately.

---

## 8. Other aspects I suggest you are missing

1. **Consumer-protection edge cases** — 14-day withdrawal on EU subscriptions,
   refund policy, and the "lifetime data loss" disclosure for zero-knowledge key
   custody (TRLGDCU).
2. **Google Play policy surface** — Play Billing requirement for digital subs,
   the **Data Safety form**, and the **in-app account deletion** requirement.
3. **Dispute resolution** — EU ODR platform + ADR for consumer complaints.
4. **Support & SLAs** — define a support response commitment per tier (esp. for
   Teams/B2B).
5. **Pricing psychology & regional pricing** — Madrid/EU first, then EU-wide,
   then global.
6. **Localization** — Spanish as the launch language (LSSI requires Spanish
   consumer terms).
7. **Insurance** — cyber-liability + professional indemnity.
8. **IP/branding** — trademark "Shary"; decide open-source vs closed licensing
   (the repo may already be public).
9. **Privacy-safe analytics** — aggregate, no fingerprinting; ePrivacy compliant.
10. **Growth/CAC** — customer-acquisition cost is often the real financial risk
    in a freemium product, not cloud cost.
11. **Accessibility (EAA)** — European Accessibility Act obligations for
    e-commerce.
12. **Backup of your own business records** — accounting, ROPA, DPA, and the
    breach register, retained per legal periods.
13. **Succession / data stewardship clause** — a named person/process who can
    act on user data if the company is dissolved (ties to Section 9).

---

## 9. Continuity & termination protocols

### Protocol 1 — Service interruption (outage / disruption)

1. **Detect** via monitoring (latency/error/Function-cold-start alerts).
2. **Classify** severity; page the on-call owner.
3. **Contain** — rollback last deploy or disable the failing path (feature
   flag/endpoint gate); the app already degrades to offline-first, so vault use
   is unaffected — communicate this.
4. **Communicate** — status page + in-app `CloudStatusStrip` (already exists).
5. **Restore** — from backup/PITR if data-affecting.
6. **RTO/RPO targets** — propose RTO 4h, RPO 24h for the relay (data is
   rebuildable from devices; ciphertext is transient by design).
7. **Postmortem** + backlog item; do not change unrelated code.

### Protocol 2 — Data breach (compromise)

1. **Contain** — revoke/rotate affected keys, cut the compromised surface.
2. **Assess** — determine whether personal data was exposed; note E2E
   ciphertext is likely to downgrade severity.
3. **Notify AEPD within 72h** (GDPR Art. 33) if notifiable.
4. **Notify users** (Art. 34) if high risk — with a pre-drafted template.
5. **Preserve evidence** and run forensics; record in the breach register.
6. **Remediate** the root cause and re-test.

### Protocol 3 — Solvent wind-down (planned closure)

1. **Advance notice** (e.g., 60–90 days) to all users via app/email.
2. **Export window** — users export their local vault (it is already on their
   device; cloud ciphertext is transient and self-purges by TTL).
3. **Stop new charges** — cancel Play subscriptions; process refunds per policy.
4. **Shutdown sequence** — freeze uploads, drain inbox, stop scheduler, then
   delete Firestore data and backups after the retention window.
5. **Deregister** — delist from Play, close DPA records, notify AEPD of the
   controller's cessation.

### Protocol 4 — Insolvency / bankruptcy (Ley Concursal)

1. **Key-custody reality** — E2E design means the company *cannot* decrypt user
   data; users' vault data lives on their own devices, so **user data is safe by
   design** in bankruptcy. This is a documented advantage.
2. **Ciphertext** — only the transient relay ciphertext is affected; it expires
   via TTL and is deleted by the administrator.
3. **Data-stewardship clause** in ToS — a named trustee/admin action to notify
   users and permit export before data destruction.
4. **Administrator handover** — the insolvency administrator (TRLC) takes over
   the Firebase project/keys under the DPA; the plan documents the handover.
5. **Communication** — notify users via the retained channel even if the app is
   discontinued.

### Protocol 5 — Key / credential compromise & forced takeover

1. **Signing-key loss** — recover via Play App Signing backup; if unrecoverable,
   issue a new app listing (worst case) and notify users to export.
2. **Backend key/secret compromise** — rotate the Firebase service account /
   Firestore credentials immediately.
3. **Account takeover (admin)** — revoke sessions, enforce MFA, audit Cloud
   Audit Logs.
4. **Revocation/kill-switch** — revoke MCP grants and rotate user identity keys
   (the `/v2/identity/rotate` path already exists).
5. **Communicate** transparently per Art. 33/34.

---

## 10. Open decisions / next steps

1. Confirm Plus price and whether MCP lives in Plus or as a separate add-on.
2. Confirm the "no registration" promise boundary: local vault anonymous; cloud
   + MCP require verified email (recommended as the free→paid line).
3. Validate legal/tax details with a Madrid *asesor* / abogado.
4. Re-verify Firestore/Cloud Functions list prices (Standard vs Enterprise
   edition) before finalizing Section 6.
5. Decide launch scope: Android-only now, iOS/web later (affects billing + EAA).
