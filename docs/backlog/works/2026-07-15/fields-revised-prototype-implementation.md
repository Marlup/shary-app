# Fields revised prototype implementation

## Scope

Implement the approved `inbox-fromuser/shary-fields-revised.html` direction in
the live Fields screen, including the later corrections that remove the title
area, keep sorting fixed, and label the cloud action accurately.

## Change list

- Added a Fields-only toolbar with `Search fields`, an adjacent overflow menu,
  a fixed sort control, and direct horizontally scrolling tag chips.
- Reworked field cards to support two-line keys, a highlighted mono value,
  an inset divider, and a bold violet alias label.
- Added a clickable dashed vault-building nudge only for unfiltered vaults
  containing one or two fields.
- Replaced the icon-only idle actions with equal-size `Cloud inbox` and
  `Add field` controls. Cloud availability remains visible through its colour
  and unavailable taps still explain the reason.
- Added direct layout-policy and Compose coverage for the nudge condition and
  direct tag-toolbar structure.

## Viability and complexity per change

- Toolbar: high viability, low risk. It is isolated to Fields so Contacts and
  Requests retain their current shared toolbar behavior.
- Card hierarchy: high viability, low risk. It changes Compose presentation
  only and preserves tap, selection, copy, and sensitive-value access paths.
- Cloud action: high viability, low risk. The label now describes the existing
  inbox action rather than introducing a new synchronization contract.

## Design decisions

- The top title/count band is removed completely; overflow belongs beside the
  search field.
- The sort control is deliberately outside the `LazyRow`; tag filters alone
  scroll horizontally.
- The vault nudge is suppressed during search/filtering so it never implies
  that a filtered list represents the whole vault.

## Follow-up card compaction

- Field keys are deliberately one line with ellipsis, so a long key never
  increases card height.
- The value and 48dp selection target share one row. The value receives the
  remaining width after an 8dp gap, approximately 84% of the card's content
  width on a 390dp device.
- Tag and typed-value chips use fixed dimensions and ellipsis, preserving
  card geometry regardless of metadata length.
- The static Key sort control is 54dp by 40dp, with a 1.5dp violet border and
  white background for a clearer, more compact contrast treatment.

## Validation plan

1. Compile the debug Kotlin source set.
2. Run `FieldScreenLayoutPolicyTest`.
3. Compile Android instrumentation tests, including the Fields toolbar test.
4. Inspect the app on an emulator for title-free toolbar layout, scrollable
   tags, nudge visibility, card hierarchy, and cloud-unavailable feedback.

## Result summary

Implementation completed.

Validation passed:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.screens.field.FieldScreenLayoutPolicyTest"`
- `./gradlew :app:compileDebugAndroidTestKotlin`
- Installed refreshed debug and Android-test APKs on `emulator-5554`.
- `adb shell am instrument -w -r -e class com.shary.app.ui.components.SharySectionControlsTest com.shary.app.test/androidx.test.runner.AndroidJUnitRunner` — `OK (4 tests)`, including fixed-sort/direct-tag-toolbar and fixed-long-tag checks.

The direct emulator check confirms the Fields toolbar keeps sorting static and
exposes direct tag chips in the dedicated horizontal scroller.
