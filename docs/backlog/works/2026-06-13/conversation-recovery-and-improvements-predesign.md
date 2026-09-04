# Conversation recovery instruction and improvement predesign

## Scope

Start a durable predesign list for Shary Android visual, backend, and capability improvements, and save a reusable instruction for preserving the last two complete user-agent message pairs.

This is a planning artifact only. No app behavior was changed.

## Conversation recovery instruction

For future non-trivial Shary planning or implementation sessions, save the last two complete user-agent exchange pairs in a local markdown recovery file.

Recommended artifact name:

- `docs/backlog/works/<YYYY-MM-DD>/last-two-user-agent-exchanges.md`
- `docs/backlog/works/latest-user-agent-exchanges.md` as the stable pointer to the latest recovery file or latest recovery content.

Recommended behavior:

- Capture only complete pairs: user message plus agent response.
- Keep exactly the latest two pairs.
- Overwrite the file when newer pairs are available instead of appending indefinitely.
- Preserve the full text of each pair unless the user explicitly asks for redaction.
- Mark the file as local planning context, not as a product requirement, backend contract, or source of frozen decisions.
- Do not store secrets, tokens, private keys, or credentials if they appear in the exchange; replace them with `[redacted secret]`.
- Use the file only to recover immediate context after interruption or handoff.
- Ignore the recovery content in Git. Commit the instruction and folder/index structure, but keep captured exchange text local.

Suggested markdown shape:

```markdown
# Last Two User-Agent Exchanges

Updated at: <local timestamp>
Scope: local Shary planning context
Retention: latest two complete user-agent pairs

## Pair 1

### User

<full user message>

### Agent

<full agent response>

## Pair 2

### User

<full user message>

### Agent

<full agent response>
```

ROI:

- High for handoff and interruption recovery.
- Low implementation cost because it is a small markdown artifact.
- Medium privacy sensitivity because user prompts and agent responses can contain operational context.
- Best used as short-lived context, not as a permanent transcript archive.

## Predesign scoring

Feasibility:

- High: fits current app structure and known patterns.
- Medium: needs cross-layer changes or more design decisions.
- Low: needs backend changes, security review, or new infrastructure first.

Difficulty:

- Low: mostly UI/docs or localized ViewModel/repository changes.
- Medium: multiple app layers, tests, and migration/contract updates.
- High: backend contract, security, data model, or external-client integration.

## Improvement predesign table

| Area | Improvement | User value | Feasibility | Difficulty | Predesign notes |
|---|---|---|---|---|---|
| Visual | Cloud readiness strip | Makes offline, unverified, ready, syncing, and backend-rejected states visible without hiding actions. | High | Low | Reuse existing `CloudUiState` reason model; show concise inline state and tap/click feedback for blocked cloud actions. |
| Visual | Request inbox review dashboard | Gives a controlled review surface before importing cloud data. | High | Medium | Build on existing inbox/review pattern; include sender, payload preview, accept, reject, cancel, and loading/error states. |
| Visual | Visual diff before import | Shows added, changed, duplicated, ignored, and conflicting fields before commit. | Medium | Medium | Needs comparison model between incoming payload and local fields; high trust value. |
| Visual | Detail dialog v2 sections | Makes field detail clearer as value, alias, tags, history, cloud status, and security sections. | High | Medium | Continue the current left content plus right action column pattern. Avoid adding actions back to top chrome. |
| Visual | Card density modes | Lets users scan many fields or inspect rich details without switching screens. | Medium | Medium | Add compact, normal, expanded display modes; persist preference locally. |
| Visual | Progressive loading banner | Explains that first fields are ready while the rest decrypt/load in background. | High | Low | Aligns with progressive field loading and avoids generic spinner fatigue. |
| Visual | Field grouping and sections | Organizes fields by tag, source, user, request, or security level. | Medium | Medium | Needs clear grouping priority and search/filter behavior; likely best after card density modes. |
| Visual | Field history timeline | Shows created, edited, copied, shared, accepted, rejected, and restored events. | Medium | High | Needs audit/event persistence before UI can be complete. |
| Backend/cloud | Structured backend error mapping | Converts backend errors into precise app states and messages. | High | Medium | Map `rate_limited`, `unsupported_schema_version`, auth failures, stale token, and decision rejection into typed exceptions/UI states. |
| Backend/cloud | Redacted diagnostics panel | Helps debug backend failures without exposing sensitive payloads. | High | Medium | Capture endpoint, status, correlation id, structured error, and redacted body snippet. |
| Backend/cloud | Decision audit ledger | Records accepted, rejected, canceled, imported, failed, and retried cloud decisions. | Medium | High | Requires durable local event model and possibly backend correlation fields. |
| Backend/cloud | Backend health contract check | Confirms service online, schema version, auth mode, and endpoint availability. | Medium | Medium | Could run at app start or from settings; must stay advisory so offline-first local flow remains usable. |
| Backend/cloud | Cloud inbox lifecycle states | Tracks received, previewed, accepted, partially accepted, rejected, expired, and sender revoked. | Medium | High | Requires state model, UI mapping, tests, and backend alignment if lifecycle persists remotely. |
| Backend/cloud | Schema migration support | Allows safe evolution of request payloads and field formats. | Medium | High | Extend current `schema_version` contract with compatibility warnings and migration strategy. |
| Backend/cloud | Conflict resolution engine | Handles local-vs-remote divergence through keep local, accept remote, duplicate, merge, or ignore. | Medium | High | Needs deterministic merge rules and strong preview/undo behavior. |
| Backend/cloud | Bounded post-verification retry | Fixes stale-token failures after email verification. | High | Medium | Reload user, force token refresh, retry once, then show exact failure. |
| Capability | Connector paired-clients UI | Lets users see, revoke, lock, rotate, and audit local assistant clients. | High | Medium | Continues existing connector roadmap; Settings is the natural entry point. |
| Capability | Real localhost connector listener | Makes the connector usable by external local tools instead of only in-process routing. | Medium | High | Needs transport security, lifecycle management, integration tests, and explicit user consent. |
| Capability | Secure share bundles | Sends selected fields as encrypted, time-limited bundles. | Medium | High | Requires payload model, expiration/revocation policy, and careful cloud/backend contract. |
| Capability | Field templates | Creates reusable packs such as identity, payment, emergency, travel, and work. | High | Medium | Mostly app-side data modeling and UI; strong onboarding value. |
| Capability | One-time reveal mode | Shows sensitive values briefly after biometric/local unlock and auto-hides them. | Medium | Medium | Needs UX/security decisions around clipboard, screenshot, timeout, and accessibility. |
| Capability | Encrypted local backup and restore | Gives users recovery and migration without cloud dependency. | Medium | High | Needs export/import preview, conflict handling, and encryption key handling. |
| Capability | Import sandbox | Keeps received data quarantined until accepted. | High | Medium | Extends the existing review-before-import preference and reduces accidental mutation risk. |
| Capability | Qualitative safety labels | Labels items as local-only, cloud-ready, pending review, stale token, unsupported schema, or sender unknown. | High | Low | Avoid numeric scoring; can be introduced incrementally in cards and detail dialogs. |

## First recommended implementation batch

1. Cloud readiness strip.
2. Progressive loading banner.
3. Qualitative safety labels.
4. Structured backend error mapping.
5. Request inbox review dashboard.
6. Visual diff before import.

This order starts with visible, low-regression UI clarity, then adds backend-facing precision and review safety.

## Validation plan for future implementation

- Compile with `./gradlew :app:compileDebugKotlin`.
- Add or update ViewModel/domain tests for backend error mapping and lifecycle transitions.
- Add Compose tests for request inbox review, blocked cloud action feedback, and visual diff actions where practical.
- For connector or backend-contract work, also compile Android test sources with `./gradlew :app:compileDebugAndroidTestKotlin`.

## Open questions

Resolved decisions:

- Use a stable pointer: `docs/backlog/works/latest-user-agent-exchanges.md`.
- Ignore captured recovery content in Git. The durable committed artifact is the instruction, not the private exchange text.
- Use split field history: local audit for device events and cloud decision audit for backend/request lifecycle events. This keeps offline-first behavior intact while preserving backend traceability.
- Require visual diff only when conflicts, duplicates, overwrites, or schema migrations are detected. For clean imports, show a concise preview and keep the full diff available on demand.
