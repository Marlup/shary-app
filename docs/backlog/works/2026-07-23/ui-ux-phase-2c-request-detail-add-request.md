# UI/UX Phase 2C Request detail and Add Request migration

## Scope

Complete only the Request detail and Add Request portion of Phase 2 item 2.
Migrate both surfaces onto `SharyModalScaffold`, preserve stable Request
identity and typed draft outcomes, and expose only metadata and actions that
current persisted models and workflows can support truthfully.

## Change list

1. Replaced the information-only Request `AlertDialog` with the adaptive modal
   scaffold.
2. Made Request detail consume the current repository-backed `RequestDomain`
   re-resolved by stable Request ID and an explicit Received/Sent mode.
3. Added a mode/status header, relative and absolute timestamps, requester and
   complete recipient disclosure, the complete requested-field list with
   aliases, and received-response match progress.
4. Added truthful channel, delivery-update, and per-recipient result rendering.
   Legacy Sent history without evidence is labelled `Outcome not recorded`
   instead of implying delivery.
5. Added `Match fields` / `Continue matching` for pending received Requests.
   Reopening an already active Request preserves its existing match draft.
6. Replaced the fixed-height Add Request bottom sheet with the adaptive modal
   scaffold and clarified that it adds one requested field to the outgoing
   draft.
7. Made key, alias, advanced disclosure, validation, and error state saveable;
   added dirty-dismiss confirmation and key/alias IME submission.
8. Passed typed `RequestDraftFieldAddOutcome` results directly into the editor,
   retaining duplicate or invalid drafts and dismissing only after `ADDED`.
9. Extended `InputWithSuggestions` with optional supporting text and keyboard
   actions while preserving existing callers through defaults.
10. Added pure timestamp-formatting tests and focused Compose coverage for
    detail truthfulness, callbacks, typed editor outcomes, dirty dismissal, and
    IME submission.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 2 item 2, Request detail/Add Request.

Continuation source:
[[docs/backlog/works/2026-07-23/ui-ux-phase-2b-contact-detail-editor.md#Result summary]].

## Viability and complexity per change

- Detail migration: medium complexity because Received and Sent records share
  one model while their supported status, progress, and action surfaces differ.
- Match action: low-to-medium complexity. It reuses the existing verified
  requester guard and matching workspace and avoids resetting an active
  response draft.
- Add Request migration: medium complexity because the editor must preserve the
  existing synchronous typed draft outcome while adding modal restoration,
  dirty exit, suggestions, and IME behavior.
- Backend and repository contracts: unchanged.
- Expiry: not rendered because `RequestDomain` does not persist the configured
  cloud expiry.
- Sent retry, review, and history deletion: not invented. The current Requests
  screen has no safe history-to-draft restoration or deletion contract; these
  remain with Phase 2 item 3 and its visible outgoing-draft/state-action work.

## Design decisions

- Use explicit `RequestListMode` for presentation instead of inferring mode
  from legacy flags.
- Keep stable Request ID in `RequestsScreen` and pass the freshly resolved
  domain record to the leaf detail renderer.
- Label old Sent records without persisted outcomes as
  `Outcome not recorded`.
- Label the email channel `Email handoff`; chooser launch is not presented as
  confirmed sending or delivery.
- Keep the only new detail action to the already supported pending-received
  matching flow.
- Name the editor `Add requested field` and its action `Add to draft` because it
  mutates the outgoing draft rather than sending a complete Request.

## Implementation plan

1. Migrate Request detail content and truthful state rendering. Completed.
2. Wire match progress and the pending-received matching action. Completed.
3. Migrate Add Request to the shared scaffold and typed outcome API. Completed.
4. Add restoration, dirty-dismiss, IME, formatting, and detail tests. Completed.
5. Run focused and full host/device validation. Completed.

## Validation plan

- Compile production, JVM-test, and Android-test Kotlin.
- Run focused Request draft and timestamp tests.
- Run the complete JVM suite with forced recompilation.
- Run lint and build both APKs.
- Run focused Request/modal instrumentation.
- Run the complete available instrumentation suite.
- Re-run session-init and `git diff --check`.

## Manual verification procedure

1. Open Received and Sent Request detail and verify mode, status, people,
   complete requested fields, and both timestamp forms.
2. For a pending received Request, start matching from detail, reopen detail,
   and confirm `Continue matching` preserves progress.
3. Open legacy Sent history and verify it does not claim a channel, outcome, or
   delivery that was not persisted.
4. Enter an Add Request key and alias, press Close, and verify Keep editing
   preserves the draft while Discard exits.
5. Submit from the key or alias IME action and verify only `ADDED` closes the
   editor; duplicate/invalid outcomes retain input.
6. Repeat the Phase 2 exit matrix at compact widths, landscape, font scale 2.0,
   visible IME, TalkBack, and keyboard/DPAD before closing the whole phase.

## Validation results

| Check | Result |
|---|---|
| Production, JVM-test, and Android-test Kotlin compilation | Passed |
| Focused Request JVM tests | Passed |
| `:app:testDebugUnitTest --rerun-tasks` | Passed, 191 tests, 0 failures |
| `:app:lintDebug` | Passed, 0 errors and 106 warnings |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Focused Request + modal instrumentation | Passed, `OK (12 tests)` |
| Complete instrumentation suite | Passed, `OK (50 tests)` on API 37 / Android 17 |

## Result summary

The Request detail and Add Request portion of Phase 2 item 2 is complete.
Request detail now provides truthful, complete supported history and response
progress, while the requested-field editor uses deterministic typed outcomes,
saveable drafts, IME actions, and dirty-dismiss protection.

The next bounded plan step is Field update/recovery, completing Phase 2 item 2.
The outgoing-draft panel and remaining state-dependent Request actions stay in
Phase 2 item 3. API 26 and the complete Phase 2 interaction/accessibility matrix
remain pending because the API 26 system image is unavailable and those are
phase-level exit gates.
