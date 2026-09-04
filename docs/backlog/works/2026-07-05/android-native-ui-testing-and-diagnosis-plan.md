# Android native UI testing and diagnosis plan

## Scope

Complete the native Android UI test stack by wiring `UI Automator`, document how to use native Compose UI Testing plus UI Automator in this repo, and define a focused diagnosis-and-improvement plan for Shary UI and UX.

## Change list

1. Confirmed Compose UI Testing was already installed and actively used by existing `androidTest` coverage.
2. Added `androidx.test.uiautomator` to the version catalog and app `androidTest` dependencies.
3. Added a minimal device-level smoke test that launches Shary from the launcher using UI Automator.
4. Added a native testing guide under `docs/testing/android-native-ui-testing.md`.
5. Fixed the failing `FieldDetailDialogTest` pure-UI path by allowing tag options to be injected without requiring a Hilt-backed host activity.

## Viability and complexity

- Compose UI Testing: already viable and low-risk because the repo already has Compose instrumentation tests.
- UI Automator: viable and low-risk for launcher, deep-link, permission, and cross-app/device checks.
- Full UX diagnosis suite: medium effort because it should combine unit tests, Compose tests, UI Automator, Layout Inspector, and the existing MCP harness rather than replace them.

## Design decisions

- Keep Compose UI Testing as the default UI regression tool because Shary is a native Compose app.
- Add UI Automator only as a complementary device-level layer, not as the main UI framework.
- Keep MCP and cloud validation under the existing `mcp-testing` harnesses because those flows are not purely local UI behavior.

## Brief how to use

### Compose UI Testing

Use it for native screen behavior inside the app process:

- render a composable with `createComposeRule()`;
- query nodes by text, content description, or semantics;
- perform clicks, text input, scrolls, and assertions;
- keep these tests focused on screen and component behavior.

Current examples:

- `app/src/androidTest/java/com/shary/app/ui/components/SharySectionNavigationBarTest.kt`
- `app/src/androidTest/java/com/shary/app/ui/connector/ConnectorsContentTest.kt`

### UI Automator

Use it for device-level flows outside pure Compose scope:

- app launch from launcher;
- deep links and external approval/browser returns;
- runtime permissions and system dialogs;
- cross-app transitions.

Current example:

- `app/src/androidTest/java/com/shary/app/ui/diagnostics/SharyAppLaunchUiAutomatorTest.kt`

## Diagnosis and test suite plan

1. Baseline compile gate
   - Run `./gradlew :app:compileDebugKotlin`
   - Run `./gradlew :app:compileDebugAndroidTestKotlin`
2. Component regression layer
   - Add or extend Compose tests for top bar, bottom bar, command dock, cards, dialogs, and screen headers.
3. Screen diagnosis layer
   - For each target screen, pair Compose assertions with Android Studio Layout Inspector snapshots before and after changes.
4. Device-flow layer
   - Add UI Automator checks for launcher start, deep links, permission prompts, and any browser-mediated approval return.
5. UX-risk layer
   - Focus first on accessibility labels, compact control density, selection clarity, disabled-state explanation, and first-use guidance.
6. Cloud and MCP layer
   - Reuse `mcp-testing/real` and existing focused JVM tests for connector or cloud behavior instead of forcing it into instrumentation UI tests.
7. Iteration rule
   - Fix one screen slice at a time, add the nearest tests first, then rerun the focused compile and relevant test target before moving on.

## Validation plan

- `tools/codex/session-init.ps1`
- `./gradlew :app:compileDebugAndroidTestKotlin`
- `./gradlew :app:installDebug`
- `./gradlew :app:installDebugAndroidTest --rerun-tasks`
- `adb shell am instrument -w -r -e class com.shary.app.ui.components.SharySectionNavigationBarTest,com.shary.app.ui.connector.ConnectorsContentTest,com.shary.app.ui.components.CloudStatusStripTest,com.shary.app.ui.screens.field.components.FieldDetailDialogTest,com.shary.app.ui.diagnostics.SharyAppLaunchUiAutomatorTest com.shary.app.test/androidx.test.runner.AndroidJUnitRunner`

## Result summary

The repo now has the correct native Android pair for UI diagnosis and test automation: Compose UI Testing for in-app native UI and UI Automator for device or cross-app behavior. The emulator-backed regression slice passed 8 instrumentation tests after fixing the field-detail dialog test crash caused by an implicit Hilt dependency in a pure Compose test host.
