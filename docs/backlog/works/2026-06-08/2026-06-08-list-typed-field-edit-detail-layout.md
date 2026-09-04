# List Typed-Field Edit And Detail Layout

## Scope

Fix field cards whose typed value is a list so users can update existing elements, add new elements, and inspect list values cleanly from the card detail dialog.

## Change List

- Added inline editable inputs for existing list elements in the add/update field dialogs.
- Preserved the existing pending element plus `+` flow for adding new list items.
- Added parsed typed-list item rendering in `FieldDetailDialog` so detailed mode shows each element vertically.
- Reworked field detail layout so values/alias content stay on the left and related actions stack vertically on the right side.
- Added instrumentation coverage for detail list rendering and updating/adding list elements through `UpdateFieldDialog`.

## Viability And Complexity

This was a positive, low-to-medium complexity fix. The app already stored list typed values as a typed envelope with a separator, so the change could reuse the current parse/encode flow and focus on UI state handling.

## Design Decisions

- Keep list storage unchanged: update `listItems`, then reuse existing `joinListItems` and `encodeValueFromDraft` paths on save.
- Use small Compose test tags only on list editor inputs so behavior tests do not depend on fragile visual text matching.
- Parse list detail items at the `FieldScreen` call site to keep `FieldDetailDialog` mostly presentational.

## Validation Plan

- Compile app Kotlin sources.
- Compile Android instrumentation test sources that cover the changed UI behavior.
- Use existing dialog tests as the regression surface for action callbacks.

## Result Summary

- `./gradlew :app:compileDebugKotlin` completed with exit code 0. Kotlin initially reported a daemon/incremental-cache collision from the earlier parallel compile, then recovered and completed.
- `./gradlew :app:compileDebugAndroidTestKotlin` completed successfully.
