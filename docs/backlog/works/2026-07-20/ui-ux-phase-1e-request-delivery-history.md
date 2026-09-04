# UI/UX Phase 1E Request and response delivery history

## Scope

Continue Phase 1 after tag-operation outcomes with the stable-identity portion
of delivery persistence: store terminal cloud delivery metadata for outgoing
Requests and received responses, then restore the exact receipt/retry boundary
when a review route obtains the same stable Request ID.

This slice is bounded to outgoing Requests and responses because they already
have stable transaction/local-history identity. Arbitrary Field shares do not;
inventing an identity here would risk restoring a receipt onto the wrong review
payload.

## Change list

1. Extended the backward-compatible Request protobuf/domain model with delivery
   channel, per-recipient backend status, and delivery-update timestamp.
2. Added stable-ID Request history lookup and preserved atomic same-ID upsert so
   each retry replaces the prior outcome rather than duplicating history.
3. Persisted `Sent`, `Partial`, and `Failed` cloud Request/response receipts,
   including transport failures and empty acknowledgement maps.
4. Restored persisted receipts into `CloudDeliveryController` when Request or
   response review is recreated with the same stable ID, and recomputed the
   aggregate summary from recipient outcomes.
5. Preserved failed-only retry after restoration and rejected invalid or
   duplicate restoration attempts.
6. Made Request history cards label persisted partial and failed attempts
   truthfully instead of presenting every history record as `Sent`.
7. Added focused controller, history-mapping, protobuf-mapping, repository, and
   delivery-summary regression coverage.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 item 4.

Continuation source:
[[docs/backlog/works/2026-07-19/ui-ux-phase-1d-tag-operation-outcomes.md#Result summary]].

## Viability and complexity per change

- Request schema extension: medium complexity but low migration risk because
  protobuf additions use new field numbers and old records retain defaults.
- Stable history upsert/lookup: low complexity because the repository already
  owns stable request IDs and serialized DataStore mutation.
- Receipt restoration: medium complexity because stored aggregate labels must
  not be trusted; summary and retry eligibility are recomputed from outcomes.
- Generic Field-share history: deferred. That review flow lacks a stable local
  history identity, so persistence would require a separate design.

## Design decisions

- Persist terminal failures as well as successes; otherwise process recreation
  would erase the only retryable evidence.
- Keep raw backend recipient statuses so authorization, unavailable-recipient,
  invalid-request, and retryable-error distinctions survive storage.
- Preserve legacy Request history: records without delivery metadata continue
  to render as the prior successful `Sent` history.
- Keep retry transport in the existing controller; persistence restores state
  but cannot start network work by itself.

## Implementation plan

1. Extend Request domain/protobuf/mappers. Completed.
2. Add stable history lookup and focused persistence tests. Completed.
3. Add receipt persistence/restoration mapping and controller tests. Completed.
4. Wire CloudViewModel and Request review recreation. Completed.
5. Wire received-response persistence and restoration. Completed.
6. Surface partial/failed status on Request history cards. Completed.
7. Run focused and full validation gates. Completed on the host and the
   available API 37 emulator.

## Validation plan

- Focused cloud controller/history, Request mapper/repository, and summary tests.
- Complete JVM unit suite.
- Debug Kotlin and Android-test source compilation.
- Lint, debug APK, Android-test APK, session-init, and `git diff --check`.
- Targeted/full device instrumentation when an emulator is available.

## Validation results

| Check | Result |
|---|---|
| Focused cloud controller/history and Request repository tests | Passed |
| `:app:testDebugUnitTest --rerun-tasks` | Passed, 178 tests, 0 failures |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:lintDebug` | Passed, 0 errors and 108 warnings |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| `tools/codex/session-init.ps1` | Passed, policy version `2026-07-18.1` |
| `git diff --check` | Passed |
| Complete device instrumentation | Passed, `OK (38 tests)` on API 37 / Android 17 emulator |

The first unforced complete unit run reused stale test bytecode after the
`RequestDomain` constructor extension and produced 13 `NoSuchMethodError`
failures. Re-running with `--rerun-tasks` rebuilt every production/test class;
all 178 tests then passed. This was an incremental cache/ABI artifact, not a
behavioral assertion failure.

## Result summary

Phase 1E is complete for Request-backed cloud delivery. Sent, partial, failed,
and transport-error outcomes are stored per recipient for outgoing Requests and
responses, restored by stable Request ID, and remain eligible only for
failed-recipient retry. Request history no longer labels partial/all-failed
records as Sent.

End-to-end process-death recovery still depends on the separate
outgoing/response draft identity being available after restart; this slice
guarantees durable receipt data and correct restoration once that stable ID is
present. Identity-less ad-hoc Field shares remain route-local. The complete
available device suite passes on API 37; API 26 execution remains unavailable
because that emulator image is not installed.
