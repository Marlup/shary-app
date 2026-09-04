# UI/UX Slice 0A progress and agent navigation

## Scope

Resume the saved application-wide UI/UX audit from its authoritative handoff,
implement the code changes in Slice 0A, and add a compact agent-oriented
navigation layer that reduces broad source scanning.

The worktree was already heavily dirty. Changes were limited to clean files or
narrow hunks whose existing surrounding work was preserved.

## Change list

1. Replaced API-35-only matching history removals with API-26-safe indexed
   removals.
2. Added direct JVM coverage for matching commit, undo, redo, redo invalidation,
   clear, and empty-history behavior.
3. Corrected the behavior-neutral `FieldViewModel` indentation lint failure.
4. Moved the emulator-host cleartext exception from the main source set to a
   debug-only manifest/resource overlay and kept release cleartext explicitly
   disabled.
5. Added `app/AGENTS.md`, a focused Compose `ui/AGENTS.md`, and
   `docs/ai/CODEBASE_NAVIGATION.md` with task-to-code, test, document, and
   PowerShell search shortcuts.
6. Updated root and `docs/ai` retrieval surfaces and refreshed the session-init
   manifest.
7. Added non-throwing, typed task-value validation while preserving the existing
   throwing `encode` API for compatible callers.
8. Blocked invalid blank-task saves and type changes with exact inline row
   errors, then covered repair and normalized persistence in JVM and Compose
   tests.
9. Split `CompactTagSelector` into a dependency-free injected renderer and a
   Hilt-backed fallback path; tag creation is offered only when a persistence
   callback exists.
10. Added current-device matching coverage and stabilized the task-editor popup
    interaction against scroll and separate-window attachment timing.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 0 items 1 and 5, plus the task-validation part of item 6.

Continuation handoff:
[[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0A — restore static and device gates]]
-> items 1-5.

## Viability and complexity per change

- Matching history compatibility: small and low regression risk. The production
  file was clean and the controller supports direct host-side tests.
- Indentation correction: small and behavior-neutral. Only the malformed block
  indentation was changed inside a user-modified ViewModel.
- Cleartext source-set boundary: small-to-medium and security-sensitive. The
  main manifest was already modified, so only the three owning attributes were
  changed; unrelated notification and MCP entries were preserved.
- Typed task validation: medium and behavior-sensitive. A sealed result keeps UI
  actions non-throwing while the existing `encode` contract remains available
  to callers that expect an exception for invalid persisted data.
- Dependency-free tag rendering: small-to-medium. The public selector API stays
  compatible, but an explicitly injected tag list can no longer resolve Hilt.
- Agent navigation: medium documentation work. A small hierarchy is more
  maintainable than adding one policy file per package.

## Design decisions

- `app/AGENTS.md` owns module-wide layer and source-set guidance.
- `ui/AGENTS.md` exists because Compose route/renderer boundaries, feature
  ownership, accessibility, and device-test routing need more specific rules.
- Existing application-layer READMEs remain the bounded retrieval surface for
  application submodules; their content was not duplicated into new policy
  files.
- `CODEBASE_NAVIGATION.md` is curated because file names cannot infer security
  or state ownership. Any future generated inventory should be disposable under
  `build/agent-index/`, while a checker may report broken paths without
  rewriting authored descriptions.
- Current work is discovered through both backlog indexes. Source TODO searches
  are supplementary, not the durable plan source.
- Task validation belongs to the value contract and returns exact invalid row
  indices; the editor only translates that result into inline UI state.
- `CompactTagSelector` resolves a ViewModel only for its fallback route. Its
  injected renderer receives tags and an optional creation callback directly.

## Implementation plan

1. Recover and revalidate the July 17 plan and July 18 handoff. Completed.
2. Apply and test the isolated matching compatibility fix. Completed.
3. Remove the remaining static Slice 0A lint errors without broad refactoring.
   Completed.
4. Add the minimal scoped retrieval hierarchy and central navigation map.
   Completed.
5. Implement task-editor validation and dependency-free tag rendering as
   separate patches. Completed.
6. Run the current-API and full instrumentation gates. Completed on API 37.
7. Run the minimum-API instrumentation gate. Pending because no API 26 system
   image is installed locally.

## Validation plan and results

| Check | Result |
|---|---|
| Focused `MatchingHistoryControllerTest` | Passed |
| Focused `FieldValueContractTest` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| `:app:lintDebug` | Passed with zero errors; existing warnings remain |
| `:app:assembleDebug` | Passed |
| `:app:processReleaseMainManifest` | Passed |
| `tools/codex/session-init.ps1` after manifest refresh | Passed |
| Merged debug manifest | Verified `usesCleartextTraffic="true"` with `networkSecurityConfig` |
| Merged release manifest | Verified `usesCleartextTraffic="false"` and no debug network config |
| Sampled navigation targets | All existed |
| Focused API 37 instrumentation | Passed 7/7 |
| Stabilized `FieldTaskEditorTest` on API 37 | Passed three consecutive focused runs |
| Complete API 37 instrumentation | Passed 27/27 |
| API 26 instrumentation | Not run; no API 26 system image is installed locally |

The first lint run after the matching patch reported only the two previously
known non-matching errors. The final lint run passed, proving the two API-level
errors, indentation error, and network-security error are resolved without a
lint baseline.

## Result summary

Slice 0A is code-complete and all static, JVM, and current-API gates are green.
Release no longer inherits the emulator cleartext exception. Matching history
now supports the minimum API without calling an API-35 method, task edits fail
inline instead of throwing, and injected tag rendering no longer requires Hilt.

Agents now have a short retrieval path from root policy to a central feature
map and the relevant scoped guide. The Slice 0A exit gate remains open only for
device execution on API 26; the installed current-API emulator passed the full
27-test instrumentation suite.
