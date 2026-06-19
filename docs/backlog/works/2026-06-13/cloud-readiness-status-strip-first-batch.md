# Cloud readiness status strip first batch

## Scope

Implemented the first low-regression slice from the improvement predesign:

- qualitative cloud readiness labels,
- visible Fields-screen cloud status strip,
- disabled cloud action feedback from the toolbar button,
- clearer progressive loading copy,
- unit coverage for cloud status derivation.

## Change list

- Extended `CloudUiStatus` with `label`, `detail`, and `safetyLabel`.
- Added reusable `CloudStatusStrip`.
- Wired the strip into `FieldsScreen`.
- Kept the cloud inbox action visible when unavailable and made disabled clicks show the specific blocker reason.
- Updated the partial-loading message to explain that the first fields are usable while the rest decrypts in the background.
- Added `CloudUiStateTest`.

## Viability and complexity

- Feasibility: high. The app already had `cloudUiStatus`, cloud state, and field loading state.
- Difficulty: low to medium. The change is mostly UI plus a small pure helper extension.

## Validation plan

- Run `./gradlew :app:compileDebugKotlin`.
- Run targeted unit tests for `CloudUiStateTest`.

## Residual risks

- The status strip is currently wired only to the Fields screen. Summary screens already use `cloudUiStatus` for disabled messaging, but they do not yet render the shared strip.
- The safety labels are qualitative UI text only; they do not yet represent a persisted audit model.
