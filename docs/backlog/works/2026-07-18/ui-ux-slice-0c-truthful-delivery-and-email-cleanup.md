# UI/UX Slice 0C truthful delivery and email cleanup

## Scope

Complete the next containment slice from the application-wide UI/UX audit:
disclose the complete recipient set and channel behavior, reduce cloud results
truthfully, make send and inbox decisions idempotent, and define the lifecycle
of readable email attachments without treating Android chooser launch as
delivery.

The worktree was already heavily dirty. The changes remain narrow to the
review/delivery, request-inbox decision, and email attachment boundaries;
unrelated user-owned changes were preserved.

## Change list

1. Both review screens now render every recipient and channel-specific
   encryption, retention, readability, and recipient-visibility facts.
2. Added a process-local cloud delivery controller with synchronous attempt
   claims, stable request transaction identity, per-recipient outcomes, and
   exact `SENT`, `PARTIAL`, or `FAILED` reduction.
3. Missing and all-error result maps now fail truthfully. Explicit retry sends
   only failed recipients and retains earlier acknowledgements.
4. Cloud review stays in a terminal receipt until the user selects Done or an
   eligible retry; rapid ordinary Send taps cannot start duplicate work.
5. Accept and Reject now use one atomic decision claim, same-decision retry
   after ambiguous failure, and a terminal receipt that separates local import
   from backend acknowledgement.
6. Email sharing now uses `Preparing -> Prepared -> Launching -> Share sheet
   opened` state. It never reports sent or delivered because Android supplies
   no such receipt.
7. Readable JSON attachments are adopted into a dedicated FileProvider cache
   directory, deleted immediately after chooser-launch failure, scheduled for
   deletion after 24 hours while Shary runs, and swept on the next startup if
   they expired while the process was absent.
8. Added direct controller, reduction, privacy-copy, path-safety, restart, and
   cleanup tests plus synchronized application/design/navigation docs.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 0 item 4 and Phase 1 items 2, 4, and 5.

Continuation handoff:
[[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0C — truthful, idempotent review and send behavior]]
-> items 1-4.

## Viability and complexity per change

- Recipient and privacy disclosure: low complexity and high trust value. The
  screens already own the complete recipient list, so no transport change was
  needed.
- Cloud receipt reduction and retry: medium complexity. A pure controller keeps
  normalization, acknowledgement semantics, idempotency, and partial retry
  deterministic outside Compose.
- Inbox decision idempotency: medium complexity and security-sensitive. The
  claim must occur before launching the coroutine, and ambiguous failures must
  not allow the opposite decision.
- Email handoff state: medium complexity. Android can confirm chooser launch
  only, so delivery status deliberately stops there.
- Attachment cleanup: medium complexity and privacy-sensitive. Exact deletion
  while the process is absent is not guaranteed by the current architecture;
  UI copy therefore states the real 24-hour schedule plus next-start sweep.

## Design decisions

- `SUCCESS`, `STORED`, and same-transaction `EXISTS` are acknowledged cloud
  outcomes. Every other status is failed.
- A non-empty map containing only errors is `FAILED`, never success.
- Request retry requires the original stable transaction ID. Unexpected result
  recipients are ignored and missing attempted recipients become `ERROR`.
- Terminal receipts are process-local and contain recipient/status metadata,
  not payload field values.
- Email is a readable sharing handoff. Multiple addresses remain in `To` and
  their possible mutual visibility is disclosed rather than hidden behind an
  encryption claim.
- The FileProvider exposes only `cache/email-attachments/`; attachment IDs are
  canonical UUIDs and cannot resolve outside that directory.

## Implementation plan

1. Add pure privacy, cloud-delivery, email-handoff, and inbox-decision models
   with direct tests. Completed.
2. Wire terminal state and failed-recipient retry into both summary screens and
   request inbox review. Completed.
3. Implement the scoped attachment store, cleanup coordinator, startup sweep,
   and one-shot chooser claim. Completed.
4. Run focused and full JVM tests, Android source/build gates, lint, and device
   discovery. Completed for every locally available gate.
5. Record device/manual residuals and synchronize module, navigation, and
   architecture documentation. Completed.

## Validation plan and results

| Check | Result |
|---|---|
| Focused Slice 0C JVM suite | Passed, 33 tests across 8 classes before final copy clarification; focused copy test rerun after clarification |
| Full `:app:testDebugUnitTest` | Passed, 139 tests; zero failures/errors/skips |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:lintDebug` | Passed with zero errors; 108 existing warnings remain |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| `git diff --check` | Passed |
| `adb devices -l` | No connected emulator/device; instrumentation not run |

The Compose chooser launch and live cloud transport are not meaningful JVM
tests. Pure controller tests cover rapid claims, terminal transitions, partial
retry, all-error maps, request identity, and cleanup policy. Manual/device
verification remains:

1. Send cloud data to multiple recipients with all-success, partial, and
   all-failed responses; verify the receipt and failed-only retry.
2. Rapid-tap Send, Accept, and Reject; verify one transport operation and one
   terminal receipt.
3. Open and cancel the Android share sheet; verify Shary reports only `Share
   sheet opened`, never sent/delivered, and the readable attachment is scoped to
   the dedicated FileProvider path.
4. Advance or wait past the lease while Shary runs, then repeat across process
   restart; verify live cleanup and next-start expired sweep.

## Result summary

Slice 0C is implemented and all available static, JVM, Android-source, lint,
and packaging gates are green. Reviews now disclose the real recipients and
channel behavior; cloud outcomes are terminal and retryable without false
success; inbox choices are one-shot; and email stops truthfully at chooser
handoff with a bounded attachment lifecycle.

Live instrumentation is pending only because no device is connected. The
broader Phase 1 persistence/editor work, Slice 0B's device-global account-store
ownership residual, and Slice 0A's API 26 execution residual remain separate.
