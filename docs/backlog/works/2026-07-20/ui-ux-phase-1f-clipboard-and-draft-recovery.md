# UI/UX Phase 1F Clipboard and draft recovery

## Scope

Complete only the two requested Phase 1 residuals: make clipboard cleanup
lifecycle-independent, and persist outgoing Request/received-response draft
identity so workflow and delivery receipts can recover after process death and
re-authentication.

## Change list

1. Replaced Field and Contact composition-owned clipboard jobs with one
   application-lifetime cleanup coordinator.
2. Marked sensitive Field clips with Android's sensitive metadata and tagged
   every Shary clip with an ownership token.
3. Made expiry, process background, and authentication teardown clear only the
   still-current Shary-owned clip, preserving any external replacement.
4. Extended Request workflow state with normalized outgoing recipient identity.
5. Added backward-compatible protobuf persistence for owner-scoped outgoing and
   response workflow snapshots.
6. Persisted stable request IDs, requested key/alias identity, recipient
   addresses, requester identity, and matched Field keys without persisting
   Field values; requested and matched keys use the existing vault codec.
7. Restored only the authenticated owner's snapshot and validated response
   identity against immutable received Request history.
8. Added the outgoing Request ID to the review route so persisted delivery
   receipts and the restored draft use the same transaction identity.
9. Made outgoing and response review preparation await the ordered DataStore
   snapshot write before navigation.

## Plan source

Plan source:
[[docs/backlog/works/2026-07-17/application-ui-ux-deep-sweep-and-rework-plan.md#Recommended implementation plan]]
-> Phase 1 items 2 and 5, plus the Phase 1 process-recreation exit gate.

Continuation source:
[[docs/backlog/works/2026-07-20/ui-ux-phase-1e-request-delivery-history.md#Result summary]].

## Viability and complexity per change

- Clipboard ownership: low-to-medium complexity. Android clipboard access stays
  in infrastructure; a pure scheduler/gateway boundary makes timer and
  replacement behavior directly testable on the JVM.
- Draft persistence: medium complexity. A new optional protobuf collection is
  backward-compatible, but restoration must be owner-scoped and must not place
  Field values in workflow state.
- Authentication boundary: process death deliberately loses in-memory vault
  keys, so restoration occurs after normal re-authentication initializes the
  codec. This is safer than trying to persist cryptographic session material.

## Design decisions

- Use an opaque clip token so a delayed expiry never clears content copied by
  another app after Shary.
- Clear the current Shary clip on background even when auto-clear is disabled;
  the setting controls the foreground timer, not session privacy boundaries.
- Store one workflow snapshot per normalized owner rather than one device-global
  draft.
- Persist only containment identity: Request IDs, key/alias metadata, recipient
  addresses, requester identity, and matched keys. Field values remain in the
  encrypted Field repository.
- Restore responses only when Request ID, requester hash, and requester address
  still match received history.
- Await the final workflow snapshot before entering review so the saved route
  can never reference a newer identity than durable storage.

## Implementation plan

1. Move clipboard ownership and wire Field/Contact/session callers. Completed.
2. Add clipboard timer, replacement, background, and teardown tests. Completed.
3. Extend the Request reducer and protobuf repository snapshot. Completed.
4. Wire owner-triggered restoration and stable review routing. Completed.
5. Add reducer/repository restoration tests. Completed.
6. Synchronize documentation and run full validation. Completed.

## Validation plan

- Focused clipboard/session teardown tests.
- Focused Request reducer/repository/summary tests.
- Complete JVM unit suite with forced recompilation.
- Debug Kotlin and Android-test source compilation.
- Lint, both debug APK assemblies, `git diff --check`, and the available device
  instrumentation suite.

## Manual verification procedure

1. Set Clipboard auto-clear to a short non-zero value, copy a sensitive Field,
   navigate to another primary section immediately, wait for expiry, and verify
   paste yields no Shary value.
2. Repeat the copy, replace it with content copied outside Shary before expiry,
   and verify the external content remains.
3. Create an outgoing Request with recipients and enter review; press Home, run
   `adb -s emulator-5554 shell am kill com.shary.app`, relaunch, authenticate as
   the same owner, and verify the same requested keys, recipients, Request ID,
   and delivery receipt recover.
4. Repeat with a matched received response and verify a different owner cannot
   restore that response draft.

## Validation results

| Check | Result |
|---|---|
| Focused clipboard and authentication teardown tests | Passed |
| Focused Request reducer, cold-repository restoration, and summary tests | Passed |
| `:app:testDebugUnitTest --rerun-tasks` | Passed, 185 tests, 0 failures |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:lintDebug` | Passed, 0 errors and 108 warnings |
| `:app:assembleDebug` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| `tools/codex/session-init.ps1` | Passed, policy version `2026-07-18.1` |
| `git diff --check` | Passed |
| Complete device instrumentation | Passed, `OK (38 tests)` on API 37 / Android 17 emulator |

## Result summary

Both requested residuals are complete. Clipboard expiry now survives navigation
and clears at process/session privacy boundaries without erasing a newer
external clip. Outgoing and response review identity is written before review
navigation, survives a cold repository recreation, restores only after matching
owner re-authentication, and reconnects to the same persisted delivery receipt.

All 185 JVM tests, Android-test compilation, lint, both APK builds, and the
complete 38-test API 37 device suite pass. An API 26 system image is not
installed, so this session could not repeat the device suite on the minimum API.
