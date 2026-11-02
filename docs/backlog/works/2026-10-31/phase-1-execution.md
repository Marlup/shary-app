# Phase 1 Execution — All Lines

Date: 2026-10-31
Status: Phase 1 workbooks complete; external actions pending
Owner: Founder (single-owner project)
Scope: Execute Phase 1 of all eight Lines, prioritize the external lead-time
path, and record code-verified findings that change the plan.

## 1. Scope

The previous work log set the next step as: start Phase 1 of all eight Lines,
prioritize L1 incorporation, L2 data map, L4 pricing, L5 backup/TTL, L6 identity
ADR, and L7 trademark search, and book the external asesor, broker, and reviewer.

This pass produces the Phase 1 working artifacts that do not require external
authority or console access. It also records what remains blocked on the founder,
the asesor, the abogado, the broker, the security reviewer, and the Firebase/GCP
console.

## 2. Artifacts produced

| Line | Artifact | Status |
|---|---|---|
| L1 | [Entity, tax, and merchant checklist](lines/L1-entity-and-payments/phase-1-entity-tax-and-merchant-checklist.md) | Decision record, asesor brief, incorporation/tax/merchant checklist |
| L2 | [Data map and ROPA delta](lines/L2-privacy-and-data-governance/phase-1-data-map-and-ropa.md) | Draft; includes the Android backup finding |
| L3 | [ToS and store-compliance gap review](lines/L3-consumer-contracts-and-store-compliance/phase-1-tos-and-store-gap-review.md) | Gap review; legal text needs abogado |
| L4 | [Tier, pricing, and unit-economics freeze](lines/L4-monetization-and-entitlements/phase-1-tier-pricing-and-economics.md) | Planning freeze; Play fee/VAT verify |
| L5 | [Backup, TTL, and monitoring runbook](lines/L5-reliability-and-continuity/phase-1-backup-ttl-and-monitoring.md) | Runbook and console action list |
| L6 | [Identity ADR](lines/L6-security-and-identity/phase-1-identity-adr.md) | Option B proposed; Android backup decision |
| L7 | [Trademark, insurance, disclosure, accessibility](lines/L7-trust-brand-and-risk/phase-1-trademark-insurance-disclosure.md) | Search/RFQ/policy drafts; external actions pending |
| L8 | [Support, SLA, analytics, growth](lines/L8-support-growth-and-analytics/phase-1-support-sla-and-analytics.md) | Runbook, analytics spec, research/CAC plan |

## 3. Code-verified findings

These are the material findings from inspecting the Android and backend repos.
They change or sharpen the plan.

### F1 — Android Auto Backup is enabled with incomplete exclusions

`AndroidManifest.xml` has `android:allowBackup="true"`. The backup rules exclude
only `remote_mcp_identity.xml`, `remote_mcp_secure.xml`, `remote_mcp_state.xml`,
and `connector_prefs.preferences_pb`.

They do **not** exclude:

- `fields.pb`
- `users.pb`
- `requests.pb`
- `app_settings_prefs.preferences_pb`
- `contact_tags_prefs.preferences_pb`
- `field_value_history_prefs.preferences_pb`
- the credentials blob

Impact: encrypted vault/request ciphertext and plaintext contacts, settings, and
tags can be copied to the user's Google backup and device transfer. The
credentials blob is included, but its Android Keystore wrap key is not
restorable on another device. The claim "vault content never leaves your device"
is inaccurate for plaintext metadata and ciphertext moved by the OS.

Decision applied on 2026-11-01: Android cloud backup and device transfer are
disabled. `allowBackup="false"`; backup and data-extraction rules exclude every
app domain; `BackupPolicyTest` passes and the merged manifest reports
`allowBackup="false"`. The explicit encrypted export/import path is still
pending. This is shared component SC13, owned by L6, consumed by L2 and L5.

### F2 — No in-app account deletion

There is no account-deletion UI in `SettingsScreen` or its ViewModel. The backend
has `/v2/identity/delete`, but it requires a signature from the current signing
key (`_v2_delete_identity` in `main.py`). A user who lost the device or password
cannot delete the cloud identity. This violates the GDPR Art. 17 path and the
Play account-deletion requirement.

### F3 — No full vault export

No export UI or full-vault export path was found. The email share creates a
readable JSON attachment of selected fields only. This is not an access or
portability surface for all local data.

### F4 — Identity is password-derived at 200k PBKDF2

`deriveIdentity` uses PBKDF2-HMAC-SHA256 at 200,000 iterations, then HKDF to
derive Ed25519 and X25519 seeds. This is below the current OWASP 600k guidance
and has no KDF version in the credential blob, so raising it needs a migration.

### F5 — ECDH shared secret is used directly as the AES key

`encryptWithPeerPublic` uses the X25519 shared secret directly as the AES-GCM
key without HKDF. The audit flagged this; it remains true in the current code.

### F6 — Rate-limit and dedup checks are not atomic

The rate-limit counter is a read-modify-write on `rate_limits_v1`; the upload
dedup is query-then-set. Concurrent requests can exceed the limit or create
duplicates. The audit flagged this; it remains true.

### F7 — Firestore backups are absent and cleanup is paused

The code has `clean_expired_docs` and TTL fields, but the repo does not prove
that Firestore backups, PITR, `delete_after` TTL policies, budget alerts, or
monitoring alerts are enabled. A read-only check on 2026-11-01, repeated with
`tools/ops/verify-gcp-readiness.ps1` on 2026-11-02, found: no Firestore backup
schedule, no completed backup, PITR disabled, and the `cleanup-expired-payloads`
scheduler **PAUSED** (daily at 00:00, correct Madrid target). The seven MCP
`delete_after` TTL policies are enabled. Expired relay data is therefore not
being cleaned by the scheduler. The L5 runbook lists the exact actions.

### F8 — Firestore location is confirmed

The region migration runbook says Firestore location is fixed at database
creation. A read-only check on 2026-11-01 confirmed `europe-southwest1`.
Functions default to the same region, and Cloud Scheduler is in `europe-west1`
targeting the Madrid function. The 2026-11-02 checker also confirmed PITR is
disabled and the database edition is `STANDARD`.

### F9 — MCP persistent data is not covered by the TTL list

`mcp_oauth_clients_v1`, `mcp_agent_profiles_v1`, `mcp_vault_grants_v1`, and
`mcp_devices_v1` have no TTL or `delete_after` in the code. Grants expire after
30 days but remain as records; devices and profiles persist. L5/L6 must define
cleanup and account-deletion behavior for them.

### F10 — No DPA, DPIA, or published policy

The 2026-08-30 artifacts are drafts. No Google/Firebase DPA acceptance, DPIA,
published Privacy Policy, ROPA, or subprocessor list was found. The ROPA and
Privacy Policy drafts also contain overclaims about key storage and data egress.

### F11 — The ODR route in the ToS is dead

The ToS references the EU ODR platform. Regulation (EU) 2024/3228 repealed
Regulation (EU) 524/2013, and the platform closed on 20 July 2025. L3 must
replace the clause with the current ADR/consumer-authority route.

### F12 — No support, analytics, insurance, or trust surface

There is no published SLA, support macro set, analytics specification, insurance
policy, responsible-disclosure policy, trust page, or accessibility audit. The
L7/L8 workbooks define the drafts and external actions.

## 4. Status per line

| Line | Phase 1 state | What is blocked |
|---|---|---|
| L1 | Decision record and checklists ready | Asesor, notary, bank, AEAT, D-U-N-S, Play organization account |
| L2 | Data map and ROPA delta drafted | Abogado review, DPA acceptance, DPIA, backup decision, rights implementation |
| L3 | ToS/store gap review drafted | Abogado sign-off, Spanish version, Data Safety answers, deletion UI/web link |
| L4 | Tier, price, and economics frozen | Play fee/VAT confirmation, billing implementation, server quotas |
| L5 | Backup/TTL/monitoring runbook ready | GCP console access, backups/PITR, TTL policies, alerts, restore drill, trustee |
| L6 | Identity ADR proposed | Founder approval, independent reviewer, implementation, Android backup change |
| L7 | Search/RFQ/policy drafts ready | OEPM/EUIPO search, filing, insurance quotes, security mailbox, EAA decision |
| L8 | Support/analytics/growth workbook ready | Mailboxes, SLA page, Play vitals, interviews, dashboard |

## 5. External actions register

Deferred execution steps, costs, and triggers:
[[docs/backlog/works/2026-10-31/deferred-actions.md]].

| Action | Owner | Blocks |
|---|---|---|
| Engage the asesor | Founder | L1 P1 |
| Book the notary and bank | Founder | L1 P1 |
| Obtain D-U-N-S and create the Play organization account | Founder | L1 P2, L4 P2 |
| Accept and file the Google/Firebase DPA | Founder | L2 P2 |
| Review the data map, ROPA, DPIA, ToS, and Privacy Policy | Abogado | L2 P2, L3 P1 |
| Confirm the ODR/ADR replacement | Abogado | L3 P2 |
| Decide the Android backup policy | Founder + L6 | L2 P2, L5 P1 |
| Enable Firestore backups, PITR, TTL policies, and alerts | Founder | L5 P1/P2 |
| Verify the Firestore location | Founder | L5 P1 |
| Run the restore drill and TTL test | Founder | L5 P2 |
| Name the trustee and technical executor | Founder | L5 P4 |
| Approve the identity ADR and book the reviewer | Founder | L6 P1/P4 |
| Run the trademark search and file | Founder | L7 P1 |
| Request and bind insurance | Founder/broker | L7 P2 |
| Create security@ and publish security.txt | Founder | L7 P2 |
| Confirm the EAA applicability/exemption | Founder/abogado | L7 P3 |
| Create support@ and publish the SLA | Founder | L8 P1 |
| Run 20-30 pricing interviews | Founder | L8 P3 |

## 6. Decisions applied and new decisions

Applied from the previous pass:

- S.L. in Madrid; single-shareholder S.L.U. acceptable.
- Spain-first; Spanish consumer information at launch.
- MCP included in Plus; v1 hard cap.
- Lean external budget; 20-week gate-driven target.
- Evidence outside the repo; founder owns all Lines.
- Production/paid launch waits for the S.L., NIF, and organization Play account.
- Android-only MVP.

New decisions proposed in this pass:

- **D3a:** defer the MCP overage add-on to v1.1; v1 hard-caps.
- **D3b:** defer Teams/Family.
- **D3c:** no free trial or introductory offer at launch.
- **D3d:** replace "unlimited fair use" with numeric allowances and hard caps.
- **D6a:** adopt identity Option B (600k PBKDF2, KDF version, HKDF for ECDH,
  passphrase policy, offline recovery secret, email-verified erase).
- **D6b:** disable Android backup for all Shary data and add an explicit
  encrypted export/import path (pending founder approval).
- **D5a:** support SLAs are Free 5 business days, Plus 2 business days; no 24/7
  on-call.
- **D8a:** no third-party analytics SDK at launch; use Play Console and server
  aggregate counters.

## 7. Validation performed

- Session-init policy gate passed (`policy_version=2026-07-18.1`).
- Inspected the Android manifest, backup rules, local DataStore/repository code,
  identity derivation, cloud service, and MCP client storage.
- Inspected the backend function constants, identity routes, TTL cleanup, MCP
  OAuth/grant/operation code, Firebase config, Firestore rules, and region
  runbook.
- Cross-checked the existing 2026-08-30 audit, remediation plan, ROPA draft,
  ToS draft, Privacy Policy draft, and account-recovery note.
- Read-only `gcloud` checks: Firestore location, backup list, scheduler state.
- Markdown link/table/numbering/whitespace checks on the new documents.
- Repo-side hardening applied: Android backup is now fail-closed
  (`allowBackup="false"` plus all-domain exclusions).
- `BackupPolicyTest` passes; `:app:processDebugMainManifest` passes and the
  merged manifest reports `allowBackup="false"`.
- No other application code was changed. No other unit tests were run.

## 8. Next steps

1. Founder: book the asesor, abogado, broker, and security reviewer; start L1
   name reservation and bank/notary paperwork.
2. Founder: decide D6b (Android backup) and approve the identity ADR.
3. Founder: enable/verify the GCP/Firebase controls in the L5 runbook.
4. Abogado: review the L2 data map/ROPA and L3 gap review.
5. Then implement, in order: L6 erasure/backup/identity migration, L4 billing and
   quotas, L2 rights/export/DPIA, L3 deletion UI/Data Safety, L5 restore/alerts.
6. Do not submit the store listing or charge users until the L2/L3/L4 gates
   pass.

## 9. Plan source

Plan source: [[docs/backlog/works/2026-10-31/lines-of-refinement.md#6. Implementation plan]]

Supporting evidence:

- [[docs/backlog/works/2026-08-30/security-compliance-audit.md]]
- [[docs/backlog/works/2026-08-30/remediation-plan.md]]
- [[docs/backlog/works/2026-08-30/account-recovery-options.md]]
- [[docs/backlog/works/2026-08-30/ropa-draft.md]]
- [[docs/backlog/works/2026-08-30/terms-of-service-draft.md]]
- [[docs/backlog/works/2026-08-30/privacy-policy-draft.md]]
