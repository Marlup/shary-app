# Scope

Review the three corrected `inbox-fromuser` PNG mockups against the current Android implementation, then apply the clear `Fields` screen compaction changes that remained after the broader MCP/accessibility batch.

## Change list

1. Reviewed `add-field-image-correction-suggestion.png`, `Connect-not-4th-bottom-tab-but-2nd.png`, and `Rearange-field-screen.png` against the current code.
2. Confirmed no navigation code change was needed for the `Connect` bottom-tab order because the implemented navigation bar already places `Connect` second.
3. Removed the always-visible stored-count subtitle from `Fields` and promoted the section title so the top bar no longer leaves a wide low-value empty band.
4. Extended `PrimaryTabOverflowMenu` with optional extra items and added a `Field statistics` action on `Fields`.
5. Added a `Field statistics` dialog that reports total stored fields, the currently shown count when filtered/searched, and grouped counts by tag.
6. Simplified `CloudStatusStrip` to a compact icon-led status with short visible labels while preserving the fuller status explanation in accessibility semantics.
7. Added focused coverage for the new tag-statistics helper and the compact cloud-status UI.

## Plan source

Primary plan: `[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`

Image-review follow-up source: `[[docs/backlog/orders_draft/2026-07-04/raw-order-inbox-image-review-field-screen-compaction.md]]`

## Viability/complexity per change

1. Image review and code mapping: low. The mockups pointed to one already-correct nav detail and one real remaining `Fields` layout issue.
2. Overflow-based field statistics: low. The data was already present in memory on the screen; only aggregation and presentation were needed.
3. Compact cloud strip: low to medium. The visible UI changed materially, but the logic stayed localized to one shared component.

## Design decisions

1. The stored counter moved behind an explicit overflow action instead of staying permanently visible because the mockup feedback was primarily about wasted header space.
2. The cloud strip now favors short visual copy and colored status signaling, while `stateDescription` carries the fuller explanation for assistive technologies.
3. The field-stat aggregation was extracted into a small helper so the sorting/grouping behavior could be covered by a JVM test.

## Implementation plan

1. Review the corrected mockups against the existing implementation.
2. Patch the shared overflow menu to accept screen-specific actions.
3. Refactor the `Fields` top bar and add statistics presentation.
4. Compress the cloud strip without removing accessibility detail.
5. Re-run compile, focused unit coverage, Android-test compilation, APK install, and direct instrumentation.

## Validation plan

1. Run `./gradlew :app:compileDebugKotlin`.
2. Run `./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.screens.field.FieldStatisticsTest"`.
3. Run `./gradlew :app:compileDebugAndroidTestKotlin`.
4. Run `./gradlew :app:installDebug :app:installDebugAndroidTest`.
5. Run direct instrumentation for `SharySectionNavigationBarTest`, `SharyIconButtonTest`, `ConnectorsContentTest`, and `CloudStatusStripTest`.

## Result summary

The image review confirmed that the main remaining mismatch was the `Fields` header density, not the bottom navigation order. `Fields` now uses its top space more intentionally, the detailed field counts live behind a dedicated statistics action, and the cloud status reads as a short colored signal instead of a text-heavy banner. Kotlin compile, the new focused JVM test, Android-test compilation, debug/test APK installation, and the focused connected instrumentation slice all passed on `emulator-5554`.
