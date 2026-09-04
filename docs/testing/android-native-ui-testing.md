# Android Native UI Testing

## Current stack status

- Compose UI Testing is already installed in `app/build.gradle.kts` through:
  - `androidTestImplementation(platform(libs.androidx.compose.bom))`
  - `androidTestImplementation(libs.androidx.ui.test.junit4)`
  - `debugImplementation(libs.androidx.ui.test.manifest)`
- UI Automator is now installed for device-level and cross-app checks through:
  - `androidTestImplementation(libs.androidx.test.uiautomator)`

## When to use each tool

### Compose UI Testing

Use it for Shary native Compose surfaces when you want to verify:

- visible text and controls;
- enabled or disabled states;
- callback wiring;
- selection, tab, dialog, and screen-level interactions;
- accessibility semantics that Compose exposes to tests.

Typical pattern:

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun showsConnectTab() {
    composeTestRule.setContent {
        SharySectionNavigationBar(
            currentTab = SectionTab.FIELDS,
            onTabSelected = {}
        )
    }

    composeTestRule.onNodeWithText("Connect").assertExists()
}
```

Run focused compile or tests:

```powershell
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:connectedDebugAndroidTest
```

### UI Automator

Use it when the diagnosis crosses app boundaries or device chrome, for example:

- permission dialogs;
- deep links;
- app launch from launcher;
- biometric or system-confirmation prechecks;
- browser, Settings, or other external surfaces.

Typical pattern:

```kotlin
val instrumentation = InstrumentationRegistry.getInstrumentation()
val device = UiDevice.getInstance(instrumentation)

device.pressHome()
context.startActivity(launchIntent)
assertTrue(device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000))
```

The starter smoke test in `app/src/androidTest/java/com/shary/app/ui/diagnostics/SharyAppLaunchUiAutomatorTest.kt`
shows the expected setup.

## Recommended Shary workflow

1. Start with Compose UI tests for native screen regressions.
2. Use Layout Inspector when spacing, hierarchy, or recomposition behavior is unclear.
3. Add UI Automator only for launcher, deep-link, permission, or cross-app/device flows.
4. Keep cloud or MCP behavior under the existing real-agent and protocol harnesses instead of forcing it into UI-only tests.
