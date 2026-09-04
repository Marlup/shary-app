# Works Index

## 2026-09-04

1. **Unify Contacts/Requests with the Fields aesthetic + Contact grouping tags**
   Brought the Fields revised-prototype treatment to Contacts and Requests (full-width search, labeled full-width idle bottom buttons), and added a dedicated seeded Contact grouping-tag feature (Friends/Colleagues/Family/Vendor/Unknown) with Name/Tag/Added sort, tag chip on the contact card, editor tag selector, and detail quick-edit. Loading skeleton intentionally skipped for Contacts/Requests because they load synchronously from local DataStore. See [[docs/backlog/works/2026-09-04/unify-contacts-requests-fields-aesthetic.md]].

## 2026-08-30

1. **Fundamentals tutorial, legal drafts & account-recovery decision**
   Produced a plain-language tutorial of key audit terms (password-derived identity, Privacy Policy/ToS/ROPA/DPA/DPIA, non-transient backup, erasure-when-keys-lost, rate-limit/dedup atomicity, and the P1–P4 phase terms), drafted working Privacy Policy, ToS, and ROPA templates (marked for lawyer review), and evaluated account-recovery options with scoring — recommending option C (email-verified erase + offline-secret rotate). Recorded decisions: no Madrid asesor retained yet; drafts are working templates. See [[docs/backlog/works/2026-08-30/fundamentals-tutorial.md]], [[docs/backlog/works/2026-08-30/privacy-policy-draft.md]], [[docs/backlog/works/2026-08-30/terms-of-service-draft.md]], [[docs/backlog/works/2026-08-30/ropa-draft.md]], [[docs/backlog/works/2026-08-30/account-recovery-options.md]].
2. **Security & compliance audit (v1) + remediation plan**
   Evidence-based audit of `sharydemo` and `shary-backend` against the business draft as requirements, scored on a 9-point Security & Compliance Readiness scale. Result: overall 4.3/9 (Fragile) — Security 6.0, Compliance 3.25, Reliability 3.4. Top findings: password-derived identity (PBKDF2 200k) is the dominant risk; no privacy/ToS/ROPA/DPA/DPIA artifacts; no backup of non-transient data; no identity erasure when keys are lost; no tier/monetization enforcement. A phased remediation plan (P0 legal floor → P1 reliability → P2 security → P3 monetization → P4 hardening) targets Strong (7+). See [[docs/backlog/works/2026-08-30/security-compliance-audit.md]] and [[docs/backlog/works/2026-08-30/remediation-plan.md]].
2. **Business, monetization & continuity draft (v1)**
   Produced a complete planning draft for Shary: freemium-subscription monetization with a usage-based MCP add-on (anchored to Obsidian, Bitwarden, Standard Notes, 1Password, Proton, Claude/OpenAI), pros/cons of each model, Spain/Madrid legal framework (GDPR, LOPDGDD 3/2018, LSSI-CE 34/2002, TRLGDCU 1/2007, TRLC 1/2020), company-type recommendation (S.L.), minimal vs advanced launch checklist, hard-frozen per-tier limits and data-usage rules, a cost model for 1/100/1k/10k/100k/1M users, critical cloud-infrastructure maintenance items, additional aspects, and five continuity/termination protocols (outage, breach, solvent wind-down, insolvency, key compromise). See [[docs/backlog/works/2026-08-30/business-and-monetization-draft.md]].

## 2026-07-23

1. **UI/UX Phase 2C Request detail and Add Request migration**
   Migrated Request detail and requested-field drafting onto the adaptive modal scaffold; added truthful Received/Sent status, dual timestamps, requester/recipient disclosure, complete fields, match progress, channel and per-recipient outcomes, a pending-received Match action, saveable dirty drafts, typed outcome handling, and IME submission. All 191 JVM tests, lint with zero errors, both APK assemblies, 12 focused device tests, and the complete 50-test API 37 instrumentation suite pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 2 item 2, Request detail/Add Request. See [[docs/backlog/works/2026-07-23/ui-ux-phase-2c-request-detail-add-request.md]].
2. **UI/UX Phase 2B Contact detail and editor migration**
   Migrated ordinary Contact detail and add/copy/update editing onto the adaptive modal scaffold; added stable email-keyed detail state, truthful local metadata, Edit/Copy/Create similar/confirmed Delete with Undo, a separate guided send CTA, saveable drafts, IME actions, dirty-dismiss protection, and focused regression coverage. All 188 JVM tests, lint with zero errors, both APK assemblies, 10 focused device tests, and the complete 45-test API 37 instrumentation suite pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 2 item 2, Contact detail/editor. See [[docs/backlog/works/2026-07-23/ui-ux-phase-2b-contact-detail-editor.md]].
3. **Checkpoint review policy synchronization**
   Clarified the user-added mandatory index-first checkpoint rule, mirrored it in the AI retrieval guide, refreshed the session-init manifest, and immediately exercised the new retrieval order before continuing Phase 2. Session-init passes. See [[docs/backlog/works/2026-07-23/checkpoint-review-policy-sync.md]].
4. **UI/UX Phase 2A adaptive modal scaffold**
   Added one reusable full-screen/sheet/dialog scaffold with width-and-complexity routing, explicit Close/Back navigation, independent scrolling and sticky actions, typed operation feedback, dirty-dismiss protection, large-font action stacking, and confirmed destructive actions. All 188 JVM tests, Android-test compilation, lint with zero errors, and both APK assemblies pass; device execution is pending because no emulator is connected, and a concurrent unrelated `AGENTS.md` checksum change was left untouched. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 2 item 1. See [[docs/backlog/works/2026-07-23/ui-ux-phase-2a-adaptive-modal-scaffold.md]].

## 2026-07-20

1. **UI/UX Phase 1E Request and response delivery history**
   Persisted cloud channel, timestamp, and per-recipient results for every terminal outgoing-Request and received-response attempt; restored receipts by stable Request ID for failed-only retry after review recreation; and labelled partial/failed history truthfully. All 178 JVM tests, lint with zero errors, both APK assemblies, and the complete 38-test API 37 instrumentation suite pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 item 4. See [[docs/backlog/works/2026-07-20/ui-ux-phase-1e-request-delivery-history.md]].
2. **UI/UX Phase 1F lifecycle-independent clipboard and draft recovery**
   Moved Field/Contact clipboard expiry to process ownership with sensitive metadata and safe ownership-token clearing, then added encrypted, owner-scoped outgoing/response workflow identity snapshots and stable Request review routing for recovery after process death and re-authentication. All 185 JVM tests, lint with zero errors, both APK assemblies, and the complete 38-test API 37 instrumentation suite pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 items 2 and 5 plus the process-recreation exit gate. See [[docs/backlog/works/2026-07-20/ui-ux-phase-1f-clipboard-and-draft-recovery.md]].

## 2026-07-19

1. **UI/UX Phase 1A Field editor persistence outcomes**
   Added typed `Idle`/`Saving`/`Saved`/`Failed` editor state, synchronous duplicate-submit protection, owner-controlled dismissal, and failure-preserving drafts for Field add/copy/update/recovery. The complete JVM, lint, package, and 31-test API 37 instrumentation gates pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 item 3. See [[docs/backlog/works/2026-07-19/ui-ux-phase-1a-field-editor-outcomes.md]].
2. **UI/UX Phase 1B Contact editor identity and outcomes**
   Replaced email-edit upsert with an atomic original-email update contract, added collision/not-found outcomes, and made Contact add/copy/update sheets wait for typed persistence results while preserving failed drafts. The complete JVM, lint, package, and 34-test API 37 instrumentation gates pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 items 1 and 3. See [[docs/backlog/works/2026-07-19/ui-ux-phase-1b-contact-editor-outcomes.md]].
3. **UI/UX Phase 1C Request draft outcomes and identity**
   Added explicit Added/Duplicate/Invalid requested-key outcomes, retained the editor and draft on rejected input, and replaced filtered-list detail indexes with stable Request IDs and keyed list identity. The complete JVM, lint, package, and 37-test API 37 instrumentation gates pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 items 1 and 3. See [[docs/backlog/works/2026-07-19/ui-ux-phase-1c-request-draft-outcomes-and-identity.md]].
4. **UI/UX Phase 1D tag operation outcomes**
   Added shared tag-name validation, atomic typed create/rename outcomes, failure-preserving editor state, and persisted-success-only compact selection. The complete JVM, lint, package, and 38-test API 37 instrumentation gates pass. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 1 item 3. See [[docs/backlog/works/2026-07-19/ui-ux-phase-1d-tag-operation-outcomes.md]].

## 2026-07-18

1. **Application UI/UX audit continuation handoff**
   Saved a resume-ready checkpoint with the current dirty-worktree boundary, unresolved P0 findings, exact Slice 0A–0C order, validation baseline, and restart commands. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 0. See [[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md]].
2. **UI/UX Slice 0A progress and agent navigation**
   Completed the Slice 0A code changes: API-safe matching history, typed inline task validation, dependency-free injected tag rendering, a zero-error lint gate, and a debug-only emulator cleartext exception with release disabled. Added a curated task-to-code map plus module and Compose-scoped `AGENTS.md` guidance, and passed the complete 27-test instrumentation suite on API 37; the Slice 0A exit gate remains open only for API 26 execution because that system image is not installed. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 0 items 1 and 5, plus the task-validation part of item 6; [[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0A — restore static and device gates]] -> items 1-5. See [[docs/backlog/works/2026-07-18/ui-ux-slice-0a-and-agent-navigation.md]].
3. **UI/UX Slice 0B session and request-state isolation**
   Added atomic process-local account teardown, stable request identity, authenticated requester binding, and separate received-response/outgoing-request workflows with focused regression coverage. Response routing now fails closed on identity mismatch and immutable same-ID retries cannot reopen completed requests; device-global persisted stores remain an explicit owner-policy migration residual. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 0 items 2-3; [[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0B — isolate session and request-response state]] -> items 1-4. See [[docs/backlog/works/2026-07-18/ui-ux-slice-0b-session-and-request-state.md]].
4. **UI/UX Slice 0C truthful delivery and email cleanup**
   Added complete recipient and channel disclosure, idempotent cloud Send and inbox Accept/Reject claims, terminal per-recipient `Sent`/`Partial`/`Failed` receipts, failed-only retry, and a truthful Android share-sheet handoff with scoped 24-hour attachment cleanup plus next-start sweeping. All available JVM, Android-source, lint, and packaging gates passed; live instrumentation remains pending because no device is connected. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]] -> Phase 0 item 4 and Phase 1 items 2, 4, and 5; [[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0C — truthful, idempotent review and send behavior]] -> items 1-4. See [[docs/backlog/works/2026-07-18/ui-ux-slice-0c-truthful-delivery-and-email-cleanup.md]].

## 2026-07-17

1. **Application UI/UX deep sweep and rework plan**
   Audited Fields, Contacts, Requests, details/editors, matching, send review, navigation, accessibility, and performance; separated stop-the-line correctness/privacy defects from visual redesign and defined a phased, testable rework backlog. Plan source: [[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md]].

## 2026-07-15

1. **Fields revised prototype implementation**
   Replaced the title band with a search-and-overflow toolbar, made tag filters directly scrollable while sort stays fixed, and brought the field card and bottom actions in line with the approved prototype. See [[docs/backlog/works/2026-07-15/fields-revised-prototype-implementation.md]].
2. **Fields card compaction correction**
   Made key, value, selection, and metadata geometry fixed and ellipsized, while reducing and strengthening the static Key control. See [[docs/backlog/works/2026-07-15/fields-revised-prototype-implementation.md#Follow-up card compaction]].
1. **Compose preview catalog**
   Added live Android Studio previews for the representative Fields, Contacts, Requests, cloud, search, and navigation states. See [[docs/backlog/works/2026-07-15/compose-preview-catalog.md]].

## 2026-07-13

1. **Selection dock icon controls**
   Standardized fixed-size deselect and delete icon buttons for every shared command dock, removing selection counts from the deselect action. See [[docs/backlog/works/2026-07-13/selection-dock-icon-controls.md]].
2. **Field send-flow exit crash fix**
   Removed the invalid non-null CTA assertion during the `Fields` command-dock exit transition, so clearing the final selection no longer crashes. See [[docs/backlog/works/2026-07-13/field-send-flow-exit-crash-fix.md]].
2. **Guided field and user send-flow implementation**
   Implemented visible `Select Users` and `Select Fields` CTAs for the missing selection, preserved the review/send confirmation path once both sides are selected, and added direct CTA-state unit tests. Plan source: `[[docs/backlog/works/2026-07-04/guided-fields-users-review-flow-plan.md#Recommended implementation plan]]`.

## 2026-07-11

1. **Unified primary-screen controls and permanent search**
   Added adaptive permanent-search toolbars, stable ellipsized buttons, right-aligned bottom actions, consistent icon selection docks, request metadata filtering, and compact field-selector tag creation across Fields, Contacts, and Requests. Plan source: `[[docs/backlog/works/2026-07-11/unified-section-controls-plan.md#Implementation plan]]`.

## 2026-07-05

1. **Android native UI testing stack and diagnosis plan**
   Confirmed Compose UI Testing was already installed, added UI Automator plus a launcher smoke test, documented how to use both native Android UI tools, and defined a focused diagnosis-and-test plan for improving Shary UI and UX.
2. **Fields toolbar tuning and cloud-action contrast pass**
   Added tuneable shared button sizing for the compact Fields controls, replaced the cloud inbox affordance with a stronger-contrast CloudDownload action, and separated the bottom controls so the + button stays truly centered.
3. **Field command dock icon buttons**
   Added an icon-only visual mode to the shared dock, switched only FieldScreen to that compact treatment, and removed visible delete text while keeping explicit action semantics.
4. **Secure field types, session timeout, and startup loading**
   Implements task and sensitive field types, protected local/MCP access, five-minute background session closure, progressive post-login loading, and a development-only element/action map.

## 2026-07-04

1. **Connect tab, MCP master toggle, and shared accessibility correction batch**
   Added a dedicated `Connect` tab for assistant connectors, moved MCP management out of Settings, made MCP capability default to disabled with coordinator/work gating, and applied the shared top-bar/action-row/accessibility fixes across Fields, Users, Requests, and Connect. Plan source: `[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`.
2. **Inbox image review and Fields header compaction**
   Reviewed the corrected `inbox-fromuser` mockups, confirmed the implemented `Connect` tab order already matched the requested second-slot placement, then compacted the `Fields` header by moving field counts into overflow statistics and shortening the cloud-status strip. Plan source: `[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`.
3. **Fields single-row controls and compact editor pass**
   Collapsed the `Fields` screen into one compact control row, moved the add action to the bottom center, renamed `Fields info`, and simplified the add/update field sheet around default text fields with compact top-row type/tag selectors. Plan source: `[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`.
4. **Agent spec rework and module/script awareness**
   Reworked the repo's agent-facing documentation by adding a repo-specific retrieval layer, application-module awareness docs, and script contracts that keep general governance in Enhanced-BRAIN while making `sharydemo` easier to operate through direct Markdown retrieval.
5. **Fields bottom-toolbar button reorder**
   Moved the `Fields` cloud action to the left of the bottom add button, moved the search lens to the right, and shortened the add action label to `+`.
6. **Guided Fields and Users review-flow plan**
   Evaluated the current cached-selection review flow, proposed explicit next-step CTA variants for `Fields` and `Users`, and recommended a low-risk guided path that improves first-use clarity without replacing the existing send/review architecture.

## 2026-06-28

1. **Typed email fields and MCP type discovery**
   Added a canonical field-type registry, email field UX, immediate MCP type listing, and typed direct/batch writes with working alias and tag persistence across Android and the remote backend.
2. **Shary app intention and capabilities docs**
   Added executive and technical product overviews that explain Shary's
   purpose, supported capabilities, current architecture, and cloud-first MCP
   product boundary from the main docs tree.

## 2026-06-27

1. **Cloud-only MCP readaptation and legacy local-flow archive**
   Reworked the Android Settings MCP flow around cloud OAuth vault grants,
   auto-revoked deprecated local connectors, archived superseded local/desktop
   MCP docs and protocol assets, and aligned backend public docs to the same
   supported path.
## 2026-06-26

1. **OAuth MCP continuation readiness review**
   Resumed the June 21 public OAuth / multi-vault MCP checkpoint, validated the
   Android and isolated backend MCP worktrees, and reduced the remaining path
   to a single external Play App Signing certificate blocker. Plan source:
   `[[docs/backlog/works/2026-06-21/public-oauth-multi-vault-mcp-implementation.md#Deployment prerequisites]]`.
2. **Debug certificate remote OAuth test path**
   Confirmed the test-only workaround for the public OAuth / MCP flow by
   passing the read-only backend preflight with the explicit debug-certificate
   allowance, avoiding any cloud mutation or deployment. Plan source:
   `[[docs/backlog/works/2026-06-21/public-oauth-multi-vault-mcp-implementation.md#Deployment prerequisites]]`.

## 2026-06-21

1. **Public OAuth multi-vault MCP implementation**
   Implemented the no-ADB production path for Codex and Claude with OAuth
   PKCE, owner-selected vault grants, encrypted cloud operations, Android
   app-link/QR approval, FCM/WorkManager execution, per-write device
   confirmation, renewal/revocation, and multi-owner vault attachment.

## 2026-06-20

1. **Codex-operated MCP Phase 0 live test guide**
   Added a Codex execution contract for real-device dispatcher success,
   signed-out denial, bearer rate limiting, evidence reporting, and cleanup.
2. **Desktop bridge Phase 0 connector boundary**
   Extracted transport-neutral MCP dispatch, added locked/signed-out
   enforcement and bounded bearer-scoped rate limits, and preserved localhost
   MCP compatibility with focused tests.
3. **Agent connection checkpoint consolidation**
   Consolidated the MCP implementation, lifecycle UI, transport hardening, tests, and operator documentation; passed the policy gate and focused Android validation while retaining live device E2E as an explicit pending acceptance step.
4. **Production desktop bridge design**
   Designed an installable loopback MCP bridge with Android-initiated encrypted LAN pairing, capability-scoped agent profiles, lifecycle controls, threat mitigations, implementation phases, and no ADB production dependency.
5. **Repository routing and cloud MCP placement**
   Recorded the Android/backend workspace paths and selected an isolated Firebase MCP codebase inside the existing backend repository, with incremental backend modularization and shared Android connector execution.

## 2026-06-19

1. **Connector and MCP E2E quick-start guide**
   Added a detailed Windows-to-Android validation guide and completed the Settings workflow with command explanations, scrollable controls, a five-active-connector limit, bearer copy, individual revoke, revoke-all, audit-preserving lifecycle behavior, and focused tests.
2. **English-only agent policy and low-risk build warning cleanup**
   Made English mandatory for all agent-authored communication and removed low-risk AGP, Kotlin annotation-target, lifecycle, dropdown-anchor, and tooltip deprecations while preserving higher-risk migration flags.
3. **MCP checkpoint consolidation and fresh validation**
   Recovered the agent-access checkpoint on `connector-details`, passed the mandatory policy gate, rebuilt the Android Kotlin sources, and reran the focused MCP unit suite from scratch before commit.
4. **MCP PowerShell test suite**
   Converted the manual MCP E2E guide into standalone PowerShell scenarios plus an assertion runner, preparation/cleanup scripts, secure token inputs, mutation opt-in, and live public-suite validation.
5. **Real-agent MCP E2E guide**
   Added a local Codex/Claude-focused guide for testing natural-language tool selection, least-privilege reads, truthful denial handling, approval-gated writes, preview/commit, prompt-injection resistance, revocation, expiration, audit correlation, and cross-client consistency.
6. **Command-driven real-agent MCP suite**
   Added an executable Codex agent contract, structured scenarios, evidence recording and validation under `mcp-testing/real`, while preserving the direct PowerShell suite under `mcp-testing/protocol`.
7. **Same-device AI connector design**
   Designed local MCP, Android IPC, Shary-owned assistant, and optional remote-relay paths for connecting AI clients on Android without bypassing connector policy, audit, confirmation, or revocation.
8. **Local-agent MCP transport hardening**
   Narrowed the active architecture to MCP-capable agent tools and implemented case-insensitive headers, negotiation checks, request limits, explicit stateless methods, response security headers, and focused transport tests.

## 2026-06-15

1. **MCP server for agent access**  
   Added a Shary MCP JSON-RPC adapter and localhost `/mcp` HTTP endpoint over the existing connector security path, surfaced start/stop and pairing credentials in Settings, and covered tool discovery plus auth/policy behavior with targeted unit tests.

## 2026-06-14

1. **Cloud inbox review diff and error hardening**  
   Retested the previous cloud-readiness batch, added stronger unit and Compose source coverage, then implemented structured cloud error messaging, schema-aware inbox review dialogs, and local field-import diff planning before accept.

## 2026-06-13

1. **Android Layout Inspector setup**  
   Made the debug build explicitly inspectable and added a repo-local Android Studio Layout Inspector guide for visual UI diagnosis.
2. **Conversation recovery instruction and improvements predesign**  
   Added a reusable instruction for saving the latest two complete user-agent exchange pairs and started a feasibility/difficulty predesign table for visual, backend/cloud, and capability improvements.
3. **Cloud readiness status strip first batch**  
   Implemented the first low-regression improvement slice with qualitative cloud status labels, a visible Fields-screen status strip, disabled cloud-action feedback, clearer progressive loading copy, and cloud-status unit coverage.

## 2026-06-08

1. **List typed-field edit and detail layout fix**  
   Made list typed fields editable element-by-element in add/update dialogs, rendered typed list values vertically in field detail, and aligned detail actions in right-side vertical groups; verified by Kotlin app and Android-test compilation.

## 2026-05-31

1. **AI session-init policy enforcement and checksum gate**  
   Implemented canonical AI-policy loading order (`AGENT.md` first), added `CODEX.md`/`CLAUDE.md` adapters, and introduced manifest-backed startup checks plus prompt payload emission scripts.

## 2026-05-17

1. **Shary LLM/GenAI connector analysis and design evaluation**  
   Defined architecture options and recommended a local loopback connector MVP with capability-scoped auth, consent/policy controls, audit trail, and staged rollout; included optional `agent2user` design for future decision.
2. **MCP feasibility and ecosystem evaluation for Shary connector strategy**  
   Extended the connector design with an MCP-specific feasibility decision, comparative pros/cons against local loopback architecture, and a curated snapshot of available open-source MCP SDK/server tooling as of 2026-05-17.
3. **P0 Option A functional design freeze draft**  
   Added implementation-ready functional design for local connector behavior, capability matrix, consent modes, UX states, error semantics, and acceptance criteria before coding.
4. **P0 Option A technical design freeze draft**  
   Added implementation-ready technical design with architecture layers, module boundaries, local persistence schema, endpoint contracts, security controls, idempotency/concurrency strategy, and rollout gates.
5. **P0 Option A Phase 0 implementation skeleton**  
   Implemented connector core contracts (enums/models/envelopes/errors), repository interface plus DI-wired in-memory scaffold to unblock next-phase pair/preview/commit logic; verified by `:app:compileDebugKotlin`.
6. **P0 Option A Phase 1 and Phase 2 implementation pass**  
   Implemented pair/refresh use-cases and policy evaluator skeleton, then added loopback read-only transport endpoints (`/connector/v1/fields`, `/connector/v1/users`) with bearer auth + policy checks; verified by `:app:compileDebugKotlin`.
7. **P0 Option A Phase 3 and Phase 4 implementation pass**  
   Implemented write endpoints plus preview/commit action orchestration with confirmation gating, and added Settings UI surfaces for assistant connector pairing and recent activity; verified by `:app:compileDebugKotlin`.
8. **P0 Option A completion pass - tests and durable connector persistence**  
   Replaced in-memory connector repository with DataStore-backed durable persistence and added focused unit tests for policy evaluation and preview/commit orchestration behavior; compile and targeted unit tests pass.

## 2026-05-16

1. **Field typed-value UX and interaction redesign impact evaluation**  
   Viability/complexity assessment and implementation design for typed value auto-detection, link/url type support, card selection/detail behavior split, search visibility toggle, and command dock compaction.
2. **Implementation pass - card interaction, typed auto-detection, search toggle, and dock compaction**  
   Applied the evaluated UI/UX changes across Fields/Users shared surfaces, added `link_url` typed support and auto-detection on save, and validated by compiling `:app:compileDebugKotlin`.
3. **Completion pass - Request card interaction parity and typed-contract tests**  
   Extended the new selection/detail interaction model to Request cards and added focused unit tests for `FieldValueContract` `link_url` handling; compile and targeted tests pass.
4. **Selected-card quick action set for fields**  
   Added explicit single-selection icon actions for copy value, duplicate, edit value, edit tag, edit alias, and delete, reusing existing clipboard/update/delete behavior and validating via Kotlin compile.
5. **Field-detail dialog action placement and callback hardening**  
   Relocated field actions into the detail dialog beside their target sections (value/tag/alias) with labeled controls, moved field-level actions to bottom, hardened edit callback state handling, and updated AGENT instructions with mandatory unit-test verification guidance.
6. **Tag-edit crash fix and dialog callback test coverage**  
   Fixed `rememberSaveable` misuse with non-saveable `Tag` state in tag editor flow, renamed dialog action labels for unambiguous UX, and added Android Compose test coverage for detail-dialog action callback wiring.
