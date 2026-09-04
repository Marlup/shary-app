# Application UI/UX deep sweep and rework plan

## Scope

This is a repository-grounded audit of the current Android application, with
particular attention to Fields, Contacts, Requests, their detail/editor flows,
matching, review/send flows, top-level navigation, accessibility, performance,
and stale implementation surfaces.

The audit deliberately separates correctness and trust defects from visual
polish. No application code was changed in this pass. The target worktree was
already heavily modified, so every finding below describes the inspected state
without attempting to normalize or revert unrelated work.

## Change list

1. Reconciled the current implementation against recent Fields and shared-control work.
2. Inspected the Fields, Contacts, Requests, matching, summary, authentication,
   theme, cloud-state, navigation, and shared-component paths.
3. Searched active source and durable planning documents for TODOs and obsolete surfaces.
4. Ran build, unit, Android-test compile, lint, emulator, and instrumentation checks.
5. Produced a risk-ranked rework sequence with testable acceptance criteria.

## Plan source and existing TODO status

This audit extends, rather than blindly replaces, three useful prior plans:

- [[docs/design/PROPOSAL_5_SOFT_SPATIAL_MERGED_DESIGN_PLAN.md]] already calls
  for modal-sheet consolidation, matching improvements, Requests parity,
  accessibility, and legacy cleanup. Its baseline is stale: it still describes
  bottom navigation and transitions as absent even though both exist now.
- [[docs/design/UI_UX_FRONTEND_AGENT_BRIEF.md]] schedules Contacts, Requests,
  matching, and summary work after Fields and correctly describes the desired
  visible request-draft hierarchy. Some of its Fields assumptions have already
  been superseded by the July 11–15 implementation.
- [[docs/backlog/works/2026-07-05/android-native-ui-testing-and-diagnosis-plan.md]]
  remains the active native-test TODO. Its broader component, screen, device,
  accessibility, deep-link, and system-flow coverage is not complete.

No actionable source-code `TODO` or `FIXME` marker was found for this redesign.
The `todo` occurrences in `FieldValueContract.kt` are domain task statuses, not
developer tasks. The real TODO is therefore distributed across the plans above.
Those plans do not cover several correctness, privacy, state-isolation, and
send-lifecycle defects found here; those defects must be inserted ahead of the
existing visual phases.

## Executive verdict

The recent Fields card and shared toolbar work is a stronger visual baseline and
should be preserved. The application is not ready for a broad aesthetic pass,
however. Several primary workflows can target the wrong recipient, retain state
across accounts, mix incoming-response data with outgoing request drafts, report
failed work as sent, or crash on supported Android versions.

Recommended product order:

1. Contain state, recipient, identity, send-result, and API-compatibility defects.
2. Replace index/global-cache-driven workflows with stable, explicit flow state.
3. Rework detail, editor, request-draft, matching, and receipt navigation.
4. Complete accessibility, adaptive-layout, theme, and lifecycle improvements.
5. Measure startup/list/tab performance before applying speculative optimizations.

## Evidence and limitations

- Source and tests were inspected throughout the current worktree.
- Authentication screens were visually exercised on a Pixel 9 Pro emulator at
  font scales 1.0, 1.3, and 2.0. The available tall handset did not expose an
  immediate clipping defect, but keyboard, landscape, small-width, and
  authenticated flows still need the matrix defined below.
- Authenticated cloud screens were source-inspected rather than exercised with
  live credentials. No credentials were added to the repository.
- No Macrobenchmark, Baseline Profile, JankStats, or Compose compiler metrics
  setup was found. Performance findings below are code-backed risks, not claims
  of measured jank.

## P0: stop-the-line correctness, privacy, and crash findings

### 1. Session-scoped request state survives logout

`SelectionCacheService.clearAllCaches()` clears Fields, Contacts, and phone
state, but leaves requests, outgoing request fields, active request, owner, and
owner identity state intact (`SelectionCacheService.kt:35-48,128-133`). Logout
uses this incomplete clear path (`AuthenticationServiceImpl.kt:153-157`).

Impact: account B can inherit account A's draft, request, owner, or recipient
context on the same device.

Rework: make session teardown one atomic operation that clears every
account-scoped value on logout, timeout, password recovery, login failure, and
account switch. Test the complete state inventory rather than individual lists.

### 2. A received-request response is not bound to its requester

After matching, `RequestScreen` only checks that some cached Contact exists
(`RequestScreen.kt:534-540`). `SummaryFieldScreen` reads the global cached
Contact list and sends to all of it (`SummaryFieldScreen.kt:86-88,289-293`),
without proving that it matches `RequestDomain.user`.

Impact: fields matched for Alice's request can be sent to stale selected Contact
Bob. This is both a privacy defect and a broken mental model.

Rework: introduce an immutable `ResponseDraft(requestId, requesterId,
requesterAddress, matchedFieldIds)`. The requester is locked by the incoming
request; the review screen shows that exact recipient and never reuses the
general outbound-recipient cache.

### 3. Incoming responses and outgoing request drafts share state

Opening an incoming request copies requested keys into the same `_draftFields`
used for outgoing requests (`RequestScreen.kt:472-483`). Switching to Sent
clears only the active request (`RequestViewModel.kt:46-49,78-83`). Leaving the
screen can then persist those incoming keys as an outgoing draft
(`RequestScreen.kt:245-251`).

Impact: mode switching silently changes what the user is composing and can turn
an incoming request into an outgoing draft.

Rework: model `Received`, `OutgoingDraft`, and `SentHistory` as separate states
with separate storage. Do not infer workflow from the currently selected tab.

### 4. Matching undo/redo can crash on Android 8–14

`MatchingHistoryController.kt:37,44` calls `List.removeLast()`. Lint resolves
that call to API 35 while the application supports API 26
(`app/build.gradle.kts:84-88`).

Impact: a visible matching action can crash on most supported Android versions.

Rework: use a compatibility-safe last-index removal, add controller tests, and
exercise undo/redo on API 26 and the current target API.

### 5. Review screens conceal recipients and make inaccurate security claims

Both review screens display only `firstOrNull()` while sending to the complete
recipient list (`SummaryFieldScreen.kt:86-88,184-190,289-293` and
`SummaryRequestScreen.kt:77-79,133-139,219-224`). The email implementation puts
all addresses in the `To` field (`EmailServiceImpl.kt:25-54`) and writes field
values to a plaintext JSON cache attachment (`JsonFileServiceImpl.kt:124-140`).
Cloud payloads are encrypted client-side, but ciphertext is retained until
accepted or expired. Therefore the shared “data never stored” message is not
true for either channel.

Impact: users cannot verify the real disclosure set; email recipients can see
one another; sensitive values exist as plaintext cache files; privacy copy
overstates both transports.

Rework:

- Show every recipient, total count, and channel before the final action.
- Send separate email intents or explicitly disclose group-address visibility.
- Define and test attachment cleanup on success, cancellation, timeout, and app restart.
- Use channel-specific copy. For cloud: “Encrypted before leaving this device;
  relay ciphertext expires after the stated retention period.” For email:
  disclose that the mail provider and recipient receive a readable attachment.
- Add a pre-send sensitive-field warning for email rather than claiming parity
  with encrypted cloud delivery.

### 6. The current Android UI regression gate crashes

The full instrumentation run reached 24 tests, then the test process crashed in
`FieldDetailDialogTest.updateFieldDialogCanEditAndAddListElements`.
`CompactTagSelector` resolves a Hilt ViewModel when `onCreateTag == null`, even
when injected `availableTags` are supplied (`TagPicker.kt:192-197`). The pure UI
test path reaches that branch through `FieldUiManagement.kt:781` in a plain
`ComponentActivity`.

Impact: the detail/editor regression slice is not hermetic and the full device
gate cannot complete.

Rework: make provided data and callbacks a complete dependency-free rendering
path. Keep Hilt resolution at route/container boundaries, never inside reusable
leaf UI.

### 7. A development cleartext exception is enabled in the main application

The main manifest globally sets `android:usesCleartextTraffic="true"` and installs
the main-source `network_security_config` (`AndroidManifest.xml:30-33`). That
config permits cleartext to the emulator host `10.0.2.2` and omits the explicit
`includeSubdomains` value that lint requires (`network_security_config.xml:3-4`).
The Firebase emulator hookup itself is guarded by the `dev` environment in
`MainActivity.kt:61-64`, but the transport permission is not build-variant scoped.

Impact: a release manifest can permit cleartext far beyond the intended local
Firebase emulator workflow. Simply changing `includeSubdomains` to `true` would
broaden the exception rather than fix the release boundary.

Rework: move emulator transport configuration and `usesCleartextTraffic` into a
debug/dev manifest and resource overlay. Keep release cleartext disabled. If the
host exception remains, set `includeSubdomains="false"` explicitly and validate
the merged manifests for both variants.

### 8. Blank task edits throw from a user-facing editor path

Existing task titles can be edited to blank (`FieldUiManagement.kt:478-482`).
Save and value-type changes call the throwing persistence encoder directly
(`FieldUiManagement.kt:892-910,1212-1226,1245-1250`), while
`TaskValueContract.encode()` rejects blank titles with an exception
(`FieldValueContract.kt:86-93`).

Impact: ordinary editing can deterministically terminate the app instead of
showing validation.

Rework: validate rows inline, return typed validation failures rather than throw
through UI code, focus/announce the invalid row, and disable Save/type changes
until the draft is valid.

## P1: broken or misleading primary workflows

| Finding | Evidence and impact | Recommended rework | Acceptance test |
|---|---|---|---|
| Normal Contact editing is unreachable | With no payload cached, `UserScreen.kt:203-245` exposes progression to Fields but no secondary edit/duplicate actions; `UserDetailDialog.kt:22-49` only closes. | Put Edit, copy email, create-similar, and Delete in the Contact detail surface. Keep workflow progression separate from entity management. | Open an unselected Contact from the ordinary list and complete every management action. |
| Editing a Contact email creates another Contact | Upsert uses the new email as identity (`UserRepositoryImpl.kt:51-75`), while the contract carries neither a stable ID nor original email (`UserRepository.kt:6-12`). | Add a stable Contact ID, or an explicit `update(originalEmail, updatedContact)` migration contract. | Alias-only, email-only, and collision updates preserve exactly one intended record. |
| Received requests do not reliably become Responded | The summary route gets a route-local `RequestViewModel` that does not own the active request (`AppNavigator.kt:126-139`; `SummaryFieldScreen.kt:82-84,144-150`). Email never marks the response. | Use request ID in a durable response draft. Mark status only after an acknowledged terminal event; opening an email composer is not delivery confirmation. | Cloud success, cloud partial/failure, email composer cancel, and retry produce truthful statuses. |
| All-error cloud results are treated as success/history | Any non-empty result map advances success or saves Sent history (`CloudViewModel.kt:80-85,107-129`), even though `CloudServiceImpl.kt:330-378` can return only errors. | Reduce per-recipient outcomes into `Sent`, `Partial`, or `Failed`. Persist channel and each recipient outcome. | An all-error result never shows Sent; partial results identify retryable recipients. |
| A completed review remains resendable | Summary success shows a snackbar but does not transition to a terminal receipt, clear the completed draft, or disable the action. | Implement `Review -> Sending -> Sent/Partial/Failed`, with a receipt and explicit Done/Retry-failed actions. | Rapid taps create one operation; returning from a receipt cannot silently resend. |
| Outgoing request drafting is invisible | Sent mode shows history while draft keys affect only the dock (`RequestScreen.kt:293-348,410-495`). | Add a visible draft card/board with key edit/remove, recipient readiness, and `Select Contacts` / `Review Request`. | A user can inspect and correct the complete draft without guessing from a count. |
| Add Request reports false success and maps alias incorrectly | Duplicate keys are ignored but always announced as added; Alias is assigned to `FieldDomain.value`, not `keyAlias` (`RequestViewModel.kt:93-99`; `RequestScreen.kt:512-524`). | Return a typed add result, keep the sheet open on duplicate/invalid input, and write the correct property. | Added, duplicate, invalid, and corrected inputs each produce accurate state and messaging. |
| Inbox accept/reject can both fire | `RequestInboxReviewDialog.kt:85-105` has no synchronous decision lock; its current test expects Reject then Accept callbacks both to run. | Lock before invoking either action, make processing idempotent by payload ID, and confirm destructive rejection. | Repeated/opposing taps produce exactly one backend decision. |
| Field save sheets acknowledge before persistence completes | Add, update, and recovery surfaces dismiss/show success around dispatch rather than a repository terminal result. | Expose `Idle/Saving/Saved/Error`; keep input and validation on failure; dismiss only on `Saved`. | A forced repository failure preserves the editor and shows an actionable error. |
| Field key appears editable but update does not persist it | The update surface allows key edits while the ViewModel update path retains the original identity. | Either make the key explicitly immutable with explanatory copy, or add a safe rename transaction with collision checks and reference migration. | UI behavior and persisted record agree after edit/restart. |
| Filters can leave invisible destructive selections active | Search/filter changes only the rendered list (`FieldScreen.kt:284-286,335-339,464-469`), while the dock still acts on every selected Field (`:546-587`). Zero matches also says “No fields yet.” | Distinguish empty vault from no results. Clear selection on a context change, or disclose “N selected, including hidden items” and reveal them before destructive work. | Select a Field, filter it out, then verify Delete cannot silently target an invisible item. |
| User values can collide with hard-coded lazy keys | `All` is both a legal tag and the filter sentinel; `build-vault-nudge` is both a legal Field key and a literal auxiliary item key (`FieldScreen.kt:318-327,699-702,730-732`). | Use typed filter identity and namespaced keys such as `filter:all`, `filter:tag:<id>`, `field:<id>`, and `aux:nudge`. | A tag named `All` and Field key `build-vault-nudge` render without duplicate-key failure. |
| Field selection can be overwritten from a stale lifecycle capture | The `DisposableEffect` is keyed only by lifecycle owner while reading `selectedFields` (`FieldScreen.kt:295-315`). | Use `rememberUpdatedState`, or make ViewModel selection the single observable source of truth. | Select, background/resume, and switch sections; cached selection remains the latest state. |
| Clipboard expiration can be cancelled by navigation | Field and Contact auto-clear jobs run in composition-owned scopes. Leaving the screen can cancel the promised clear, and sensitive values are not marked as sensitive clipboard content. | Move expiry to session/application ownership, mark sensitive clips, and clear on background/session close. | Copy a sensitive value, navigate immediately, advance the timer, and verify the clipboard is cleared. |
| Cloud readiness can be stale or false-positive | Mutable fields inside `CloudState` are changed without assigning a new `StateFlow` value (`CloudServiceImpl.kt:169-185,1015`); token presence is treated as online (`:123-131`). | Use immutable state/copy updates and separate auth, identity, reachability, and service readiness. Refresh on connectivity/resume and expose Retry. | Offline/online transitions update every cloud control while local work remains usable. |
| Theme selection is non-functional | `MainActivity.kt:68-76` passes theme state, but `AppOptionalTheme.kt:7-12` ignores it and `Theme.kt:8-36` always installs a light scheme. | Implement real semantic light/dark/product schemes, or remove the dormant preference and picker. | Every exposed theme choice visibly changes the app and survives restart, or no fake choice remains. |

## P2: UX, navigation, accessibility, and performance debt

### Navigation and state ownership

- Top-level tabs call `launchSingleTop` without `popUpTo`, `saveState`, or
  `restoreState` (`FieldScreen.kt:630-645`, `UserScreen.kt:293-308`,
  `RequestScreen.kt:382-397`, `ConnectorsScreen.kt:138-153`). Back can replay
  tab history and retain duplicate route-scoped ViewModels. Centralize a
  `navigateTopLevel()` contract and make Back clear selection/detail before
  leaving the section.
- Request selection and detail use filtered-list indexes in an unkeyed list
  (`RequestScreen.kt:450-499`). Insert, filter, or reorder can retarget an open
  request. Use stable request IDs for model identity, selection, details, and
  LazyColumn keys.
- Queries, filters, selected IDs, and open-detail context rely largely on
  `remember`, not `rememberSaveable` or `SavedStateHandle`. Preserve small,
  stable identifiers across rotation and process recreation; never save whole
  mutable domain objects.
- Auth navigation can stack duplicate Login/Signup destinations. Define an auth
  graph whose success transition clears the whole auth stack.

### Detail and editor surfaces

The current Contact and Request details are information-only `AlertDialog`
surfaces. Request detail omits recipients, channel, delivery results, request
mode, and status; Contact detail omits the management actions users expect after
opening an entity. Several editors use fixed screen-height fractions and do not
own IME, large-font, dirty-dismiss, or save-result behavior consistently.

Create one adaptive `SharyModalScaffold` with:

- a compact identity header and explicit Close/Back affordance;
- a scrollable or lazy body with system-bar and IME insets;
- a sticky action/footer region that can grow at large font scales;
- `Idle/Validating/Saving/Saved/Error` behavior;
- dirty-dismiss confirmation and state restoration;
- destructive actions visually separated and confirmed;
- full-screen phone presentation for complex editors/matching, and a constrained
  sheet/dialog presentation only when width and task complexity allow it.

Recommended entity designs:

**Field detail**

- Header: key, type, sensitivity, favorite state, and overflow.
- Content: readable value renderer, tags, task/list metadata, timestamps where
  real data exists, and copy feedback.
- Actions: Edit and Share/Select as the primary pair; Copy and Delete remain
  secondary/destructive. Never show speculative history sections without data.
- Update: key is either visibly locked or handled as a true rename transaction.

**Contact detail**

- Header: alias or address fallback, verified/local/cloud availability state.
- Content: full email and relationship metadata actually supported by the model.
- Actions: Edit, Copy email, Create similar, and Delete. A flow CTA such as
  `Select Fields` appears separately and only when relevant.
- Editor: stable identity, email keyboard, IME actions, validation/collision
  feedback, and save-result state.

**Request detail**

- Header: Received/Sent, status, relative and absolute timestamp.
- Content: requester/recipients, channel, requested keys capped in the card but
  complete in detail, match progress, expiry, and per-recipient delivery result.
- Actions vary by state: Match/Respond, Review draft, Retry failed, or Delete
  history. Sent cards must not show a no-op selection checkbox.
- A persistent outgoing-draft panel belongs in Requests; a count in a bottom dock
  is insufficient navigation.

### Matching redesign

The current two-column dialog is dense, index-driven, color-dependent, and not
adaptive. Undo/Redo look available when they are no-ops, and dismissal can lose
work without warning.

Use a task-oriented matching workspace:

1. On compact phones, show the requested key first and an explicit “Choose local
   field” step; advance one pair at a time with persistent progress.
2. On expanded widths, use a true two-pane layout with requested keys left and
   local fields right.
3. Show numbered/badged pair relationships, not color alone.
4. Give Undo/Redo real disabled semantics, preserve draft matching, and confirm
   abandoning changes.
5. Keep a sticky `N of M matched` summary and a single final `Review response`
   action. Do not choose recipients here; requester binding is already fixed.

### Accessibility and input

- `FieldCard` and `UserCard` implement click/hold through `pointerInput`
  (`FieldCard.kt:89-112`; `UserCard.kt:73-96`) and do not expose standard click
  actions, keyboard behavior, or ripple. Use combined click semantics and
  item-specific selection descriptions.
- The Received/Sent switch relies heavily on color and lacks selected/tab-group
  semantics. Use Material segmented-button behavior or an equivalent selectable
  group with announced state.
- Bottom-navigation icons duplicate their visible text through identical content
  descriptions (`SharySectionNavigationBar.kt:54-80`). Make labeled icons
  decorative and announce each destination once. Selection needs more than color.
- Fixed 46dp shared buttons and the fixed 60dp bottom bar can clip or compress at
  large font scale. Use at least 48dp touch targets and content-driven height.
- Login, Signup, Contact editor, and Add Request lack consistent email keyboard,
  IME action, autofill, scroll, and `imePadding` behavior. Test keyboard-visible
  compact/landscape layouts, not only a tall handset.
- The blocking loading indicator is visually tiny and lacks useful progress/live
  semantics. Prefer localized progress; when blocking is unavoidable, announce
  it and prevent ambiguous background interaction.
- Tag dialogs validate less than the repository, and the compact selector can
  select a tag immediately even when persistence later rejects it. Share one
  validator and make create/rename return explicit persistence results.
- UI text is nearly entirely hard-coded: no `stringResource()` use was found in
  the UI package and `strings.xml` contains only `app_name`. Move visible copy,
  plurals, and accessibility labels to resources before localization or robust
  long-string testing.

### Rendering and runtime performance

- Production has dozens of `collectAsState()` calls and no
  `collectAsStateWithLifecycle()`. Dense roots observe many independent flows
  (`FieldScreen.kt:155-168`; `RequestScreen.kt:104-113`). Use lifecycle-aware
  collection and aggregate route UI state where that improves atomicity.
- Request cards render every requested key. Show three or four previews plus
  `+N`; render the full set only in detail. Key request rows with stable IDs.
- `CloudServiceImpl` creates multiple `OkHttpClient` instances instead of
  sharing an injected client. Reuse one configured client and its pools.
- Mutable domain properties such as Contact and cloud-state fields make Compose
  stability and state notification harder to reason about. Prefer immutable UI
  models and copy-on-write updates.
- Add Macrobenchmark/Baseline Profile only after correctness state is stable.
  Measure cold start, 500/1,000-row search and scroll, tab switching, detail open,
  and matching. Report median/p95 startup and frame timing rather than claiming
  improvement from code shape alone.

## P3: stale, inconsistent, and dead surfaces

Candidate cleanup after call-site verification:

- `Screen.Home` and `HomeScreen` are not represented in the active navigator;
  `GoBackHomeLayout` targets the absent route.
- `StartDestinationResolver` has no live caller and returns the same destination
  in every branch while `AppNavigator` duplicates startup logic.
- No live callers were found for `ThemeMenuButton`, `RowSearcher`/`FilterBox`,
  `SelectableRow`, `ScreenScaffold`, the legacy Field/Contact item-row chain,
  `SpecialComponents`, `SharySectionActionRow`, `SendCommunicationDialog`,
  `SendFieldsGenericButton`, or `FieldViewModel.fetchFieldsFromCloud`.
- `SendRequestDialog.kt` is unused and still says “Send Fields.”
- `UserRowSelectable.kt.disabled` remains as a disabled source artifact.
- Terminology alternates among Users, Contacts, recipients, Alias, Duplicate,
  and Create similar. Choose Contact for the entity and recipient for its role.

Remove these in a dedicated cleanup patch with compile/test evidence; do not mix
deletion into the state-correctness fixes.

## Positive baseline to preserve

- The July Fields/card compaction and permanent-search work gives the primary
  lists a clearer density and hierarchy.
- Cloud-disabled actions remain visible and Fields can explain why they are disabled.
- Fields and Contacts already supply stable list keys.
- Shared icon buttons generally have explicit descriptions and minimum interaction sizing.
- Compose UI Testing and UI Automator are installed. The gap is coverage and
  dependency isolation, not another testing framework.
- Offline-first local work must remain available while cloud state changes.

## Target navigation model

Use one workspace graph for the four top-level sections and explicit nested flows:

```text
Auth graph
  Login <-> Sign up
  success -> Workspace (auth graph removed)

Workspace graph
  Fields      -> Field detail -> Field editor
  Contacts    -> Contact detail -> Contact editor
  Requests
    Received  -> Request detail -> Matching -> Response review -> Receipt
    Draft     -> Request editor -> Contact selection -> Request review -> Receipt
    Sent      -> Request detail -> Retry failed
  Connect

Settings remains a secondary destination, not a fifth workspace history stack.
```

Top-level switching restores each section state without adding replayable tab
history. Back priority is: dismiss transient UI, close detail/editor, clear
selection, leave nested flow, then exit the section/app.

## Recommended implementation plan

### Phase 0 — containment and red gates (S–M slices)

1. Fix matching API compatibility and all four lint errors. Scope the
   `10.0.2.2` cleartext exception to debug/dev with
   `includeSubdomains="false"`; do not broaden it to satisfy lint.
2. Clear every account-scoped cache on session teardown.
3. Bind responses to request ID/requester and split received/outgoing state.
4. Correct all-recipient disclosure and channel-specific security copy.
5. Make the Field detail test path dependency-free and restore a complete device gate.
6. Replace throwing task-editor validation and namespace user-controlled lazy keys.

Exit gate: lint passes; unit tests pass; instrumentation passes on API 26 and a
current API; cross-account and wrong-recipient regression tests pass.

### Phase 1 — stable flow state and truthful outcomes (M–L)

1. Add stable Contact/Request identity and ID-based list selection/detail.
2. Introduce typed request/response draft reducers and terminal send states.
3. Make repository/editor operations return explicit success/failure results.
4. Persist channel and per-recipient outcomes; implement receipt/retry behavior.
5. Make inbox decisions idempotent and clipboard cleanup lifecycle-independent.

Exit gate: process recreation, mode switching, rapid taps, partial failure, and
email cancellation produce deterministic and truthful state.

### Phase 2 — entity details, editors, and request hierarchy (L)

1. Build the adaptive modal scaffold.
2. Migrate Contact detail/editor, Request detail/Add Request, then Field update/recovery.
3. Add the visible outgoing request draft and state-dependent details/actions.
4. Rework matching into compact-step and expanded-two-pane presentations.

Exit gate: 320dp/412dp, landscape, split-screen, font scale 2.0, visible IME,
TalkBack, and keyboard/DPAD paths keep all primary actions reachable.

### Phase 3 — navigation, theme, accessibility, and resilience (M–L)

1. Replace top-level push navigation with state-restoring workspace navigation.
2. Add selection-first Back behavior and saveable transient state.
3. Implement or remove theme selection; migrate hard-coded colors to semantic tokens.
4. Resource visible strings and complete semantics/touch-target work.
5. Add loading/content/empty/error state to Contacts and Requests.

### Phase 4 — measured performance and cleanup (M–L)

1. Migrate Flow collection to lifecycle-aware route state.
2. Add stable request keys, capped card previews, and a shared HTTP client.
3. Establish Macrobenchmark and Baseline Profile measurements.
4. Remove verified dead surfaces and update stale design-plan status.

## Validation plan

### Fast gate per patch

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:lintDebug
```

### Device gate

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -r com.shary.app.test/androidx.test.runner.AndroidJUnitRunner
```

Required scenario coverage:

- account A populated -> logout/timeout -> account B has no inherited state;
- incoming request requester remains the only response recipient;
- Received/Sent/Draft switching never shares fields;
- Contact email rename and collision behavior;
- add/update/recovery repository failure keeps input and gives accurate feedback;
- cloud all-success/partial/all-failed and email cancel/open paths;
- rapid accept/reject and rapid Send taps are idempotent;
- selection/detail survives filter/reorder by stable ID;
- navigation after 50 tab switches has one state-restored entry per section;
- API 26 and current API matching undo/redo;
- 320dp and 412dp widths, landscape, font scales 1.0/1.3/2.0, visible IME,
  TalkBack, keyboard/DPAD, offline/online transition, and process recreation;
- benchmark cold start and 500/1,000-row search/scroll only after functional gates pass.

## Validation results for this audit baseline

| Check | Result |
|---|---|
| `tools/codex/session-init.ps1` | Passed |
| `:app:assembleDebug` | Passed |
| `:app:testDebugUnitTest` | Passed |
| `:app:compileDebugAndroidTestKotlin` | Passed |
| `:app:assembleDebugAndroidTest` | Passed |
| Instrumentation excluding `FieldDetailDialogTest` | Passed, 21 tests |
| Full instrumentation | Failed: process crash in the Field detail/update test dependency path |
| `:app:lintDebug` | Failed: 4 errors, 110 warnings |

Lint errors are the two API-35 `removeLast()` calls, suspicious indentation in
`FieldViewModel.kt:607`, and missing `includeSubdomains` in
`network_security_config.xml:4`. The last error must be resolved together with
the main-manifest cleartext boundary, not by enabling subdomains. The generated report is
`app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.

## Reference standards

The rework direction follows current Android guidance for
[Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility),
[minimum touch targets and component defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults),
[Navigation back-stack state restoration](https://developer.android.com/guide/navigation/backstack),
[adaptive Compose layouts](https://developer.android.com/develop/ui/compose/build-adaptive-apps),
[lifecycle-aware Compose state collection](https://developer.android.com/develop/ui/compose/state),
and [performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices).

## Result summary

The requested deep sweep is complete and now has a durable, implementation-ready
backlog. The highest-return move is not another visual-only pass: first establish
session isolation, requester-bound response state, stable entity identity,
truthful send outcomes, and clean lint/device gates. Once those foundations are
testable, the Contact/Request/Field detail-editor redesign can be built on a
shared adaptive modal system without preserving today's navigation and state bugs.
