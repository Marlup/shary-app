# UI/UX Phase 1B Contact editor identity and outcomes

## Scope

Continue Phase 1 after the Field editor outcome slice by correcting Contact
email-edit identity and applying truthful persistence state to Contact add,
copy, and update sheets.

The worktree was already heavily dirty. The implementation remains bounded to
the Contact repository contract, persistence implementation, ViewModel/editor
wiring, focused tests, and matching active documentation.

## Change list

1. Added an explicit `updateUser(originalEmail, updatedUser)` repository
   contract with `Updated`, `Unchanged`, `NotFound`, and
   `EmailAlreadyExists` outcomes.
2. Contact email edits now atomically replace the original record instead of
   upserting by the new email and accidentally creating another Contact.
3. Email collisions and missing originals leave persistent data unchanged and
   return typed outcomes to the ViewModel.
4. Added typed `Idle`, `Saving`, `Saved`, and `Failed` editor state for Contact
   Add and Update operations, including synchronous duplicate-submit claims.
5. Contact add/copy/update sheets now wait for persistence success, retain the
   draft and inline error on failure, block dismissal/re-editing while saving,
   and use the email keyboard for the email field.
6. Preserved connector upsert behavior as a separate contract; only the
   interactive Contact editor uses original-identity update semantics.
7. Added repository, operation-controller, and Compose regression coverage and
   synchronized the active navigation, technical-overview, and action-map docs.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 items 1 and 3.

Continuation source:
[[docs/backlog/works/2026-07-19/ui-ux-phase-1a-field-editor-outcomes.md#Result summary]].

## Viability and complexity per change

- Original-email migration contract: medium complexity and lower risk than a
  protobuf-wide stable-ID migration. The repository can atomically detect the
  original row and target-email collision in one DataStore update.
- Typed editor outcomes: low-to-medium complexity because the proven Field
  owner-controlled dismissal pattern can be applied without changing shared
  connector behavior.
- Permanent Contact IDs: intentionally deferred. Adding IDs requires protobuf
  migration, existing-data backfill, selection/cache changes, connector
  contract review, and reference migration across request/send flows.

## Design decisions

- The editor captures the original Contact email and passes it separately from
  the updated draft. The repository never infers original identity from the
  changed email.
- Email comparison remains trimmed and case-insensitive, matching existing
  repository behavior.
- Alias-only, email-only, and combined edits replace exactly one original row.
  A collision or missing original performs no write.
- `Updated` and `Unchanged` are successful editor outcomes. Expected collision
  and not-found cases remain visible in the editor rather than becoming false
  success or duplicate records.
- Connector-owned upsert retains its established create-or-replace semantics;
  this slice does not change external connector contracts.

## Implementation plan

1. Add and test the atomic original-email update contract. Completed.
2. Add and test Contact editor operation state and duplicate claims. Completed.
3. Wire add/copy/update sheets to owner-controlled terminal state. Completed.
4. Add Compose behavior coverage and update active docs. Completed.
5. Run focused, full static, package, and device gates. Completed.

## Validation plan and results

| Check | Result |
|---|---|
| `UserRepositoryImplTest` | Passed, 4 tests |
| `ContactEditorOperationControllerTest` | Passed, 4 tests |
| `:app:compileDebugKotlin` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:lintDebug` | Passed with zero errors |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Contact editor instrumentation | Passed, 3 tests on API 37 |
| Complete instrumentation suite | Passed, 34 tests on API 37 |
| `git diff --check` | Passed after final validation-record update |

Manual verification remains useful for a real DataStore write failure: confirm
the Contact draft remains editable with the failure visible, then succeeds on
retry without creating a duplicate.

## Result summary

Phase 1 now has identity-safe Contact email updates and truthful Contact editor
outcomes. Email rename no longer creates a second record, collisions fail
closed, and editor sheets wait for repository acknowledgement.

This does not complete permanent ID-based Contact selection/detail. That wider
protobuf/cache/reference migration remains separate, as do equivalent Request
and tag editor outcome slices.
