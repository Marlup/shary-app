# L2 Phase 1 — Data Map and ROPA Delta

Status: In progress (draft for abogado review)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L2 — Privacy & Data Governance](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

This is a working data map and ROPA delta. It is not legal advice and does not
replace the abogado's review. It extends the 2026-08-30
[ROPA draft](../../../2026-08-30/ropa-draft.md) with code-verified storage,
retention, and the Android backup finding.

## 1. Controller and roles

| Role | Entity | Notes |
|---|---|---|
| Controller | `[S.L. name, NIF, address]` | Pending L1 incorporation |
| Processor | Google Cloud / Firebase | Firestore, Functions, Auth, FCM, KMS, Scheduler, Hosting |
| Processor | Google Play | Distribution, billing, purchase data |
| Processor | Support email provider | Only if a third-party provider is used; not configured yet |
| DPO | Not appointed | Assess whether required; AEPD guidance applies |

The DPA with Google/Firebase must be accepted and stored as evidence. Firebase
Auth and FCM may use global infrastructure even when Firestore and Functions are
in `europe-southwest1`; the transfer analysis must cover them.

## 2. Local data inventory

The app is offline-first. Most vault data stays on the device.

| Store | File | Contents | Protection | Backup exposure | Retention |
|---|---|---|---|---|---|
| Fields | `datastore/fields.pb` | Field key, value, alias, tag, date added, MCP-shareable flag | Key/value/alias/tag encrypted per purpose with a password-derived key; metadata plaintext | Excluded; Android backup disabled on 2026-11-01 | Until the user deletes the field or uninstalls |
| Contacts | `datastore/users.pb` | Name, email, tag, date added | Plaintext in app-private storage | Excluded; Android backup disabled on 2026-11-01 | Until the user deletes the contact or uninstalls |
| Requests | `datastore/requests.pb` | Requested keys/aliases, recipients, requester, status, delivery outcomes, workflow snapshots | Requested fields encrypted; recipients/status plaintext | Excluded; Android backup disabled on 2026-11-01 | Until the user deletes the request or uninstalls |
| Settings | `datastore/app_settings_prefs.preferences_pb` | Remembered email, MCP enabled, cloud expiry, biometric preference, clipboard timer, sync preference | Plaintext | Excluded; Android backup disabled on 2026-11-01 | Until changed or uninstalled |
| Contact tags | `datastore/contact_tags_prefs.preferences_pb` | Tag assignments for contacts | Plaintext | Excluded; Android backup disabled on 2026-11-01 | Until changed or uninstalled |
| Field value history | `datastore/field_value_history_prefs.preferences_pb` | Previous field value snapshots | Encrypted with the vault codec | Excluded; Android backup disabled on 2026-11-01 | Until consumed, cleared, or uninstalled |
| Connector state | `datastore/connector_prefs.preferences_pb` | Clients, sessions (token hashes), policies, audit events, pending actions (may include proposed field values) | Plaintext JSON in app-private storage; explicitly excluded from backup | Excluded by `backup_rules.xml` and `data_extraction_rules.xml` | Until revoked, cleared, or uninstalled |
| Remote MCP credentials | `shared_prefs/remote_mcp_secure.xml` | Access token, refresh secret, grant metadata, pending operation commands | AES-GCM with an Android Keystore key | Excluded from backup | Until revoked, expired, or uninstalled |
| Remote MCP identity/state | `shared_prefs/remote_mcp_identity.xml`, `remote_mcp_state.xml` | Owner identity and connection state | App-private storage | Excluded from backup | Until logout or uninstall |
| Credentials blob | App files | Email, username, version, timestamp | Inner AES-GCM with a password-derived key; outer AES-GCM wrap with an Android Keystore key | Excluded; Android backup disabled on 2026-11-01 | Until logout, password reset, or uninstall |
| Email attachments | `cache/email-attachments/` | Readable JSON of selected fields | Readable by the chosen sharing app; cache only | Cache is not backed up | 24 hours or next app start after expiry |
| Clipboard | System clipboard | Copied field/contact values | Android sensitive flag where supported; auto-clear | Not persisted by Shary | Configured seconds, or process background |

Code evidence:

- `FieldListDataStore.kt`, `UserDataStore.kt`, `RequestListDataStore.kt`
- `mappers.kt` (field keys/values/aliases/tags encrypted; user fields plaintext)
- `SettingsRepositoryImpl.kt`, `ContactTagRepositoryImpl.kt`,
  `FieldValueHistoryRepositoryImpl.kt`, `ConnectorRepositoryImpl.kt`
- `RemoteMcpSecureStore.kt`, `AuthenticationServiceImpl.kt`
- `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`

## 3. Server data inventory

| Collection or system | Contents | Retention | Notes |
|---|---|---|---|
| `identities_v2` | Email, user hash, public signing/key-exchange keys, key version, timestamps | Until deletion | Non-transient; must be backed up |
| `identity_rotate_nonces_v2` | Nonce, user hash, expiry, used flag | 5 minutes; scheduler cleanup | Used for rotation |
| `payloads` | Encrypted payload, sender/recipient hashes, verification, signature, timestamps, expiry | 7 days default, 14 days max; scheduler cleanup | Ciphertext only |
| `requests` | Encrypted request payload, sender/recipient hashes, verification, signature, timestamps, expiry | 7 days default, 14 days max; scheduler cleanup | Ciphertext only |
| `request_decisions_v1` | Request ID, recipient hash, decision, reason code, timestamps | Aligned to request expiry; scheduler cleanup | Idempotent decisions |
| `rate_limits_v1` | Actor key, counter, window, expiry | Window plus two windows; scheduler cleanup | Hashed actor keys |
| `mcp_oauth_clients_v1` | Client ID, name, redirect URIs, grant types, creation time | Persistent | No TTL observed |
| `mcp_oauth_requests_v1` | Authorization request, client, scopes, poll token hash, status, expiry | About 10 minutes; Firestore TTL `delete_after` | Verify TTL policy |
| `mcp_oauth_codes_v1` | Authorization code hash, client, agent profile, scopes, expiry | About 10 minutes; TTL | Verify TTL policy |
| `mcp_oauth_refresh_tokens_v1` | Refresh token hash, agent profile, client, scopes, expiry, revocation | 30 days; TTL | Verify TTL policy |
| `mcp_oauth_consumptions_v1` | One-time refresh-token consumption records | 30 days; TTL | Verify TTL policy |
| `mcp_agent_profiles_v1` | Agent profile ID, client, display name, status, activity | Persistent | No TTL observed |
| `mcp_vault_grants_v1` | Grant ID, agent profile, owner UID, vault ID, device ID, alias, scopes, status, expiry, revocation | 30 days; renewal; no TTL observed | Verify cleanup of expired grants |
| `mcp_devices_v1` | Owner UID, device ID, FCM token, platform, app version, last seen | Until device removal | FCM token is personal data |
| `mcp_operations_v1` | Operation ID, grant, owner, device, tool, encrypted command/result, status, timestamps | 10 minutes plus 5-minute result TTL; TTL | Ciphertext envelope |
| `mcp_grant_invitations_v1` | Invitation ID, grant, status, expiry | About 10 minutes; TTL | Verify TTL policy |
| `mcp_rate_limits_v1` | Hashed key, counter, window, expiry | Short window; TTL | Hashed keys |
| Firebase Auth | Email, UID, password hash, verification status, tokens | Google-managed | Google processor; transfer analysis needed |
| FCM | Device token, delivery metadata | Google-managed | Google processor |
| Cloud KMS | Envelope keys and key versions | Google-managed | No plaintext |
| Cloud Logging | Event name, request ID, actor hash, error code, timestamps | Verify current log retention | Do not log payloads or tokens |
| Google Play | Purchase token, order ID, subscription status, price, country | Google-managed | Google merchant/processor |

Code evidence:

- `shary-backend/firebase/functions/constants.py` (TTL constants)
- `shary-backend/firebase/functions/main.py` (identity, payload, decision,
  rate-limit, cleanup)
- `shary-backend/firebase/mcp-functions/mcp_gateway/oauth.py` (OAuth
  collections, scopes, grant TTL)
- `shary-backend/firebase/mcp-functions/mcp_gateway/service.py` (devices,
  operations, invitations, operation/result TTL)
- `shary-backend/firebase/mcp-functions/mcp_gateway/tokens.py` (token and grant
  TTLs)
- `shary-backend/firebase/mcp-functions/README.md` (TTL policy list)

## 4. Processing activities (ROPA delta)

These rows extend the 2026-08-30 ROPA draft.

| # | Activity | Purpose | Data subjects | Data categories | Legal basis | Recipients | Retention | Transfers |
|---|---|---|---|---|---|---|---|---|
| 1 | Local vault | Let the user store and manage fields, contacts, requests | User | Field key/value/alias/tag; contact name/email; request recipients; settings; value history | Contract | None; on-device | Until user deletes or uninstalls | None, unless Android backup is enabled |
| 2 | Account and identity | Register and authenticate the cloud identity | Registered user | Email, public keys, key version, timestamps, Firebase UID | Contract | Google/Firebase | Until deletion | Firebase Auth may be global; verify |
| 3 | Cloud relay | Send and receive encrypted shares | Sender, recipient | Ciphertext, sender/recipient hashes, signatures, timestamps, expiry | Contract | Google/Firebase | 7-14 days | EU region for Firestore/Functions |
| 4 | Inbox decisions | Record accept/reject idempotently | Recipient | Decision, reason code, request ID, recipient hash, timestamps | Contract | Google/Firebase | Request expiry | EU region |
| 5 | MCP OAuth and grants | Let an owner authorize an agent | Owner | Client metadata, agent profile, scopes, grant/device IDs, FCM token, token hashes, timestamps | Contract; consent for optional scopes | Google/Firebase, Cloud KMS | 30-day grants; short tokens; persistent profiles/clients | EU region for Firestore; KMS region verify |
| 6 | MCP operations | Execute approved agent actions | Owner | Encrypted command/result, operation ID, tool name, status, timestamps | Contract | Google/Firebase | 10 minutes plus result TTL | EU region |
| 7 | Billing and entitlements | Charge and manage subscriptions | Paying user | Purchase token, order ID, subscription status, price, country | Contract; legal obligation for tax | Google Play | Google-managed; local entitlement until account deletion | Google Play terms |
| 8 | Support | Respond to user requests | User contacting support | Email, message, attachments | Contract; consent where optional | Support email provider, if used | Until resolved plus a defined period | Verify provider |
| 9 | Security and abuse | Protect the service | Users, attackers | Rate-limit counters, IP-derived actor keys, request IDs, error codes, logs | Legitimate interest | Google Cloud | Short; logs per retention policy | EU region where possible |
| 10 | Android backup/device transfer | OS-level backup of app data | User | Encrypted vault/request files, plaintext contacts/settings/tags, credentials blob | Contract? | Google (user's backup) | User/Google-controlled | Outside the app's direct control; needs decision |
| 11 | Email sharing | Hand selected data to the user's email app | User, recipients | Readable JSON attachment | Contract; user-initiated | User's chosen email app/provider | 24-hour local cache; provider-controlled after share | User-controlled |

## 5. Retention matrix

| Data | Retention | Source of truth |
|---|---|---|
| Local fields, contacts, requests, settings, tags, history | Until user deletes or uninstalls | App storage |
| Cloud payloads, requests, decisions | 7 days default; 14 days maximum | `expires_at` and scheduler |
| Identity rotation nonces | 5 minutes | `expires_at` and scheduler |
| Rate-limit counters | Window plus two windows | `expires_at` and scheduler |
| Identity records | Until deletion | `identities_v2` |
| MCP authorization requests/codes | About 10 minutes | `delete_after` TTL |
| MCP refresh tokens/consumptions | 30 days | `delete_after` TTL |
| MCP grants | 30 days, renewable; revoke on request | `expires_at`/`revoked_at` |
| MCP operations/results | 10 minutes plus 5 minutes | `delete_after` TTL |
| MCP devices/profiles/clients | Until removed or revoked | Persistent collections |
| Support email | Define; propose 12 months after resolution | Support provider |
| Cloud logs | Define; propose 30 days unless security needs longer | Cloud Logging |
| Firestore backups | Define; propose 30 days | Backup configuration |
| Billing/tax records | 4-6 years | L1 retention schedule |
| Android backup | User/Google-controlled; disable or disclose | L6 decision |

The support, log, and backup periods are proposals; the abogado and asesor must
confirm them against the retention schedule and the privacy policy.

## 6. Subprocessors

| Subprocessor | Service | Data | Region or transfer | DPA status |
|---|---|---|---|---|
| Google Cloud / Firebase | Firestore, Functions, Auth, FCM, KMS, Scheduler, Hosting | Identity, ciphertext, metadata, device tokens, logs | `europe-southwest1` for Firestore/Functions; Auth/FCM may be global | Pending acceptance and filing |
| Google Play | Distribution and billing | Purchase and subscription data | Google terms | Pending review |
| Support email provider | Support inbox | Support messages | To be confirmed | Not configured |
| Insurance broker | Insurance | Claims and risk data | To be confirmed | Not a processing activity yet |

Do not publish the subprocessor list until the providers and regions are
confirmed. L7 will publish it on the trust page.

## 7. Data-subject rights assessment

| Right | Current state | Gap | Owner |
|---|---|---|---|
| Access | Local data is visible in the app; no full export | No single export of all local data | L2/L3 |
| Rectification | Fields/contacts editable locally | No email/identity change flow | L2/L6 |
| Erasure | Local delete exists; cloud delete requires the old signing key | No in-app account deletion; lost-key erasure missing | L6/L3 |
| Portability | Encrypted `.sharyvault` export/import covers fields, contacts, requests, tags, and safe settings | Credentials and MCP state are excluded by design; passphrase loss is unrecoverable | L2/L3 |
| Restriction | None | No UI or support procedure | L2 |
| Objection | None | No support procedure for legitimate-interest processing | L2 |
| Automated decisions | MCP writes are approval-gated | No solely automated decision with legal effect | L6/L8 |

The current erasure gap is the most serious: a user who loses the device or
password cannot delete the cloud identity because `/v2/identity/delete` requires
a signature from the lost key. Option C (email-verified erase plus offline-secret
rotate) is the selected design in the 2026-08-30 account-recovery note and must
be implemented before the L2 gate.

## 8. Android Auto Backup finding

The manifest has `android:allowBackup="true"`. The backup rules exclude only:

- `remote_mcp_identity.xml`
- `remote_mcp_secure.xml`
- `remote_mcp_state.xml`
- `datastore/connector_prefs.preferences_pb`

They do **not** exclude:

- `datastore/fields.pb`
- `datastore/users.pb`
- `datastore/requests.pb`
- `datastore/app_settings_prefs.preferences_pb`
- `datastore/contact_tags_prefs.preferences_pb`
- `datastore/field_value_history_prefs.preferences_pb`
- the credentials blob

Consequences:

1. Encrypted vault/request ciphertext and plaintext contacts, settings, and tags
   can be copied to the user's Google backup and to device transfer.
2. The credentials blob is included, but its Android Keystore wrap key is not
   restorable on another device; the app may purge credentials after restore.
3. The public claim "vault content never leaves your device" is inaccurate for
   contacts/settings and for ciphertext moved by the OS.
4. The ROPA and Privacy Policy must disclose this transfer, or the app must
   disable or narrow backup.
5. The backup increases the offline-guessing surface because vault ciphertext
   leaves the device and can be attacked without the device sandbox.

Decision applied on 2026-11-01: Android cloud backup and device transfer are
disabled. `AndroidManifest.xml` sets `allowBackup="false"`, and
`backup_rules.xml` / `data_extraction_rules.xml` exclude every app domain as
defense-in-depth. `BackupPolicyTest` guards the configuration. The policy/ROPA
must state that OS backup is disabled.

The user-initiated encrypted export/import path is now implemented for fields,
contacts, requests, tags, and safe settings. Credentials and MCP state are
excluded by design. The policy must disclose that OS backup is disabled and that
losing the export passphrase makes the backup unrecoverable.

This is a new cross-line component (SC13). L6 owns the security decision and
manifest change; L2 owns the policy/ROPA disclosure; L5 owns the continuity
consequence. See [cross-line.md](../cross-line.md).

## 9. Gaps and blockers

1. No signed Google/Firebase DPA on file.
2. No DPIA for the biometric gate, MCP access, and special-category vault data.
3. No published Privacy Policy or ROPA; only drafts.
4. Encrypted export/import exists; a structured access export for GDPR requests
   still needs a documented support path.
5. No in-app account deletion; lost-key erasure missing.
6. Android backup is disabled; the explicit encrypted export/import path is
   still missing.
7. Log, support, and backup retention periods undefined.
8. Firebase Auth/FCM transfer analysis incomplete.
9. No age affirmation at sign-up; only a policy statement.
10. No DPO assessment.
11. No subprocessor list published.
12. No support-provider DPA (if a provider is used).

## 10. Next actions

| Action | Owner | Output |
|---|---|---|
| Review this map with the abogado | Founder | Written corrections and sign-off |
| Accept and file the Google/Firebase DPA | Founder | EV-L2-04 |
| Draft the DPIA from rows 3-6 and 9-10 | Founder + abogado | EV-L2-05 |
| Decide the Android backup policy with L6 | Founder | SC13 decision |
| Implement or document the full export surface | L2/L3 | Rights drill evidence |
| Implement option C erasure | L6 | L6-V3 |
| Define log/support/backup retention | Founder + abogado | Retention matrix |
| Confirm Auth/FCM transfer mechanism | Founder + abogado | Transfer note |
| Add an age affirmation if required | L3/L8 | Flow evidence |

## 11. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L2-V1 Data map | This document | Draft |
| L2-V2 Roles and legal bases | Section 1 and 4 | Draft |
| L2-V3 Privacy Policy | Existing draft; needs update | Pending |
| L2-V4 DPA | Not signed | Pending |
| L2-V5 DPIA | Not started | Pending |
| L2-V6 Rights | Section 7 | Gaps |
| L2-V7 Key-loss erasure | Option C selected | Pending |
| L2-V8 Breach procedure | Existing draft; needs tabletop | Pending |
| L2-V9 Retention | Section 5 | Draft |
| L2-V10 Records restore | Not tested | Pending |
| L2-V11 Age and consent | Policy only | Pending |
| L2-V12 Subprocessor list | Section 6 | Draft |
| L2-V13 Android backup | Section 8 | Applied in manifest/rules; policy update pending |
