# Android Layout Inspector Setup

## Scope

Use Android Studio's built-in Layout Inspector to inspect the running Shary debug app on an emulator or physical device. This is for visual and hierarchy diagnosis, not scripted navigation.

## Project Configuration

The app debug variant is configured for Layout Inspector:

- `app/build.gradle.kts` enables Compose with `buildFeatures.compose = true`.
- The debug build type is explicitly `isDebuggable = true`.
- The debug build type keeps minification disabled with `isMinifyEnabled = false`.
- Compose tooling is available through `debugImplementation(libs.androidx.ui.tooling)`.

Use the `debug` variant for inspection. Do not use release builds for Layout Inspector diagnosis.

## Android Studio Steps

1. Open `C:\Users\marsu\AndroidStudioProjects\sharydemo` in Android Studio.
2. Select the `app` run configuration.
3. Select a running emulator, preferably `default-vd-Pixel_9_Pro` if it is available.
4. Run the app with the debug variant.
5. Open `View > Tool Windows > Running Devices`.
6. In the Running Devices toolbar, click `Toggle Layout Inspector`.
7. Enable `Toggle Deep Inspect` when selecting components.
8. Disable `Toggle Deep Inspect` when interacting with the app.
9. Use `Component Tree` and `Attributes` to inspect Compose nodes and layout values.
10. Export a snapshot when comparing before/after visual changes.

## Practical Workflow

For visual redesign work:

1. Navigate manually to the target screen in the emulator.
2. Toggle Layout Inspector.
3. Select the component with Deep Inspect.
4. Check size, padding, colors, hierarchy depth, and recomposition indicators.
5. Take a Layout Inspector snapshot before editing.
6. Make the UI change.
7. Re-run the debug app and compare the new snapshot.

## Troubleshooting

- If Layout Inspector does not attach, confirm the app is running as `debug`.
- If attributes are missing, restart the activity and attach again.
- If a physical device is used, enable device mirroring in Android Studio.
- If the inspector feels slow, keep it embedded and inspect one screen at a time.
- If Compose changes do not appear, rebuild and reinstall the debug app.

## Validation Commands

Run these after project-side Layout Inspector setup changes:

```powershell
./gradlew :app:compileDebugKotlin
```

If Android UI tests were changed, also run:

```powershell
./gradlew :app:compileDebugAndroidTestKotlin
```
