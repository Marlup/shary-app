# Android Layout Inspector Setup

## Scope

Configure and document Android Studio Layout Inspector usage for the Shary Android app.

## Change List

- Made the `debug` build type explicitly debuggable.
- Made the `debug` build type explicitly non-minified.
- Added a repo-local Layout Inspector setup and troubleshooting guide under `docs/testing/android-layout-inspector.md`.

## Viability and Complexity

Viability: high. Android Studio Layout Inspector is built in, and the app already uses Compose debug tooling.

Complexity: low. No runtime behavior changes are required.

## Design Decisions

- Keep the setup project-local and debug-only.
- Do not add a third-party automation tool for this step.
- Treat Layout Inspector as a manual visual diagnosis tool; use Maestro/UI Automator later for scripted navigation.

## Validation Plan

- Compile the debug Kotlin source with `./gradlew :app:compileDebugKotlin`.
- No Android UI test changes were made, so Android-test compilation is optional.

## Result Summary

The app now has explicit debug inspectability settings and a durable guide for opening Layout Inspector from Android Studio's Running Devices tool window.
