# UI/UX Slice 0B session and request-state isolation

## Scope

Skip further Slice 0A work and implement the next containment slice from the
application-wide UI/UX audit: atomically clear process-local account state,
separate incoming-response and outgoing-request workflows, persist stable
request identity, and bind every response to the authenticated requester of the
selected request.

The worktree was already heavily dirty. Changes were made as narrow patches and
unrelated user-owned work was preserved.

## Change list

1. Replaced independent selection-cache authorities with one immutable
   `AccountScopedCacheState` and one atomic full-session reset.
2. Added runtime teardown for auth, cloud, cryptographic identity, selections,
   request workflow, and progressively decrypted session data across logout,
   timeout, password-change relogin, failed authentication, and account switch.
3. Added stable request IDs to the domain and proto store, carried a generated
   draft-transaction UUID inside the encrypted request payload, reused it for
   retries, and retained a deterministic fallback for legacy records and
   payloads.
4. Added separate immutable `activeReceivedRequest`, `responseDraft`, and
   `outgoingRequestDraft` state with reducer-owned transitions. Outgoing drafts
   retain only requested key and alias metadata, never secret values.
5. Verified an imported request's embedded requester address against the
   authenticated cloud-envelope sender and locked response review to that exact
   request ID and requester.
6. Routed response review with an explicit request ID, resolved matched values
   from the current session vault without overwriting ordinary field/contact
   selections, and marked Responded only after a successful cloud
   acknowledgement and repository update.
7. Made repeated received-request imports immutable no-ops by stable ID so a
   retry cannot replace identity, fields, timestamp, or Responded state.
8. Added direct reducer, identity, mapper, repository, cache, import-policy,
   review-state, CTA, auth-transition, cloud-reset, and cryptographic teardown
   tests.
9. Zeroed cached private signing and key-exchange seeds before dropping the
   process-session identity reference.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 0 items 2-3.

Continuation handoff:
[[docs/backlog/works/2026-07-18/application-ui-ux-audit-handoff.md#Slice 0B — isolate session and request-response state]]
-> items 1-4.

## Viability and complexity per change

- Atomic process-local reset: medium complexity and high security value. The
  cache already had one singleton implementation, so its full inventory could
  become one observable state without changing persistence ownership.
- Runtime session teardown: medium-to-high complexity because auth, cloud,
  decrypted session data, cryptographic identity, and UI selections had
  separate owners. Transitions are now serialized and authentication UI
  submissions are guarded against overlap.
- Request identity and sender binding: medium-to-high complexity and
  security-sensitive. New payloads carry UUID identity inside existing
  encryption; legacy material remains readable through deterministic fallback.
- Received/outgoing separation: medium complexity. A pure reducer made mode
  transitions and failure behavior testable before Compose wiring changed.
- Durable account isolation: not safely solvable as a narrow patch. Credentials
  and the field/request/contact stores are still device-global and need an
  explicit single-vault owner/reset policy or an owner-scoped storage migration.

## Design decisions

- `AccountScopedCacheState` is the complete process-local account inventory;
  teardown replaces it in one assignment so observers cannot see a partially
  cleared account.
- The incoming request, response matches, and outgoing request draft are
  separate states. Mode switches never copy between them.
- The authenticated cloud sender hash is authoritative. Legacy imports without
  that proof may remain visible but cannot start a response.
- Response routing fails closed when the route ID, active request, response
  draft, requester identity, or matched keys do not agree.
- Stable request IDs are immutable. Re-importing the same ID cannot reopen or
  rewrite the original received request.
- An outgoing transaction keeps one UUID across retries. Editing or clearing
  its requested content intentionally starts or ends that identity.
- Request workflow drafts are intentionally process-local. They survive
  configuration recreation while the application process remains alive, but
  process death discards incoming response and outgoing request drafts rather
  than persisting sensitive cross-screen workflow state.
- Email-composer launch is not a delivery acknowledgement, so it does not mark
  a received request Responded. Truthful multi-channel terminal receipts remain
  in Slice 0C.

## Implementation plan

1. Revalidate Slice 0B boundaries and leave Slice 0A unchanged. Completed.
2. Add the atomic cache inventory and reducer-owned separated workflows with
   direct unit tests. Completed.
3. Add stable request identity, authenticated requester binding, and explicit
   response-route wiring. Completed.
4. Centralize runtime teardown and cover failure paths. Completed.
5. Run focused and full local validation, then record the remaining durable
   storage boundary. Completed.

## Validation plan and results

| Check | Result |
|---|---|
| Focused Slice 0B JVM suite | Passed, 44 tests across 12 classes |
| Received-request immutable retry test | Passed |
| Production Kotlin compilation through the focused suite | Passed |
| Full `:app:testDebugUnitTest` | Passed, 108 tests; zero failures/errors/skips |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:lintDebug` | Passed with zero errors; 108 existing warnings remain |
| `:app:assembleDebug` | Passed |

No API 26 or Slice 0A device work was attempted, as explicitly requested.
The Compose callback handoff is not meaningful as an isolated JVM test;
`RequestWorkflowReducerTest`, `SummaryFieldReviewStateTest`, request repository
tests, and Android-test source compilation cover its nearest deterministic
boundaries. A future device smoke check should select one pending received
request, match fields, verify the exact requester on review, send successfully,
and confirm a second tap is disabled; it should also send the same outgoing
draft twice and confirm one stable local request identity.

## Result summary

Slice 0B's process-local and request-response containment is implemented and
the focused tests are green. A response can only resolve the exact selected
request, authenticated requester, and matched vault keys; outgoing request
drafts no longer reuse incoming requested fields; and runtime auth transitions
clear all in-memory account state.

The full account-A/account-B persistent-data exit gate is not complete. The
device-global credentials and field/request/contact stores remain a deliberate
residual pending an explicit vault-ownership policy or a larger owner-scoped
persistence migration. Slice 0C review, privacy-copy, terminal-result, and
email-cleanup work is also deferred.
