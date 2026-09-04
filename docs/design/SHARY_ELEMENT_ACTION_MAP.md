# Shary Element and Action Map

Development-only map for quickly tracing visible elements, actions, guards,
state, and owning application components. This document is not rendered in the
app.

## Relation Vocabulary

| Relation | Meaning |
|---|---|
| `CONTAINS` | A screen or component owns a visible child element. |
| `TRIGGERS` | A user or agent action invokes behavior. |
| `READS` | An element or action consumes state. |
| `WRITES` | An action changes repository or session state. |
| `NAVIGATES_TO` | An action changes the active route. |
| `GUARDED_BY` | An action requires policy, confirmation, or authentication. |
| `EMITS` | A state owner publishes a result or event. |
| `REFRESHES` | A mutation causes shared state to reload. |
| `CLEARS` | An action removes ephemeral or authenticated state. |
| `MASKS` | A policy replaces protected content with a safe representation. |
| `LOADS` | A coordinator retrieves or decrypts data. |

## Application Navigation

```mermaid
flowchart LR
    LOGIN[screen.login] -->|NAVIGATES_TO after initial fields ready| FIELDS[screen.fields]
    FIELDS --> CONNECT[screen.connect]
    FIELDS --> USERS[screen.users]
    FIELDS --> REQUESTS[screen.requests]
    FIELDS -->|selected fields and users| REVIEW_FIELDS[screen.summary.fields]
    REQUESTS --> REVIEW_REQUESTS[screen.summary.requests]
    USERS --> REVIEW_FIELDS
    USERS --> REVIEW_REQUESTS
    FIELDS --> SETTINGS[screen.settings]
    CONNECT --> SETTINGS
    USERS --> SETTINGS
    REQUESTS --> SETTINGS
    SESSION[action.session.close] -->|CLEARS and NAVIGATES_TO| LOGIN
```

## Login, Loading, and Session

```mermaid
flowchart TD
    SUBMIT[login.submit] -->|TRIGGERS| AUTH[AuthenticationService.signIn]
    AUTH -->|TRIGGERS| PRELOAD[SessionDataCoordinator.start]
    PRELOAD -->|LOADS concurrently| FIELD_REPO[FieldRepository]
    PRELOAD -->|LOADS concurrently| USER_REPO[UserRepository.users]
    FIELD_REPO -->|EMITS successful decryptions| READY{5 fields ready or load complete}
    READY -->|yes| FIELDS[screen.fields]
    FIELD_REPO -->|EMITS remaining snapshots| FIELDS
    USER_REPO -->|EMITS independently| USERS[Fields and Users state]
    BACKGROUND[process.onStop] -->|TRIGGERS 5 minute deadline| TIMEOUT[SessionTimeoutCoordinator]
    BACKGROUND -->|CLEARS current Shary-owned clip| CLIPBOARD[ClipboardCleanupCoordinator]
    TIMEOUT -->|CLEARS| AUTH
    TIMEOUT -->|CLEARS| PRELOAD
    TIMEOUT -->|NAVIGATES_TO| LOGIN[screen.login]
```

## Fields and Sensitive Access

```mermaid
flowchart TD
    CARD[field.card] -->|READS| FIELD[FieldDomain]
    FIELD --> TYPE{value type}
    TYPE -->|sensitive| MASK[exact mask *****]
    MASK -->|MASKS| CARD
    CARD -->|TRIGGERS tap or long press| AUTHZ[BiometricAuthManager]
    AUTHZ -->|GUARDED_BY biometric or device credential| DETAIL[field.detail]
    DETAIL --> COPY[action.field.copy]
    COPY -->|SCHEDULES process-owned expiry| CLIPBOARD[ClipboardCleanupCoordinator]
    DETAIL --> EDIT[action.field.edit]
    DETAIL --> DUP[action.field.duplicate]
    DETAIL --> MCP_TOGGLE[action.field.mcp-shareable]
    MCP_TOGGLE -->|WRITES| FIELD_REPO[FieldRepository.updateMcpShareable]
    EDIT --> CLAIM[field.editor Saving claim]
    TASK_EDITOR[field.editor.tasks] -->|SUBMITS title and per-task status| CLAIM
    CLAIM -->|WRITES once| FIELD_REPO
    FIELD_REPO -->|Saved closes editor| SESSION_DATA[SessionDataCoordinator]
    FIELD_REPO -->|Failed keeps draft and error| EDIT
    SESSION_DATA -->|REFRESHES| CARD
    REVIEW[screen.summary.fields] -->|GUARDED_BY one-flow authentication| SEND[action.send]
```

## Contacts and Editor Outcomes

```mermaid
flowchart TD
    CARD[contact.card] -->|ordinary tap| DETAIL[contact.detail adaptive modal]
    DETAIL -->|Edit| EDITOR[contact.editor]
    DETAIL -->|Copy email| CLIPBOARD[session clipboard coordinator]
    DETAIL -->|Create similar| EDITOR
    DETAIL -->|Change tag| TAGEDIT[contact.tag quick editor]
    DETAIL -->|Delete + confirm| DELETE[repository delete then Undo]
    DETAIL -->|Guided flow| FLOW[Select Fields or Review]
    EDITOR[contact.editor] --> CLAIM[contact.editor Saving claim]
    CLAIM -->|Add| CREATE[UserRepository.saveUserIfNotExists]
    CLAIM -->|Update with original email| UPDATE[UserRepository.updateUser]
    UPDATE -->|Updated or Unchanged| SAVED[Saved closes editor]
    UPDATE -->|EmailAlreadyExists or NotFound| FAILED[Failed keeps draft and error]
    CREATE -->|created| SAVED
    CREATE -->|duplicate or exception| FAILED
    SAVED -->|REFRESHES| CONTACTS[contact list]
```

## Requests and Draft Editor Outcomes

```mermaid
flowchart TD
    CARD[request.card] -->|READS stable ID| DETAIL[request.detail]
    LIST[request.list] -->|KEYED_BY stable ID| CARD
    DETAIL -->|READS| IDENTITY[mode, status, relative and absolute time]
    DETAIL -->|READS| PEOPLE[requester and complete recipients]
    DETAIL -->|READS| KEYS[complete requested fields and aliases]
    DETAIL -->|READS when persisted| DELIVERY[channel and per-recipient outcomes]
    DETAIL -->|pending Received with verified requester| MATCH[matching workspace]
    EDITOR[request.draft.editor] --> ADD[RequestDraftEditor.add]
    EDITOR -->|dirty Close or Back| DISCARD[confirm discard or keep editing]
    ADD -->|ADDED| DRAFT[outgoing request draft]
    ADD -->|DUPLICATE or INVALID| RETAIN[editor retains input and inline error]
    DRAFT -->|rotates transaction ID only when content changes| REVIEW[screen.summary.requests]
```

## Tag Editor Outcomes

```mermaid
flowchart TD
    EDITOR[tag.editor] --> VALIDATE[TagNameContract]
    VALIDATE -->|valid create| CREATE[TagRepository.addTag]
    VALIDATE -->|valid rename/recolor with original name| UPDATE[TagRepository.updateTag]
    CREATE -->|Created| SAVED[Saved selects persisted tag and closes editor]
    CREATE -->|AlreadyExists or Invalid| FAILED[Failed keeps tag draft and error]
    UPDATE -->|Updated or Unchanged| SAVED
    UPDATE -->|NameAlreadyExists, NotFound, or Invalid| FAILED
```

## MCP Field Policy

```mermaid
flowchart TD
    AGENT[mcp.client] --> CAP[Connector capability policy]
    CAP --> LIST[fields.metadata.read]
    CAP --> GET[fields.value.read]
    CAP --> WRITE[fields.write]
    LIST -->|MASKS sensitive alias| META[metadata response]
    GET --> POLICY[SensitiveFieldMcpPolicy]
    WRITE --> POLICY
    POLICY -->|shareable false| DENY[403 sensitive_field_not_mcp_shareable]
    POLICY -->|shareable true and existing sensitive type| ALLOW[read or update]
    POLICY -->|create or type transition| TYPE_DENY[403 sensitive field policy denial]
    PREVIEW[actions.preview] --> POLICY
    COMMIT[actions.commit] -->|rechecks current field| POLICY
```

## Review Delivery and Inbox Decisions

```mermaid
flowchart TD
    FIELD_REVIEW[screen.summary.fields] -->|READS all selected contacts| RECIPIENTS[summary.recipients]
    REQUEST_REVIEW[screen.summary.requests] -->|READS all selected contacts| RECIPIENTS
    FIELD_REVIEW --> METHOD{send channel}
    REQUEST_REVIEW --> METHOD
    METHOD -->|Cloud| CLAIM[CloudDeliveryController]
    CLAIM -->|GUARDED_BY readiness and one active attempt| CLOUD[CloudService]
    CLOUD --> RECEIPT[Sent / Partial / Failed receipt]
    RECEIPT --> REQUEST_HISTORY[persist Request channel and recipient outcomes]
    REQUEST_HISTORY -->|same stable request ID| RESTORE[restore receipt after recreation]
    RECEIPT -->|failed recipients only| RETRY[action.cloud.retry]
    METHOD -->|Email| PREPARE[Email attachment preparation]
    PREPARE -->|one claimed launch| SHARE[Android share sheet]
    SHARE -->|EMITS opened, not sent or delivered| EMAIL_RECEIPT[email handoff receipt]
    PREPARE -->|CLEARS after lease or next startup sweep| CLEANUP[email attachment cleanup]
    INBOX[request.inbox.item] -->|GUARDED_BY one decision claim| DECISION{Accept or Reject}
    DECISION --> INBOX_RECEIPT[terminal decision receipt]
```

## Edge Index

| Source ID | Relation | Target ID | Guard / condition | Primary owner |
|---|---|---|---|---|
| `login.submit` | `TRIGGERS` | `session.data.start` | Local authentication succeeds | `AuthenticationViewModel` |
| `session.data.start` | `LOADS` | `field.repository` | Session is authenticated | `SessionDataCoordinator` |
| `session.data.start` | `LOADS` | `user.repository` | Non-blocking | `SessionDataCoordinator` |
| `field.initial.snapshot` | `NAVIGATES_TO` | `screen.fields` | Five successes or load complete | `LoginScreen` |
| `field.card.tap` | `GUARDED_BY` | `device.authentication` | Sensitive type only | `FieldsScreen` |
| `field.card` | `MASKS` | `field.value`, `field.alias` | Sensitive and not authorized | `FieldValueTypes` |
| `field.detail.mcp-toggle` | `WRITES` | `field.isMcpShareable` | Authenticated detail flow | `FieldViewModel` |
| `contact.editor.update` | `WRITES` | `contact.repository` | Original email exists and updated email does not collide | `UserViewModel`, `UserRepositoryImpl` |
| `contact.tag.update` | `WRITES` | `contact.repository` | Target email exists | `UserViewModel`, `UserRepositoryImpl` |
| `contact.tag.create` | `WRITES` | `contact.tag.repository` | Shared validation passes and case-insensitive name is unique | `ContactTagViewModel`, `ContactTagRepositoryImpl` |
| `request.card` | `READS` | `request.detail` | Stable Request ID survives reorder and configuration recreation; detail discloses only persisted status/history | `RequestScreen`, `RequestDetailDialog` |
| `request.draft.editor` | `WRITES` | `outgoing.request.draft` | Saveable dirty draft; only `ADDED` dismisses, while duplicate or invalid input remains visible | `AddRequestDialog`, `RequestDraftEditor`, `RequestViewModel` |
| `request.workflow.draft` | `PERSISTS` | `owner.request.workflow` | Minimal identities only; owner match and vault codec required for restore | `RequestRepositoryImpl`, `RequestViewModel` |
| `tag.editor.create` | `WRITES` | `tag.repository` | Shared validation passes and case-insensitive name is unique | `TagViewModel`, `TagRepositoryImpl` |
| `tag.editor.update` | `WRITES` | `tag.repository` | Original name exists and target name does not collide | `TagViewModel`, `TagRepositoryImpl` |
| `summary.fields.send` | `GUARDED_BY` | `device.authentication` | Selection contains sensitive field | `SummaryFieldScreen` |
| `summary.recipients` | `READS` | `selected.contacts` | Every selected recipient is disclosed | `SummaryFieldScreen`, `SummaryRequestScreen` |
| `summary.cloud.send` | `GUARDED_BY` | `cloud.delivery.claim` | Cloud ready and no active or terminal ordinary send | `CloudDeliveryController` |
| `summary.cloud.retry` | `TRIGGERS` | `cloud.delivery` | Failed recipients only; request retry keeps transaction ID | `CloudDeliveryController` |
| `cloud.delivery.receipt` | `PERSISTS` | `request.delivery.history` | Outgoing Requests and received responses; channel, update time, and every recipient result | `RequestRepository` |
| `summary.email.share` | `EMITS` | `email.share-sheet-opened` | Does not prove sending or delivery | `EmailShareController` |
| `email.attachment.cleanup` | `CLEARS` | `email.attachment` | 24-hour live-process lease or next-start expired sweep | `EmailAttachmentCleanupCoordinator` |
| `field/contact.copy` | `SCHEDULES` | `clipboard.cleanup` | Process-owned timer; sensitive Field clips are marked | `ClipboardCleanupCoordinator` |
| `process.background` | `CLEARS` | `clipboard.current` | Only when the current clip still carries Shary's ownership token | `ClipboardCleanupCoordinator` |
| `request.inbox.decision` | `GUARDED_BY` | `request.decision.claim` | One item/decision; ambiguous failure retries the same decision only | `RequestInboxDecisionController` |
| `mcp.fields.get` | `GUARDED_BY` | `sensitive.policy` | Capability plus per-field flag | `LoopbackConnectorRouter` |
| `mcp.actions.commit` | `GUARDED_BY` | `sensitive.policy` | Rechecked at commit time | `CommitConnectorActionsUseCase` |
| `process.background` | `TRIGGERS` | `session.deadline` | Process lifecycle stop | `SessionTimeoutCoordinator` |
| `session.deadline` | `CLEARS` | `auth/cache/session-data` | Five minutes elapsed | `SessionTimeoutCoordinator` |

## Maintenance Check

Update this map when a screen gains or removes a primary action, navigation
changes, a security guard moves, a new field type changes rendering or sharing,
or startup/session ownership changes. Keep IDs stable when behavior is renamed
but its responsibility remains the same.
