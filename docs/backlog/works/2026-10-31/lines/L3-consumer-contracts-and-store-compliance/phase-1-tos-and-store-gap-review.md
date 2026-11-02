# L3 Phase 1 — ToS and Store-Compliance Gap Review

Status: In progress (draft for abogado review)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L3 — Consumer Contracts & Store Compliance](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

This review compares the existing drafts and the current app behavior. It is
not legal advice. The abogado must approve the final Spanish and English text
before publication.

## 1. Inputs reviewed

- `docs/backlog/works/2026-08-30/terms-of-service-draft.md`
- `docs/backlog/works/2026-08-30/privacy-policy-draft.md`
- `docs/backlog/works/2026-08-30/ropa-draft.md`
- Play Console Help, "Understanding Google Play's app account deletion
  requirements", retrieved 2026-10-31.
- App behavior: no in-app account-deletion UI; encrypted vault export/import is
  implemented; email sharing produces a readable JSON attachment; Android Auto
  Backup is disabled.
- Backend behavior: `/v2/identity/delete` requires a signature from the current
  signing key; there is no email-verified erase path yet.

## 2. Terms of Service gap review

| # | Clause | Current draft | Gap | Required fix | Owner |
|---|---|---|---|---|---|
| 1 | Provider identity | `[Legal entity name, NIF, address, email]` | Placeholders | Fill from SC1 after L1 P1 | L3 |
| 2 | Service description | Local vault, cloud relay, MCP | Does not state the current tier limits, MCP hard cap, or store billing | Add the frozen L4 spec and a link to the price page | L3/L4 |
| 3 | Zero-knowledge claim | "Identity keys are derived from your password and stored only on your device" | Keys are derived on demand; the encrypted credentials blob is stored on device; Android backup may copy data; contacts/settings are plaintext | Rewrite precisely: what is encrypted, what metadata is stored, what can leave the device, and what happens on backup/restore | L3/L2/L6 |
| 4 | No recovery | "No recovery" | Conflicts with the planned option C (email-verified erase + offline-secret rotate); erasure is not recovery of vault content but must be explained | Distinguish erasure from key recovery; disclose the offline recovery secret and its limits | L3/L6 |
| 5 | Subscription terms | "Paid features (if any) are billed through Google Play" | No price, renewal period, cancellation, trial, price-change, or refund mechanics | Add the L4 price sheet and Play subscription terms | L3/L4 |
| 6 | Withdrawal right | 14 days; "where the service has begun with your consent you may be charged proportionally" | The digital-content waiver mechanics are not defined; "proportional charge" may not match the legal model | Draft the express-consent and acknowledgement flow at purchase; align with the abogado | L3/L4 |
| 7 | Refunds | Not specified | No refund policy beyond the withdrawal right | Add the Play refund process and the provider's discretionary policy | L3/L4 |
| 8 | Acceptable use | Generic | No MCP-specific limits, no abuse escalation, no suspension process | Add MCP scope/abuse rules and a suspension/termination process | L3/L6 |
| 9 | IP | "App and branding owned by Provider" | No open-source licence statement, no trademark notice, no user-content licence | Add the L7 licensing decision and a user-content clause | L3/L7 |
| 10 | Liability | "As is" to the maximum extent permitted | A broad "as is" clause may be unfair in Spanish consumer contracts; zero-knowledge data loss needs a specific allocation | Have the abogado rewrite the liability and data-loss allocation | L3 |
| 11 | Data stewardship | References the Privacy Policy | No named trustee, no notice period, no export window, no insolvency commitment | Add the L5 stewardship clause and the wind-down commitments | L3/L5 |
| 12 | Disputes | References the EU ODR platform | The ODR platform closed on 20 July 2025; the clause points to a dead route | Replace with the current ADR/consumer-authority route; verify with the abogado | L3 |
| 13 | Spanish version | English only | Spanish consumers need Spanish terms and LSSI information | Produce a Spanish version reviewed by a native lawyer | L3/L8 |
| 14 | Changes | Continued use constitutes acceptance | May be unfair for material changes | Add notice and a right to terminate before the change takes effect | L3 |
| 15 | Contact | `[support email]` | No monitored channel or SLA | Use the L8 support contact and SLA | L3/L8 |

## 3. Privacy Policy gap review

| # | Topic | Current draft | Gap | Required fix | Owner |
|---|---|---|---|---|---|
| 1 | Controller | Placeholder | No entity | Fill from SC1 | L2 |
| 2 | Local vault | "Vault content never leaves your device in readable form" | Contacts and settings are plaintext; Android backup can copy ciphertext and plaintext metadata | Qualify the claim and decide the backup policy | L2/L6 |
| 3 | Key storage | "On-device key storage, hardware-backed key wrapping" | Only the credentials blob is Keystore-wrapped; identity keys are derived, not hardware-backed | Rewrite the security claim precisely | L2/L6 |
| 4 | Android backup | Not mentioned | `allowBackup="true"` with incomplete exclusions | Disclose or disable; add the ROPA row | L2/L6 |
| 5 | Email sharing | Not mentioned | Readable attachment; recipients may see each other; the chosen app/provider receives the data | Add a clear disclosure before the share | L2/L3/L8 |
| 6 | Cloud relay metadata | Mentions ciphertext and hashes | Does not list sender/recipient hashes, request metadata, signatures, timestamps, or expiry plainly | Add a metadata table | L2 |
| 7 | MCP data | Grant metadata, operation IDs, device ID, scopes | Does not mention FCM token, agent profile, OAuth client, token hashes, KMS, or 30-day grants | Add the MCP data categories and retention | L2/L6 |
| 8 | Billing | Not mentioned | Google Play purchase/subscription data | Add Google Play as a processor/merchant and the purchase data | L2/L4 |
| 9 | Support | "Whatever you send us" | No retention period or provider | Define the support channel, provider, and retention | L2/L8 |
| 10 | Transfers | "EU hosting; no transfers outside EEA except..." | Firebase Auth/FCM may be global; no SCC/TIA analysis | Complete the transfer analysis and name the mechanism | L2 |
| 11 | Rights | Lists rights; contact by email | Export exists; no in-app account deletion; lost-key erasure missing | Implement deletion and describe the rights process | L2/L6 |
| 12 | Age | "Not directed at children under 14" | No age affirmation at sign-up | Add an age affirmation if the abogado requires it | L2/L3 |
| 13 | DPO | Not mentioned | No DPO assessment | Record the assessment | L2 |
| 14 | Retention | 7-14 days for relay; identities until deletion | Logs, support, backups, MCP grants, and devices are not covered | Publish the full retention matrix | L2 |
| 15 | Subprocessors | Not listed | No current list | Publish the L2 list on the trust page | L2/L7 |

## 4. Google Play Data Safety draft

This is a planning draft. The final answers must be entered in Play Console and
must match the ROPA and the SDK inventory.

| Data type | Collected? | Shared? | Purpose | Optional? | Encrypted in transit? | Deletion |
|---|---|---|---|---|---|---|
| Email address | Yes, for account and identity | Processor only (Google/Firebase) | Account, security, support | Required for cloud | Yes | Account deletion plus support |
| Name | Local contacts only; not sent to Shary | No | Local contact management | Optional | N/A | Local delete |
| Purchase history | Yes, through Google Play | Google Play processor/merchant | Billing and entitlements | Required for paid | Yes | Google Play plus account deletion |
| App interactions | Server logs and rate-limit counters; no analytics SDK at launch | Processor only | Security, reliability | Required for service | Yes | Retention policy |
| Device or other IDs | FCM token, device ID for MCP, Firebase installation ID if present | Processor only | MCP device binding and notifications | Optional for MCP | Yes | Grant/device removal |
| Crash logs and diagnostics | Not yet configured; Play Console vitals only | Google Play | Reliability | N/A | Yes | Google Play |
| Files and docs | Encrypted vault ciphertext; local contacts not sent | Processor only for ciphertext | Cloud relay | Optional | Yes | TTL plus account deletion |
| Messages | Support email only | Support provider | Support | Optional | Yes | Support retention |

Open Data Safety questions:

1. Does Play treat Google/Firebase as a "service provider" rather than a
   third-party "sharing" recipient? Confirm in Play Console guidance.
2. Is the MCP device ID declared as a device identifier or as an app-specific ID?
3. Is the email address "required" or "optional" given the local-only mode?
4. Does the app collect "app interactions" if only server logs are used?
5. Is the FCM token declared?
6. Does the app use any advertising ID or analytics SDK? Currently no.

## 5. Account deletion checklist

Play requires both an in-app deletion path and a functional web link when the
app allows account creation anywhere in the experience. The current app has
neither.

Required work:

1. Add a Settings entry "Delete account and cloud data".
2. Explain what is deleted: cloud identity, MCP grants/devices, decisions,
   ciphertext, support records; what is retained for legal/tax reasons; what
   stays on the device until the user deletes it.
3. Require a strong confirmation; offer export before deletion.
4. Cancel or explain the active subscription; Play subscriptions must be
   cancelled in Play.
5. Implement the lost-key path (email-verified erase) so a user without the old
   signing key can still delete the cloud identity.
6. Provide a web page at a stable URL that can submit a deletion request without
   reinstalling the app.
7. Record the request and completion evidence.
8. Reflect the behavior in the Privacy Policy and Data Safety answers.

The current `/v2/identity/delete` endpoint is not sufficient: it requires a
signature from the key the user may have lost.

## 6. Withdrawal and refund mechanics

The abogado must confirm the final model. The working plan:

- Show the 14-day withdrawal right before purchase.
- If the user wants immediate access, capture express consent to begin the
  service and acknowledgement that the withdrawal right is lost or limited.
- For digital content/services, the waiver must be explicit and recorded.
- If the waiver is not valid, allow withdrawal and refund the unused period.
- Route Play purchases through Play's refund flow; document the provider's
  discretionary refund policy for cases outside the statutory right.
- Do not describe a "proportional charge" unless the abogado confirms it fits
  the chosen legal model.

## 7. Dispute and ADR update

The EU ODR platform closed on 20 July 2025. Regulation (EU) 2024/3228 repealed
Regulation (EU) 524/2013. The current ToS clause points to a dead route.

Working replacement, to be confirmed by the abogado:

- Complaints first through the L8 support channel, with a response target.
- If unresolved, the consumer may use the Junta Arbitral de Consumo de Madrid or
  the competent consumer authority.
- For cross-border disputes, the European Consumer Centre can assist.
- The consumer always retains the right to go to court.
- No clause may limit mandatory consumer rights.

## 8. Spanish consumer information

LSSI Art. 10 requires service-provider information. The Spanish launch needs:

- Legal name, NIF, registered address, contact email.
- Clear description of the service and price.
- Spanish terms and privacy policy.
- Complaint and ADR information.
- Language of the contract.
- Technical steps to conclude the contract and correct errors.
- Storage of the contract and access conditions.

## 9. Launch gate

Do not submit the store listing until:

- the abogado signs the Spanish and English ToS and Privacy Policy;
- the Data Safety answers trace to the ROPA;
- in-app account deletion and the web deletion link work;
- the withdrawal/refund flow is defined and implemented;
- the ODR/ADR text is updated;
- the zero-knowledge and backup claims match the implementation.

## 10. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L3-V1 ToS published | Existing draft | Pending |
| L3-V2 Withdrawal/refund | Section 6 | Draft |
| L3-V3 Zero-knowledge disclosure | Section 2 rows 3-4 | Draft |
| L3-V4 Complaint/ADR | Section 7 | Draft |
| L3-V5 Data Safety | Section 4 | Draft |
| L3-V6 Account deletion | Section 5 | Missing |
| L3-V7 Billing declaration | Existing flow | Pending |
| L3-V8 Content rating | Not started | Pending |
| L3-V9 Spanish consumer information | Section 8 | Pending |
| L3-V10 Public claims match behavior | This review | Gap |
| L3-V11 Store listing approved | Not submitted | Pending |
| L3-V12 Store accessibility | L7 | Pending |

## 11. Questions for the abogado

1. Confirm the digital-content waiver wording and the purchase-flow consent.
2. Confirm whether a proportional charge is lawful for partial periods.
3. Confirm the current ADR/consumer-authority route after the ODR closure.
4. Confirm whether the liability and "as is" clauses are enforceable in Spain.
5. Confirm whether an age affirmation is required for a 14+ service.
6. Confirm the Spanish consumer-information checklist.
7. Confirm whether the zero-knowledge and backup disclosures are sufficient.
8. Confirm the retention periods for support, logs, and backups.
