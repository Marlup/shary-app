# UI/UX Phase 2B Contact detail and editor migration

## Scope

Complete only the Contact portion of Phase 2 item 2 by migrating ordinary
Contact detail and add/copy/update editing onto `SharyModalScaffold`. Preserve
the existing repository contracts, typed persistence outcomes, guided send
flow, clipboard ownership, and reversible deletion behavior.

## Change list

1. Replaced the information-only Contact `AlertDialog` with the adaptive modal
   scaffold.
2. Keyed the open detail state by normalized email and re-resolved the current
   entity from the repository-backed list instead of retaining a mutable domain
   object.
3. Added truthful detail metadata: alias/email fallback, full email, local-vault
   availability, and the real added date.
4. Added ordinary-list management actions for Edit, Copy email, Create similar,
   and confirmed Delete.
5. Reused the existing Undo snackbar after detail deletion.
6. Added a visually separate guided-flow action that selects only the opened
   Contact and routes to Select Fields, Field review, or Request review through
   the existing CTA policy.
7. Migrated add, duplicate, and update editing to the adaptive scaffold.
8. Made alias/email drafts saveable, added alias Next and email Done IME
   behavior, preserved repository collision errors, and required confirmation
   before abandoning edited input.
9. Added Contact detail callback/metadata coverage and expanded editor coverage
   for IME submission, saving locks, and dirty dismissal.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 2 item 2, Contact detail/editor.

Continuation source:
[[docs/backlog/works/2026-07-23/ui-ux-phase-2a-adaptive-modal-scaffold.md#Result summary]].

## Viability and complexity per change

- Leaf detail migration: medium complexity because actions must remain available
  from ordinary card taps without depending on selection-dock state.
- Editor migration: medium complexity because save-result ownership remains in
  `UserViewModel` while the scaffold owns interaction locking and dirty exit.
- Guided flow: low-to-medium complexity. It reuses the tested CTA and review
  routes and selects only the opened Contact before navigation.
- Remote status: not implemented. `UserDomain` contains no verified or cloud
  availability fields, so the UI truthfully states local-vault availability.

## Design decisions

- Treat normalized email as the current stable Contact identity because the
  repository update contract explicitly preserves original-email identity.
- Keep Edit as the sticky primary management action and Delete in the scaffold's
  separated, confirmed destructive region.
- Keep Copy email and Create similar in the scrollable management section.
- Present guided sharing in its own highlighted section so entity management and
  workflow progression are not conflated.
- Preserve the original `dateAdded` on update and assign it only for new
  Contacts.

## Implementation plan

1. Migrate Contact detail rendering and actions. Completed.
2. Wire ordinary-list callbacks, stable detail identity, Undo, and guided flow.
   Completed.
3. Migrate the shared Contact editor and saveable draft fields. Completed.
4. Add focused Compose regression coverage. Completed.
5. Run focused and full validation gates. Completed.

## Validation plan

- Compile production and Android-test Kotlin sources.
- Run the complete JVM suite with forced recompilation.
- Run lint and build both APKs.
- Run focused Contact/scaffold instrumentation.
- Run the complete available instrumentation suite.
- Run session-init and `git diff --check`.

## Manual verification procedure

1. Open an unselected Contact from the ordinary list and verify Edit, Copy
   email, Create similar, Delete, and the guided-flow action are reachable.
2. Edit alias and email, press Back/Close, and verify the discard confirmation
   preserves the draft when cancelled.
3. Force a duplicate-email result and verify the editor remains open with the
   collision message.
4. At font scale 2.0 with the IME visible, verify the email Done action submits
   once and the sticky action remains reachable.
5. Delete from detail, choose Undo, and verify the Contact returns.

## Validation results

| Check | Result |
|---|---|
| Production and Android-test Kotlin compilation | Passed |
| `:app:testDebugUnitTest --rerun-tasks` | Passed, 188 tests, 0 failures |
| `:app:lintDebug` | Passed, 0 errors and 107 warnings |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Focused Contact + modal instrumentation | Passed, `OK (10 tests)` |
| Complete instrumentation suite | Passed, `OK (45 tests)` on API 37 / Android 17 |
| `tools/codex/session-init.ps1` | Passed after the intentional policy manifest refresh |

## Result summary

The Contact portion of Phase 2 item 2 is complete. Ordinary detail now provides
the expected management and guided-flow actions, while add/copy/update editors
use the adaptive scaffold with deterministic save, validation, IME, and
dirty-dismiss behavior.

The next bounded plan step is the Request detail and Add Request migration.
Field update/recovery remains after that within Phase 2 item 2. API 26 execution
remains unavailable because that system image is not installed.
