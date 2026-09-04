# Shary codebase navigation for agents

## Purpose

Use this as the first code-oriented retrieval surface after repository policy.
It maps tasks to owning code, supporting documents, focused searches, and
proportional validation so an agent does not need to read the whole application
before planning a change.

This is a curated ownership map, not a second architecture specification.
Runtime and product detail remains in the linked design and architecture docs.

## Five-minute orientation

1. Run `tools/codex/session-init.ps1` and read root `AGENTS.md`.
2. Read `app/AGENTS.md`; for Compose work also read
   `app/src/main/java/com/shary/app/ui/AGENTS.md`.
3. Choose one feature row below and open only its entry points.
4. Trace the relevant symbol through ViewModel, contract, and implementation
   with `rg`; do not inventory every layer first.
5. Read a backlog plan only when the request continues saved work or needs a
   prior decision. Use both backlog indexes rather than source TODO markers.
6. Run the narrowest test that proves the changed behavior, then widen the gate
   in proportion to risk.

## Stable documentation shortcuts

| Need | Read |
|---|---|
| Product architecture and runtime flows | `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md` |
| Visible elements, actions, state owners, and guards | `docs/design/SHARY_ELEMENT_ACTION_MAP.md` |
| Product-level overview | `docs/design/SHARY_APP_EXECUTIVE_OVERVIEW.md` |
| Supported OAuth MCP boundary | `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md` |
| Android UI test choice and device flow | `docs/testing/android-native-ui-testing.md` |
| Script or harness behavior | `docs/ai/SCRIPT_CATALOG.md` |
| Application use-case ownership | nearest `app/src/main/java/com/shary/app/application/**/README.md` |
| Saved plans and implementation history | `docs/backlog/works/works-index.md` |
| Verbatim user orders | `docs/backlog/orders_draft/works-index.md` |

Archived documents are historical evidence, not the default source of current
behavior. A plan-source pointer in a work log is stronger than a similarly named
older proposal.

## Architecture ownership

| Layer | Primary paths | Open when |
|---|---|---|
| App entry and navigation | `MainActivity.kt`, `ui/screens/home/utils/AppNavigator.kt`, `Screen.kt` | startup, auth transition, routes, Back, or top-level tabs change |
| Compose UI | `ui/components/`, `ui/screens/`, `ui/connector/`, `ui/theme/` | visible behavior, interaction, semantics, layout, or navigation changes |
| ViewModels | `viewmodels/` | screen state, events, selections, or async results change |
| Application orchestration | `application/` | a rule spans repositories/services or owns session/connector workflow |
| Domain contracts | `core/domain/` | identities, value rules, repository contracts, or transport-neutral models change |
| Infrastructure | `infrastructure/` | persistence, cloud, MCP, authentication service, notification, or Android transport changes |
| Small shared logic | `utils/` | a controller/helper is shared without owning a product layer |
| JVM tests | `app/src/test/java/com/shary/app/` | logic can run without Android framework or Compose rendering |
| Device tests | `app/src/androidTest/java/com/shary/app/` | Compose semantics, lifecycle, storage, system API, or cross-app behavior matters |

The repository currently spells its persistence package `persistance`. Use that
existing path in searches and changes unless a dedicated migration is approved.

## Feature-to-code shortcuts

| Feature or failure | Start here | Continue only if needed | Tests |
|---|---|---|---|
| Shared adaptive modal behavior | `ui/components/SharyModalScaffold.kt` | Entity-specific detail/editor renderer and its operation-state owner | `SharyModalPolicyTest`, `SharyModalScaffoldTest` |
| Fields list, cards, search, selection | `ui/screens/field/FieldScreen.kt`, `ui/components/FieldCard.kt`, `ui/components/FieldsToolbar.kt` | `viewmodels/field/FieldViewModel.kt`, `core/domain/interfaces/repositories/FieldRepository.kt`, `infrastructure/persistance/repositories/FieldRepositoryImpl.kt` | mirrored `ui/screens/field/` and `ui/components/` test packages |
| Field detail, add, update, tasks, tags | `ui/screens/field/components/FieldDetailDialog.kt`, `ui/screens/field/components/FieldUiManagement.kt`, `ui/screens/field/components/TagPicker.kt` | `core/domain/types/valueobjects/FieldValueContract.kt`, `core/domain/types/valueobjects/TagNameContract.kt`, `viewmodels/field/FieldViewModel.kt`, `viewmodels/field/FieldEditorOperationState.kt`, `viewmodels/tag/TagViewModel.kt`, `viewmodels/tag/TagEditorOperationState.kt` | `FieldEditorOperationControllerTest`, `TagEditorOperationControllerTest`, `TagRepositoryImplTest`, `TagNameContractTest`, `FieldDetailDialogTest`, `FieldTaskEditorTest`, `CompactTagSelectorTest`, `FieldValueContractTest` |
| Contacts | `ui/screens/user/UserScreen.kt`, `ui/components/UserCard.kt`, `ui/components/ContactsToolbar.kt`, `ui/screens/user/components/`, `ui/components/SharyModalScaffold.kt` | `viewmodels/user/UserViewModel.kt`, `viewmodels/user/ContactListPolicy.kt`, `viewmodels/user/ContactTagViewModel.kt`, `viewmodels/user/ContactEditorOperationState.kt`, `core/domain/interfaces/repositories/UserRepository.kt`, `core/domain/interfaces/repositories/ContactTagRepository.kt`, `core/domain/types/enums/ContactSortByParameter.kt`, `infrastructure/persistance/repositories/UserRepositoryImpl.kt`, `infrastructure/persistance/repositories/ContactTagRepositoryImpl.kt` | `ContactEditorOperationControllerTest`, `ContactListPolicyTest`, `ContactTagRepositoryImplTest`, `UserRepositoryImplTest`, `UserEditorDialogTest`, `UserDetailDialogTest` |
| Requests and inbox decisions | `application/request/README.md`, `application/request/RequestDraftEditor.kt`, `ui/screens/request/RequestScreen.kt`, `ui/screens/request/utils/` | `viewmodels/request/RequestViewModel.kt`, `core/domain/models/RequestWorkflowState.kt`, `core/domain/interfaces/repositories/RequestRepository.kt`, `infrastructure/persistance/repositories/RequestRepositoryImpl.kt` | `RequestDraftEditorTest`, `RequestWorkflowReducerTest`, `RequestRepositoryImplTest`, `RequestImportPolicyTest`, `RequestIdentityTest`, `RequestFilteringTest`, `RequestDetailFormattingTest`, `AddRequestDialogTest`, `RequestDetailDialogTest`, `RequestInboxReviewDialogTest` |
| Matching | `ui/screens/utils/FieldMatchingDialog.kt` | `utils/MatchingHistoryController.kt`, `core/domain/types/valueobjects/MatchingState.kt` | `MatchingHistoryControllerTest`, `MatchingHistoryControllerAndroidTest`; add device coverage for the dialog |
| Field sharing review | `ui/screens/summary/SummaryFieldScreen.kt`, `SummaryFieldReviewState.kt`, `ReviewPrivacyCopy.kt` | `application/cloud/CloudDeliveryController.kt`, `application/email/EmailShareController.kt`, request workflow, cloud/email services and selection cache | `SummaryFieldReviewStateTest`, `ReviewPrivacyCopyTest`, cloud/email controller tests, then targeted summary-screen instrumentation |
| Request review/send | `ui/screens/summary/SummaryRequestScreen.kt`, `ReviewPrivacyCopy.kt` | `application/cloud/CloudDeliveryController.kt`, `application/cloud/RequestDeliveryHistory.kt`, `application/email/EmailShareController.kt`, `RequestViewModel.kt`, request/cloud repositories | cloud delivery/history, mapper/repository, email controller, and request reducer tests plus targeted summary-screen instrumentation |
| Guided send CTA | `ui/screens/SendFlowCta.kt` | Fields, Contacts, Requests callers | `SendFlowCtaTest` |
| Authentication and session teardown | `application/session/README.md`, `viewmodels/authentication/` | `AuthenticationServiceImpl.kt`, `SelectionCacheService.kt`, `CloudSessionReset.kt`, crypto manager, and persistence owners | `AuthenticationServiceImplSessionTeardownTest`, `CloudSessionResetTest`, `SelectionCacheServiceTest`; storage/device tests when persistence matters |
| Clipboard copy and cleanup | `ui/screens/field/FieldScreen.kt`, `ui/screens/user/UserScreen.kt` | `viewmodels/clipboard/ClipboardViewModel.kt`, `infrastructure/services/clipboard/ClipboardCleanupCoordinator.kt`, authentication teardown | `ClipboardCleanupCoordinatorTest`, `AuthenticationServiceImplSessionTeardownTest`; device check for Android clipboard metadata |
| Cloud readiness and inbox | `ui/components/CloudStatusStrip.kt`, field/request inbox dialogs | `CloudViewModel.kt`, `infrastructure/services/cloud/` | `CloudStatusStripTest`, `CloudInboxReviewDialogTest`, cloud JVM tests |
| Settings | `ui/screens/settings/SettingsScreen.kt` | `viewmodels/configuration/`, `core/domain/interfaces/repositories/SettingsRepository.kt`, `infrastructure/persistance/repositories/SettingsRepositoryImpl.kt` | focused ViewModel/repository tests; Compose test for state/semantics |
| Connectors and MCP | `ui/connector/`, `viewmodels/configuration/ConnectorSettingsViewModel.kt` | `application/connector/README.md`, connector infrastructure, remote MCP design | connector application and MCP transport/server tests |
| Theme | `ui/theme/`, `viewmodels/configuration/ThemeViewModel.kt` | theme repository and settings | JVM persistence test plus preview/device visual checks |
| App launch and navigation | `MainActivity.kt`, `AppNavigator.kt`, `Screen.kt` | auth/session owners and route callers | targeted navigation test; UI Automator only for device/app boundaries |

Paths in the table are relative to
`app/src/main/java/com/shary/app/` unless shown from the repository root.

The product calls the recipient entity **Contact**, while legacy source names use
`User`. Search both `Contact` and `User`, including lowercase path variants.

## Exact PowerShell search shortcuts

List feature files before opening content:

```powershell
rg --files app/src/main/java/com/shary/app | rg "Field|User|Request|Summary"
```

Find a declaration and every direct call site:

```powershell
rg -n "fun CompactTagSelector|CompactTagSelector\(" app/src/main app/src/test app/src/androidTest
```

Trace screen state to its owner:

```powershell
rg -n "class RequestViewModel|RequestViewModel|RequestRepository" app/src/main app/src/test
```

Find navigation ownership without reading all screens:

```powershell
rg -n "composable\(|navigate\(|popUpTo|launchSingleTop|restoreState" app/src/main/java/com/shary/app/ui
```

Find matching tests or the closest package mirror:

```powershell
rg --files app/src/test app/src/androidTest | rg "FieldDetail|Matching|RequestInbox"
```

Check whether a suspected dead surface still has callers:

```powershell
rg -n "SuspectedSymbol|SuspectedRoute" app/src/main app/src/test app/src/androidTest docs
```

Recover the newest durable plan through indexes:

```powershell
rg -n -i "matching|session|ui/ux|connector" docs/backlog/works/works-index.md docs/backlog/orders_draft/works-index.md
```

Inspect only relevant local changes before editing a dirty worktree:

```powershell
git status --short -- app/src/main/java/com/shary/app/ui app/src/test app/src/androidTest
git diff -- app/src/main/java/com/shary/app/ui/screens/request/RequestScreen.kt
```

## Planning shortcuts by change type

### Visual or interaction change

1. Open the screen and its shared component.
2. Check the owning ViewModel only if behavior or state changes.
3. Inspect previews for fast visual coverage and Compose tests for semantics.
4. Verify compact width, large font, visible IME, and offline/cloud-disabled
   states when relevant.

### State, callback, or navigation bug

1. Reproduce the exact event path from screen to state owner.
2. Extract or update a pure reducer/policy/controller when possible.
3. Add a JVM regression test before or with the Compose wiring change.
4. Add device coverage when lifecycle, navigation, or Android APIs are part of
   the defect.

### Persistence, security, cloud, or MCP change

1. Start with the domain interface and application rule, then inspect the
   implementation.
2. Check the active architecture/security document.
3. Inspect `C:\main_work\shary-project\shary-backend` when a shared contract
   changes.
4. Keep secrets out of all files and preserve offline local behavior.

### Cleanup or stale-code removal

1. Verify Kotlin, test, preview, navigation, and documentation call sites.
2. Compile before widening deletion.
3. Keep cleanup separate from correctness or UX patches so regressions remain
   attributable.

## Validation ladder

| Risk | Suggested first proof |
|---|---|
| Pure policy/controller/value contract | `.\gradlew.bat :app:testDebugUnitTest --tests "fully.qualified.TestClass"` |
| Kotlin production wiring | `.\gradlew.bat :app:compileDebugKotlin` |
| Compose/device test source | `.\gradlew.bat :app:compileDebugAndroidTestKotlin` |
| Manifest, API level, resources, or static correctness | `.\gradlew.bat :app:lintDebug` |
| Integrated app packaging | `.\gradlew.bat :app:assembleDebug` |
| Compose semantics/lifecycle/device API | targeted `adb shell am instrument` from the native UI testing guide |
| Release/debug boundary | build and inspect both merged manifests; do not infer release policy from debug |

Use the full gate specified by the active work plan before declaring a phase
complete.

## Maintenance and generation strategy

Keep the checked-in map curated because file names alone cannot reliably infer
state ownership, security boundaries, or the best starting point. Avoid
auto-generating prose or one `AGENTS.md` per package.

If automation is added later, split it into two outputs:

1. **Curated, checked in:** this document, scoped rules, feature ownership, and
   stable document pointers.
2. **Generated, disposable:** symbol/file inventories under `build/agent-index/`
   created from `rg --files` and declaration searches. Generated output must not
   become policy or be required for a normal build.

A future checker may validate that every path named in this map exists and that
linked Markdown files resolve. It must report drift, not rewrite descriptions.
Update this document in the same patch when a feature entry point, layer owner,
test location, or active retrieval surface changes.
