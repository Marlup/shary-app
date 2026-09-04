# Scope

Change the `Fields` selection dock so its actions follow the same icon-button language as the add, cloud-inbox, and search controls, while keeping the shared dock stable for `Users` and `Requests`.

## Change list

1. Added `contentDescription` to `DockAction` so icon-only dock actions can keep explicit accessibility semantics even when no text is rendered.
2. Added `DockVisualStyle` to `SharyCommandDock` and preserved the existing full-width text-button layout as the default shared behavior.
3. Added an icon-only dock rendering path that uses `SharyIconButton` for destructive, secondary, and primary dock actions.
4. Updated `SharyIconButton` so custom icon-button colors still dim correctly when disabled.
5. Switched only `FieldScreen` to the icon-only dock mode, using a right-arrow review action and a text-free delete icon with explicit content descriptions.

## Viability/complexity per change

1. Shared dock style split: medium. The dock is reused in three screens, so the safest implementation was an additive visual mode instead of a shared redesign.
2. Field-only adoption: low. `FieldScreen` already owned the desired compact action language.
3. Accessibility semantics: low. The icon-only path needed explicit content descriptions to avoid regressing screen-reader clarity.

## Design decisions

1. `Users` and `Requests` keep the existing dock layout because the request only targeted `FieldScreen`.
2. The icon-only dock centers the selection pill independently and anchors actions to the sides, matching the recent `Fields` bottom-control treatment.
3. The delete action keeps no visible text in `FieldScreen`, but its accessibility label is still `Delete selected fields`.

## Validation plan

1. Run `./gradlew :app:compileDebugKotlin`.
2. Run `./gradlew :app:compileDebugAndroidTestKotlin`.

## Result summary

`FieldScreen` now uses an icon-only `SharyCommandDock` that visually matches the add, cloud-inbox, and search buttons. The delete action has no visible text, the review action uses a forward arrow, and the shared dock component remains backward-compatible for `Users` and `Requests`. `./gradlew :app:compileDebugKotlin` and `./gradlew :app:compileDebugAndroidTestKotlin` both passed on 2026-07-05. No dedicated dock test was added because this pass changed shared Compose presentation rather than isolated domain logic; validation was limited to compile gates and direct source inspection.
