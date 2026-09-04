# Shary App Technical Overview

## 1. System intent

Shary is an Android-first secure data vault and controlled sharing system.
Its operating model is:

- keep personal data locally usable by default,
- allow selective outbound and inbound exchange,
- preserve explicit owner approval for sensitive operations,
- expose AI/agent access through scoped, auditable MCP grants instead of raw
  backend or database access.

## 2. Primary product domains

### Fields

Fields are the main vault records. A field is a structured data item with a
stable key plus supporting metadata such as alias, tag, typed value handling,
and timestamps.

Current behavior in the app includes:

- local create/update/delete,
- custom-tag create and rename with shared validation, collision detection, and
  terminal persistence outcomes,
- search and sort,
- selection-based review,
- value-history recovery for some edits,
- strict-mode value validation when enabled.
- task values with per-item workflow status;
- sensitive values whose alias/value are masked until device authentication;
- a local per-field MCP sharing switch for sensitive records.

Relevant code:

- `app/src/main/java/com/shary/app/viewmodels/field/FieldViewModel.kt`
- `app/src/main/java/com/shary/app/viewmodels/tag/TagViewModel.kt`
- `app/src/main/java/com/shary/app/core/domain/types/valueobjects/TagNameContract.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/FieldScreen.kt`

### Users

Users are the recipient/contact model used by sharing and request flows.

Current behavior includes:

- local create/update/delete,
- atomic email rename using the original email identity, with collision and
  missing-original outcomes instead of duplicate creation,
- typed editor saving/failure state that retains the Contact draft until the
  repository confirms success,
- cached selection for outbound review flows,
- owner identity reuse inside cloud and MCP flows.

Relevant code:

- `app/src/main/java/com/shary/app/viewmodels/user/UserViewModel.kt`
- `app/src/main/java/com/shary/app/ui/screens/user/UserScreen.kt`

### Requests

Requests model the "ask for data" side of the product.

Current behavior includes:

- drafting requested keys with explicit added/duplicate/invalid editor outcomes,
- preserving saveable key/alias input, dirty-dismiss protection, and IME submit
  behavior in the adaptive requested-field editor,
- preserving stable Request IDs for list keys and open-detail identity,
- showing adaptive Received/Sent detail with truthful mode/status, timestamps,
  people, complete requested fields, match progress, channel, and persisted
  per-recipient outcomes,
- sending requests by email or encrypted cloud relay,
- receiving inbound request items from cloud,
- matching received requests against local fields before responding.

Relevant code:

- `app/src/main/java/com/shary/app/ui/screens/request/RequestScreen.kt`
- `app/src/main/java/com/shary/app/application/request/RequestDraftEditor.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryRequestScreen.kt`

### Summary / review flows

Before send operations, Shary moves the user into a review step that shows the
selected payload, recipient set, and channel.

Review shows every recipient, not a representative first recipient. Cloud copy
states that Shary encrypts one payload per recipient and that relay ciphertext
is retained until the configured expiry. Email copy states that the selected
sharing app receives readable JSON, multi-recipient addresses may be visible to
one another, and chooser launch cannot prove sending or delivery.

Outgoing Request and received-response draft identity is stored as a minimal,
owner-scoped workflow snapshot. It includes request/key/recipient identities but
not Field values; requested and matched keys use the vault codec. After process
death, re-authentication restores only the matching owner's snapshot, and a
response is accepted only when its requester identity still matches received
history.

Current send channels are:

- Email
- Cloud relay

Relevant code:

- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryFieldScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryRequestScreen.kt`

### Settings and control plane

Settings is where the app exposes:

- login email preference,
- cloud inbox and sync preferences,
- clipboard/privacy controls,
- current public MCP endpoint,
- active MCP grant list,
- revoke actions,
- recent MCP audit activity.

Relevant code:

- `app/src/main/java/com/shary/app/ui/screens/settings/SettingsScreen.kt`

Field and Contact clipboard expiry is owned by one process singleton rather
than a screen coroutine. Sensitive Field clips carry Android's sensitive flag;
configured expiry survives section navigation, while backgrounding or session
teardown immediately clears only a still-current Shary-owned clip.

## 3. Current architecture shape

The codebase currently follows a layered shape even though it remains inside a
single Android application module.

### UI layer

Compose screens and dialogs implement the visible product behavior.

Core navigation entry points:

- `app/src/main/java/com/shary/app/MainActivity.kt`
- `app/src/main/java/com/shary/app/ui/screens/home/utils/AppNavigator.kt`
- `app/src/main/java/com/shary/app/ui/screens/home/utils/Screen.kt`

The element/action dependency map is maintained in
`docs/design/SHARY_ELEMENT_ACTION_MAP.md`.

### ViewModel layer

ViewModels own screen state, event streams, selection state, and coordination
with repositories/services.

Key examples:

- `FieldViewModel`
- `UserViewModel`
- `RequestViewModel`
- `SettingsViewModel`
- `ConnectorSettingsViewModel`
- `CloudViewModel`

### Application/service layer

This layer contains use cases and planners that encode business rules.

Important examples:

- connector pairing / session / preview / commit use cases under
  `app/src/main/java/com/shary/app/application/connector/`
- cloud import diff planning in
  `app/src/main/java/com/shary/app/application/cloud/CloudFieldImportPlanner.kt`
- post-login loading and background timeout coordination under
  `app/src/main/java/com/shary/app/application/session/`

### Infrastructure layer

This layer handles persistence, cloud transport, MCP dispatch, and background
execution.

Important examples:

- cloud services under
  `app/src/main/java/com/shary/app/infrastructure/services/cloud/`
- connector transport and auth under
  `app/src/main/java/com/shary/app/infrastructure/connector/transport/`
- Android MCP dispatch and HTTP transport under
  `app/src/main/java/com/shary/app/infrastructure/connector/mcp/`
- remote MCP coordination under
  `app/src/main/java/com/shary/app/infrastructure/connector/remote/`

## 4. Main runtime flows

### A. Local vault management

1. The user logs in locally.
2. Fields and users are loaded into ViewModel state.
3. Screen interactions update repositories through ViewModel commands. Field
   add/copy/update/recovery editors claim a typed `Saving` operation before the
   asynchronous write so rapid duplicate submissions are ignored.
4. Field editors dismiss only after `Saved`; a `Failed` result keeps the draft
   and actionable error visible for retry.
5. The UI refreshes from repository-backed state rather than direct widget
   mutation.

This preserves the offline-first baseline for core usage.

### B. Outbound secure send

1. The user selects fields and recipients.
2. Shary opens a summary/review screen.
3. The user chooses Email or Cloud.
4. If Cloud is selected, readiness gates are checked first and a synchronous
   delivery claim prevents duplicate rapid sends.
5. Payloads are encrypted separately per recipient and uploaded with expiry
   metadata.
6. The review remains in a terminal `Sent`, `Partial`, or `Failed` receipt;
   outgoing Request and received-response receipts persist the cloud channel,
   timestamp, and each recipient result under the stable request ID and restore
   when a recreated review route provides that same ID.
   Retries include failed recipients only.
7. If Email is selected, Shary prepares a readable JSON attachment and opens
   Android's share sheet. The terminal app state is `Share sheet opened`, not
   sent or delivered.

Email attachments live only in a dedicated cache directory. Cleanup is
scheduled after 24 hours while the process is alive, and application startup
sweeps files that expired while Shary was not running.

The UX intentionally keeps disabled cloud actions visible and explains why they
are unavailable.

### C. Inbound cloud inbox review

1. The owner opens the cloud inbox from Fields or Requests.
2. Shary downloads pending payload metadata.
3. The app optionally decrypts enough content to show preview field names and
   import diffs.
4. The owner explicitly accepts or rejects each pending item.
5. Accepted data is imported through controlled planner logic instead of direct
   blind merge.
6. Each decision is claimed once before cloud work starts and ends in a receipt;
   an ambiguous failure may retry only the same decision for the same item.

This is a key trust boundary in the product.

### D. Remote MCP / agent flow

The currently supported product-facing agent flow is described in:

- `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`

Supported high-level sequence:

1. An OAuth-capable MCP client connects to the public Shary MCP endpoint.
2. The user completes OAuth and grant approval in Shary.
3. The backend gateway routes encrypted operations to the bound Android vault.
4. Android remains the execution, confirmation, renewal, and revocation
   boundary.
5. Writes remain approval-gated on the device.

The old local loopback MCP flow has been archived as legacy material and is no
longer the supported product entry point.

## 5. Capabilities by surface

### Local user-facing capabilities

- Secure field vault management.
- User/contact management.
- Selective field sharing to chosen recipients.
- Request drafting and inbound request handling.
- Cloud inbox review with accept/reject decisions.
- Cloud availability messaging and privacy preferences.

### Android-side MCP execution capabilities

The in-app MCP dispatcher currently defines tool behavior for:

- `shary.fields.list`
- `shary.fields.get`
- `shary.fields.create`
- `shary.fields.update`
- `shary.users.list`
- `shary.users.create`
- `shary.users.update`
- `shary.actions.preview`
- `shary.actions.commit`

Relevant code:

- `app/src/main/java/com/shary/app/infrastructure/connector/mcp/SharyMcpDispatcher.kt`

### Supported public MCP product boundary

At the supported product level, the public OAuth MCP path emphasizes:

- vault grant discovery,
- metadata-only field listing by default,
- exact-value reads only with stronger scope,
- operation polling for approval-gated writes,
- Settings-based grant visibility and revocation.

Relevant design doc:

- `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`

## 6. Security and privacy model

Shary's current design relies on several explicit control points.

### Offline-first baseline

Core vault behavior does not depend on live cloud availability. Cloud is an
augmentation layer, not the root of usability.

### Least-disclosure data flow

- Local field metadata and field values are treated differently.
- Public MCP design limits default disclosure and separates stronger value-read
  access.
- Review screens expose only the selected payload for the current task.

### Approval gating

- Cloud inbox imports are owner-reviewed.
- Sensitive MCP writes are owner-approved on the Android device.
- Settings exposes active grants and revoke actions.

### Operational safeguards

- Optional biometric gate for cloud inbox access.
- Wi-Fi-only sync option.
- Expiry defaults for cloud items.
- Visible cloud-disabled reasons instead of silent failure.

## 7. Current product boundary and non-goals

As of the current repo state:

- The supported agent path is public OAuth MCP.
- The Android app remains the vault execution boundary.
- Deprecated local loopback MCP remains only as archived or internal legacy
  material, not as a supported end-user setup path.

The product is therefore not aiming for unrestricted background automation or
opaque third-party access. Its technical direction is controlled interoperability.
