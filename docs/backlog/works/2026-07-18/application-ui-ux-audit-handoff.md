# Application UI/UX audit continuation handoff

## Scope

Preserve the completed application-wide UI/UX audit as a resume-ready
checkpoint for a later implementation session. This handoff records the
authoritative source, current repository state, verified baseline, unresolved
risks, implementation order, and exact restart commands.

This is a documentation-only checkpoint. No application code was changed.

## Change list

1. Saved the July 17 audit as the canonical continuation source.
2. Recorded the current branch, commit, dirty-worktree boundary, and artifact status.
3. Reduced the audit to the next safe implementation slices and exit gates.
4. Preserved exact validation commands and known failures for later comparison.
5. Added the matching raw-order backup and both backlog index entries.

## Plan source

Primary source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]

Next planned step:
Phase 0 — containment and red gates.

Supporting test plan:
[[docs/backlog/works/2026-07-05/android-native-ui-testing-and-diagnosis-plan.md]]

## Current status snapshot

Snapshot date: 2026-07-18, Europe/Madrid.

- Repository: `C:\Users\marsu\AndroidStudioProjects\sharydemo`
- Branch: `connector-details`
- HEAD: `fcf4648`
- Worktree: heavily dirty before this handoff; 215 status entries after saving
  it, comprising 146 tracked-change entries and 69 untracked entries.
- The July 17 audit and raw-order backup are currently untracked.
- Both backlog indexes contain pre-existing uncommitted changes plus the new
  audit/handoff entries.
- No commit or push was performed.

Do not reset, clean, overwrite, or broadly reformat this worktree. Re-run
`git status --short` at resume time because the counts above are only a snapshot.
Treat all unrelated changes as user-owned.

## Completed work

The application sweep covered:

- Fields list/cards, add/update/recovery/detail UI, task and tag flows;
- Contacts list/cards, detail/editor behavior, identity and clipboard handling;
- Requests Received/Draft/Sent behavior, details, inbox decisions, and matching;
- field/request review, delivery channel selection, results, and receipts;
- authentication, top-level navigation, theme handling, loading and empty states;
- accessibility, adaptive layout, Flow collection, list identity, and performance risk;
- stale plans, source TODO markers, dead UI surfaces, and test coverage.

The recent Fields toolbar/card and shared permanent-search work is the approved
visual baseline. Preserve it unless a correctness or accessibility fix requires
a narrow change.

## Stop-the-line findings still unresolved

1. **Account isolation:** `SelectionCacheService.clearAllCaches()` does not
   clear all request, draft, owner, and identity state used across sessions.
2. **Wrong-recipient risk:** received-request responses use the general cached
   Contact selection instead of being locked to the request's requester.
3. **Workflow collision:** incoming requested fields and outgoing request-draft
   fields share `_draftFields`.
4. **Supported-API crash:** matching Undo/Redo uses `removeLast()`, which lint
   resolves to API 35 while `minSdk` is 26.
5. **Misleading disclosure/privacy:** review screens show one recipient while
   sending to all; email exposes group recipients and creates a plaintext JSON
   attachment; shared “data never stored” copy is inaccurate.
6. **Broken device gate:** `FieldDetailDialogTest` process-crashes because
   `CompactTagSelector` can resolve Hilt from a supposedly dependency-free UI path.
7. **Release transport boundary:** the main manifest globally enables
   cleartext traffic for a development emulator exception.
8. **Task editor crash:** blank task titles reach a throwing persistence encoder
   instead of inline validation.

The full evidence and file/line references remain in the primary audit. Do not
reconstruct these findings from chat history.

## Next implementation order

### Slice 0A — restore static and device gates

Implement as small independent patches:

1. Replace API-35-only matching history operations with API-26-safe operations.
2. Convert blank task edits into typed inline validation; no UI action may throw.
3. Split `CompactTagSelector` into a dependency-free renderer and Hilt route wrapper.
4. Move `10.0.2.2` cleartext configuration to debug/dev source sets, keep release
   cleartext disabled, and set `includeSubdomains="false"` explicitly.
5. Resolve the remaining lint error at `FieldViewModel.kt:607` without changing
   behavior unless a test proves a logic defect.

Slice 0A exit gate:

- `:app:lintDebug` reports zero errors;
- the complete instrumentation suite finishes without a process crash;
- matching Undo/Redo is exercised on API 26 and a current API;
- task blank/save/type-change tests pass;
- debug and release merged manifests prove the intended cleartext boundary.

### Slice 0B — isolate session and request-response state

1. Inventory and atomically clear every account-scoped cache on logout, timeout,
   password recovery, failed login, and account switch.
2. Add stable request identity and a dedicated immutable
   `ResponseDraft(requestId, requesterId, requesterAddress, matchedFieldIds)`.
3. Separate `activeReceivedRequest`, `responseDraft`, and
   `outgoingRequestDraft`; never reuse one field collection across modes.
4. Add reducer/service tests before changing screen composition.

Slice 0B exit gate:

- account A state cannot appear after account B signs in;
- a response can target only the requester recorded by its request;
- Received/Draft/Sent switching never copies or persists fields across workflows;
- configuration/process recreation retains only the intended stable draft state.

### Slice 0C — truthful, idempotent review and send behavior

1. Show every recipient and channel-specific privacy/retention facts.
2. Reduce per-recipient results to `Sent`, `Partial`, or `Failed`; never treat a
   non-empty all-error map as success.
3. Add terminal receipt state and idempotency for Send, Accept, and Reject.
4. Define email attachment cleanup and distinguish “composer opened” from delivery.

Do not start the broad detail/editor visual migration until Slices 0A–0C are
green. Continue with Phases 1–4 from the primary audit afterward.

## Resume procedure

From PowerShell:

```powershell
Set-Location 'C:\Users\marsu\AndroidStudioProjects\sharydemo'
& 'tools/codex/session-init.ps1'
Get-Content 'docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md'
Get-Content 'docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md'
git status --short
git branch --show-current
git rev-parse --short HEAD
```

Expected first decision:

- If the audit files and relevant source paths still match this handoff, begin
  Slice 0A with the smallest test-backed patch.
- If the branch, HEAD, or relevant files changed, revalidate the affected
  findings before implementation. Do not discard newer user work to recreate
  this snapshot.

## Validation baseline to compare, not assume

Recorded on 2026-07-17:

| Check | Recorded result |
|---|---|
| `tools/codex/session-init.ps1` | Passed |
| `:app:assembleDebug` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Instrumentation excluding `FieldDetailDialogTest` | Passed, 21 tests |
| Full instrumentation | Failed in the Field detail/update dependency path |
| `:app:lintDebug` | Failed with 4 errors and 110 warnings |

Re-run the following after implementation:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r com.shary.app.test/androidx.test.runner.AndroidJUnitRunner
```

The 2026-07-18 session-init check also passed. No build or instrumentation suite
was rerun merely to create this documentation-only handoff.

## Guardrails for the next session

- Keep local/offline Fields and Contacts usable when cloud availability changes.
- Keep unavailable cloud actions visible with a specific reason.
- Do not call email encrypted or delivered when only the composer opened.
- Do not claim cloud payloads are never stored; encrypted relay retention exists.
- Do not preserve list selection or request identity by filtered-list index.
- Keep Hilt and route ViewModels outside reusable UI renderers.
- Keep visual redesign separate from correctness patches and dead-code cleanup.
- Add direct unit tests for state transitions, identity, caching, and idempotency.
- Update the nearest application-layer README if implementation changes a module boundary.
- Preserve unrelated dirty-worktree changes.

## Result summary

The application UI/UX audit is safely handed off. A later session should open
this file first, then the primary audit, verify current repository drift, and
start with Slice 0A. The next deliverable is a clean lint/device gate followed
by session/recipient state isolation—not a visual-only dialog redesign.
