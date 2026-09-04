# Scope

Tune the `Fields` bottom controls and compact-header buttons in `sharydemo`: make the key-sorting and tag-filter controls smaller through explicit shared-button parameters, replace the inbox cloud icon treatment with a stronger-contrast `CloudDownload` affordance that signals availability in green or red, and keep the `+` add-field action truly horizontally centered.

## Change list

1. Extended `SharySoftButton` with tuneable `minHeight`, `iconSize`, `contentPadding`, and `containerColor` parameters so the compact header controls can be adjusted without forking the shared button.
2. Extended `SharyIconButton` with tuneable `buttonSize`, `iconSize`, `containerColor`, and `contentColor` parameters so stateful icon-only actions can carry stronger visual meaning.
3. Reduced the `Fields` sort and tag-filter buttons to a smaller 40 dp visual height with tighter horizontal padding.
4. Replaced the old bottom `Cloud + Inbox/Offline` control with a `CloudDownload` icon action that uses strong green or red background contrast depending on cloud readiness.
5. Split the bottom actions into left, center, and right anchors so the `+` add-field button stays truly centered instead of drifting with the other controls.

## Viability/complexity per change

1. Shared-button parameterization: low. The new parameters preserve current defaults and only opt in where needed.
2. Bottom-action recentering: low to medium. The behavior is simple, but the layout needed to move from one centered row to anchored positions.
3. Cloud affordance contrast: low. The existing readiness logic was preserved; only the presentation changed.

## Design decisions

1. The compact sort and tag-filter buttons now tune size through the shared `SharySoftButton` API instead of hard-coded one-off modifiers in `FieldScreen`.
2. The cloud inbox affordance now uses `Icons.Default.CloudDownload` as an icon-only action, with color carrying readiness instead of a redundant `Inbox` text label.
3. The add button stays in the `floatingActionButton` slot, but its internal layout is now anchored with `CenterStart`, `Center`, and `CenterEnd` alignment so the central action remains visually centered.

## Validation plan

1. Run `tools/codex/session-init.ps1`.
2. Run `./gradlew :app:compileDebugKotlin`.
3. Run `./gradlew :app:compileDebugAndroidTestKotlin`.

## Result summary

The `Fields` sort and tag-filter buttons are now smaller and tuneable through shared parameters. The cloud inbox affordance now uses a higher-contrast `CloudDownload` icon treatment with explicit green/red availability signaling, and the `+` add-field control is centered independently from the left and right utility actions. `tools/codex/session-init.ps1`, `./gradlew :app:compileDebugKotlin`, and `./gradlew :app:compileDebugAndroidTestKotlin` all passed on 2026-07-05. No new unit test was added because this patch changes Compose layout/styling rather than isolated business logic; verification was limited to the focused compile gates and direct source inspection of the final scaffold structure.
