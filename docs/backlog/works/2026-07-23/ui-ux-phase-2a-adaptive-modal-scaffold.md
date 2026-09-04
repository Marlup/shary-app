# UI/UX Phase 2A Adaptive modal scaffold

## Scope

Implement only the first Phase 2 foundation: one reusable adaptive modal
scaffold for later Contact, Request, Field, and matching migrations. Do not
migrate entity-specific content in this slice.

## Change list

1. Added `SharyModalScaffold` under the shared Compose component boundary.
2. Added a pure width/complexity policy:
   - compact tasks use a bottom sheet below 600dp and a dialog from 600dp;
   - complex tasks use full-screen presentation below 840dp and a constrained
     dialog from 840dp.
3. Added a compact identity header with explicit Close or Back navigation.
4. Kept the body independently scrollable while the operation status and action
   footer remain visible.
5. Added `Idle`, `Validating`, `Saving`, `Saved`, and typed `Error` rendering.
6. Blocked dismissal and actions during validation/save, and required explicit
   confirmation before abandoning a dirty draft.
7. Added an automatically separated and confirmed destructive-action slot.
8. Made footer actions grow and stack at large font scales instead of relying
   on fixed-height, single-line controls.
9. Added pure JVM policy tests and Compose callback/semantics tests.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 2 item 1.

Continuation source:
[[docs/backlog/works/2026-07-20/ui-ux-phase-1f-clipboard-and-draft-recovery.md#Result summary]].

## Viability and complexity per change

- Presentation policy: low complexity and independently testable.
- Shared Compose host: medium complexity because full-screen, sheet, and dialog
  hosts must preserve one behavior contract across IME and window-size changes.
- Dirty/destructive confirmation: low-to-medium complexity; the scaffold owns
  confirmation UI while entity reducers continue to own draft data and save
  outcomes.
- Entity migrations: intentionally deferred. Mixing the scaffold foundation
  with Contact, Request, Field, or matching rewrites would make regressions
  difficult to attribute.

## Design decisions

- Use the current window container width rather than device model or orientation
  labels so split-screen can select an appropriate presentation.
- Treat matching and complex editors as full-screen until expanded width; use a
  sheet only for compact phone tasks.
- Keep business validation and persistence outside the scaffold. The scaffold
  renders operation state and enforces interaction safety but does not invent
  entity outcomes.
- Keep caller draft state in saveable or ViewModel-owned state. The scaffold
  preserves its confirmation state and does not serialize mutable domain
  objects.
- Require destructive confirmation at the shared boundary so later entity
  migrations cannot accidentally expose a one-tap destructive footer action.

## Implementation plan

1. Extract and test the presentation and dismissal policy. Completed.
2. Implement the three adaptive hosts and common header/body/footer layout.
   Completed.
3. Add operation-state feedback and interaction locking. Completed.
4. Add dirty-dismiss and destructive confirmation. Completed.
5. Add focused host-side and Compose test coverage. Completed.
6. Run proportional host validation and synchronize retrieval/backlog records.
   Completed with the residuals below.

## Validation plan

- Run focused scaffold policy tests.
- Compile the complete Android-test source set.
- Run the full JVM suite with forced recompilation.
- Run lint and assemble both application and Android-test APKs.
- Run the focused Compose device test when an emulator is connected.
- Run `git diff --check` and the repository session-init gate.

## Validation results

| Check | Result |
|---|---|
| `SharyModalPolicyTest` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:testDebugUnitTest --rerun-tasks` | Passed, 188 tests, 0 failures |
| `:app:lintDebug` | Passed, 0 errors and 108 warnings |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| `git diff --check` | Passed |
| Focused device instrumentation | Not run; `adb devices` returned no connected device |
| `tools/codex/session-init.ps1` final rerun | Blocked by a concurrent, unrelated `AGENTS.md` checksum change after the gate passed at session start |

The final session-init mismatch was not caused by this slice. `AGENTS.md`
changed during validation while `tools/codex/session-init.manifest.json`
retained its earlier checksum. The policy and manifest were left untouched.

## Result summary

Phase 2 item 1 is implemented as a reusable, tested scaffold without prematurely
migrating entity-specific screens. The next plan step is Phase 2 item 2:
migrate Contact detail/editor first, then Request detail/Add Request, followed
by Field update/recovery.

Host validation is clean. Device execution of the new Compose test remains
pending because no emulator is connected, and the independently modified policy
checksum must be reconciled by the owner of that policy edit.
