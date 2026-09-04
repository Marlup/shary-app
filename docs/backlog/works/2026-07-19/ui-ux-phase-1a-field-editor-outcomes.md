# UI/UX Phase 1A field editor persistence outcomes

## Scope

Continue the application-wide UI/UX plan after Slice 0C with the smallest
independent part of Phase 1 item 3: make Field add, copy, update, and recovery
editors wait for an explicit persistence outcome instead of dismissing and
showing success immediately after dispatch.

The worktree was already heavily dirty. This patch preserves unrelated changes
and stays within Field editor state, Field screen wiring, focused tests, and
the matching agent/product documentation.

## Change list

1. Added typed `Idle`, `Saving`, `Saved`, and `Failed` Field editor operation
   states for Add, Update, and Recover.
2. Added synchronous operation claims so rapid duplicate submissions cannot
   launch a second repository write while one is active.
3. Added editor-specific ViewModel submission methods while preserving the
   existing non-editor add/update paths used by matching and quick actions.
4. Field add/copy/update/recovery sheets now remain open until `Saved`, keep the
   draft and error visible after `Failed`, and disable dismissal and repeat
   actions while saving.
5. Removed optimistic success Toasts and callback-driven dismissal from the
   reusable Field editor renderers.
6. Added pure controller regression tests and a Compose assertion proving Save
   submits without dismissing the renderer itself.
7. Corrected the Slice 0C Request reject test to assert its real same-frame
   latch behavior: the confirmation action disappears after submission and the
   restored decision actions remain disabled.
8. Updated code navigation and active product/action documentation.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 item 3.

Continuation source:
[[docs/backlog/works/2026-07-18/ui-ux-slice-0c-truthful-delivery-and-email-cleanup.md#Result summary]].

## Viability and complexity per change

- Typed editor state: low-to-medium complexity and directly JVM-testable.
- Owner-controlled dismissal: medium complexity because add, copy, update, and
  recovery previously mixed local sheet state with one-shot ViewModel events.
- Full repository result-contract migration: intentionally deferred. Changing
  every repository mutation in one patch would cross Fields, Contacts,
  Requests, tags, imports, and connector callers and would be a broad
  regression surface in the current dirty worktree.

## Design decisions

- Operation claims transition to `Saving` synchronously before launching a
  coroutine. A second claim is rejected until the active operation terminates.
- Terminal results are accepted only when operation and normalized key match
  the active claim, preventing stale completion from closing another editor.
- Reusable editor renderers submit drafts but do not infer persistence success.
  The Field screen owns terminal dismissal.
- A failed or duplicate-key write leaves the sheet and entered draft intact and
  exposes the failure inline for correction or retry.
- Existing quick actions and matching imports retain their established event
  path; this slice changes only full editor submissions.

## Implementation plan

1. Add and test the pure operation-state controller. Completed.
2. Translate editor repository exceptions and duplicate results into typed
   terminal state in `FieldViewModel`. Completed.
3. Wire Field sheets to saving/failure state and owner-controlled dismissal.
   Completed.
4. Update the closest Compose regression and active docs. Completed.
5. Run focused and full static/JVM/package validation. Completed for the
   locally available gates listed below.

## Validation plan and results

| Check | Result |
|---|---|
| `FieldEditorOperationControllerTest` | Passed, 4 tests |
| `:app:compileDebugKotlin` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:lintDebug` | Passed with zero errors |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Targeted Field editor instrumentation | Passed, 4 tests on API 37 |
| Complete instrumentation suite | Passed, 31 tests on API 37 |
| `git diff --check` | Passed before final validation-record update |

Manual/device verification remains:

1. Force add/update/recovery persistence failures and confirm the sheet retains
   all entered values with an inline error.
2. Rapid-tap Add, Save, and Recover and confirm one repository operation.
3. Confirm successful add/copy/update/recovery closes once and refreshes the
   repository-backed Field list.

## Result summary

Phase 1 item 3 is advanced for the full Field editor path. Field sheets no
longer claim success or dismiss at callback time; they wait for typed terminal
state, retain drafts on failure, and block duplicate writes while saving.

The broader repository result-contract migration and equivalent Contact,
Request, and tag editor treatment remain future Phase 1 slices. The complete
device suite is green on API 37; API 26 execution remains a separate existing
Slice 0A residual because that system image is not installed.
