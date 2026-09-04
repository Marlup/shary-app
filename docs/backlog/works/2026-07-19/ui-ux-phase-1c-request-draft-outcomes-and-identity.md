# UI/UX Phase 1C Request draft outcomes and identity

## Scope

Continue Phase 1 after Contact editor outcomes by correcting false success in
the outgoing Request draft editor and replacing filtered-list index identity
with persisted Request IDs for list keys and open details.

The authoritative flow trace showed that Add Request does not write through
`RequestRepository`: it synchronously edits the process-local outgoing draft.
This slice therefore adds truthful draft-mutation outcomes rather than an
artificial asynchronous persistence state.

## Change list

1. Added a pure Request draft editor transition with `ADDED`, `DUPLICATE`, and
   `INVALID` outcomes.
2. Normalized requested keys and aliases before draft insertion and preserved
   the existing draft unchanged on duplicate or invalid input.
3. Kept Add Request open with its input and an inline explanation when a key is
   duplicate or invalid; successful additions dismiss only after the draft
   callback confirms the mutation.
4. Replaced open-detail indexes with `rememberSaveable` Request IDs and keyed
   Request list items by mode plus stable ID.
5. Added pure editor-policy, ID lookup/reorder, and Compose editor regression
   coverage.
6. Synchronized the Request application README, agent navigation map,
   technical overview, and element/action map.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 items 1 and 3.

Specific audit findings:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Detailed usability and interaction findings]]
-> `Add Request reports false success and maps alias incorrectly`, plus the
Request filtered-list index finding under Navigation and state restoration.

Continuation source:
[[docs/backlog/works/2026-07-19/ui-ux-phase-1b-contact-editor-outcomes.md#Result summary]].

## Viability and complexity per change

- Typed draft add outcomes: low complexity. The outgoing draft is process-local
  and synchronously owned by `CacheService`, so a pure transition plus an
  immediate callback result is sufficient.
- ID-based detail state: low complexity because Request IDs were already made
  persistent in Slice 0B. No protobuf or backend contract change is required.
- Repository save-state UI: rejected for this editor because no repository
  operation occurs when a requested key is added.

## Design decisions

- The application transition owns normalization and duplicate detection. UI
  copy maps the typed outcome to an inline, correctable message.
- Duplicate detection is trimmed and case-insensitive, matching the existing
  outgoing reducer identity rule.
- A duplicate or blank candidate does not mutate requested fields or rotate the
  outgoing transaction ID.
- Detail state stores only a stable Request ID, never an index or complete
  domain object. Search and mode changes explicitly close an open detail.
- Request list lazy keys include mode and stable ID to avoid received/sent
  namespace collisions.

## Implementation plan

1. Trace Add Request to its real state owner and classify the boundary. Completed.
2. Add the typed draft transition and ViewModel result. Completed.
3. Wire truthful editor behavior and ID-based list/detail state. Completed.
4. Add focused JVM and Compose tests and update active docs. Completed.
5. Run focused, full static, package, and device gates. Completed.

## Validation plan and results

| Check | Result |
|---|---|
| `RequestDraftEditorTest` | Passed, 3 tests |
| `RequestFilteringTest` | Passed, 4 tests |
| `:app:compileDebugKotlin` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:lintDebug` | Passed with zero errors |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Add Request editor instrumentation | Passed, 3 tests on API 37 |
| Complete instrumentation suite | Passed, 37 tests on API 37 |
| `git diff --check` | Passed before final validation-record update |

API 26 execution remains unavailable because no API 26 emulator image is
installed in the current environment.

## Result summary

Phase 1 now has truthful synchronous Request draft-add outcomes and stable
Request list/detail identity. Duplicate or invalid input leaves both the draft
and editor intact, successful input preserves normalized key/alias metadata,
and list reorder can no longer retarget an open detail. The complete available
host and API 37 device gates pass.
