# UI/UX Phase 1D tag operation outcomes

## Scope

Continue Phase 1 after Request draft outcomes by aligning custom-tag dialog
validation with persistence and making create/rename terminal results explicit.

The worktree was already heavily dirty. This slice remains bounded to the tag
name contract, repository/ViewModel operation boundary, tag dialogs/selectors,
focused tests, and matching active documentation.

## Change list

1. Added one shared tag-name contract for normalization, 32-character length,
   blank input, and supported-character validation.
2. Replaced create upsert/no-op ambiguity with explicit `Created`,
   `AlreadyExists`, and `Invalid` repository results.
3. Added atomic `updateTag(originalName, updatedName, color)` outcomes for
   `Updated`, `Unchanged`, collision, missing original, and invalid input.
4. Corrected duplicate create so it no longer overwrites the existing color,
   and corrected rename so it replaces the original rather than creating a
   second tag.
5. Added typed `Idle`, `Saving`, `Saved`, and `Failed` tag editor state with a
   synchronous duplicate-submit claim.
6. Tag create/update dialogs now retain input and inline errors on failure,
   disable name/color/dismiss actions while saving, and close/select only the
   tag returned by successful persistence.
7. Removed the compact selector's optimistic selection of a tag before its
   create operation succeeds.
8. Added shared validation, repository, operation-controller, and Compose
   regression coverage and synchronized active docs.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 item 3.

Specific audit finding:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Detail and editor surfaces]]
-> tag dialogs validate less than the repository and compact creation selects
before persistence succeeds.

Continuation source:
[[docs/backlog/works/2026-07-19/ui-ux-phase-1c-request-draft-outcomes-and-identity.md#Result summary]].

## Viability and complexity per change

- Shared validation: low complexity. The previous repository rules can move
  into one transport-neutral value contract without changing persisted data.
- Atomic rename: medium complexity. The Preferences DataStore edit must locate
  the original and detect a target collision in one serialized mutation.
- Typed editor state: low-to-medium complexity because the proven Field and
  Contact owner-controlled dismissal pattern applies directly.
- Updating fields that already use a renamed tag: intentionally out of scope.
  Existing UI prevents update/delete while a tag is in use, so no field
  reference migration is required in this slice.

## Design decisions

- Overlong tag names are rejected rather than silently truncated.
- Tag identity remains trimmed, collapsed-whitespace, and case-insensitive for
  duplicate/collision checks.
- Duplicate create is a failure and never recolors the existing tag. Recoloring
  is an explicit update of the original identity.
- `Updated` and `Unchanged` are successful editor outcomes; collision,
  not-found, invalid, and thrown persistence failures retain the editor.
- Selection changes only from the persisted `Tag` carried by `Saved`, never
  from a speculative `Tag.fromString` before the write completes.

## Implementation plan

1. Add the shared validation and typed repository contracts. Completed.
2. Implement/test atomic create and original-name update outcomes. Completed.
3. Add/test tag editor operation state and duplicate claims. Completed.
4. Wire full and compact selectors to terminal results. Completed.
5. Run focused, full static, package, and device gates. Completed.

## Validation plan and results

| Check | Result |
|---|---|
| `TagNameContractTest` | Passed, 4 tests |
| `TagRepositoryImplTest` | Passed, 5 tests |
| `TagEditorOperationControllerTest` | Passed, 4 tests |
| `:app:compileDebugKotlin` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:lintDebug` | Passed with zero errors |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed after final test-selector correction |
| Compact tag selector instrumentation | Passed, 3 tests on API 37 |
| Complete instrumentation suite | Passed, 38 tests on API 37 |
| `git diff --check` | Passed before final validation-record update |

The first targeted instrumentation run exposed an ambiguous test query because
the new tag name appears in both the input and preview. The query was narrowed
to the editable field semantics; no production behavior changed, and both the
targeted and complete device suites then passed.

API 26 execution remains unavailable because no API 26 emulator image is
installed in the current environment.

## Result summary

Phase 1 now has truthful custom-tag create and rename outcomes. Validation is
shared with persistence, duplicates/collisions do not mutate stored tags,
renames replace the original atomically, failed drafts remain visible, and the
compact selector chooses only the tag returned by successful persistence. The
complete available host and API 37 device gates pass.
