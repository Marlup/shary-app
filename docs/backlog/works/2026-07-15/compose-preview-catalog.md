# Compose preview catalog

## Scope

Add a preview-only catalog for Shary's shared Compose UI states.

## Change list

- Added `SharyComponentPreviews.kt` with representative Fields, Contacts,
  Requests, cloud, search, and section-navigation previews.
- Used only state-driven shared components and realistic sample data.

## Viability and complexity

High viability and low regression risk. The existing route screens retrieve
navigation and ViewModel state directly, so adding previews to those functions
would not be reliable without a separate screen-content refactor.

## Design decisions

- Keep previews in `ui/preview` rather than mixing development samples into
  production components.
- Include interactive state in the navigation and search previews for Android
  Studio Interactive Preview.
- Exclude `MainActivity`, `AppNavigator`, and ViewModel-owned route screens.

## Validation plan

Compile the debug Kotlin source set and open the preview catalog in Android
Studio.

## Result summary

The catalog gives a live design surface for the reusable controls that define
the first visible state of the core Shary sections.

Validation passed: `./gradlew :app:compileDebugKotlin`.
