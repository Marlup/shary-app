# Scope

Apply the next `Fields` screen and field-editor compaction pass from the reviewed mockup feedback: reduce the top controls to one dense row, collapse cloud status into the inbox control itself, rename statistics to `Fields info`, and make the add/edit field sheet more compact and inference-driven.

## Change list

1. Removed the dedicated `Fields` top app-bar action cluster and rebuilt the header around a title plus one compact control row.
2. Removed the standalone `Cloud ready` strip from `Fields`.
3. Folded cloud state into a single `Inbox ready/offline` button with cloud-color signaling and minimal text.
4. Kept search, sort, tags, and overflow in the same compact row; moved `Add field` out of that row.
5. Added a centered bottom `Add field` button above the navigation bar when no card selection is active.
6. Renamed the overflow action and dialog from `Field statistics` to `Fields info`.
7. Added a `Text` default field category and compact top-row selectors for value type and tag inside add/update field sheets.
8. Removed the field-sheet advanced-options expander, changed key/value prompts to inline guidance text, kept alias as optional, and kept extra controls visible only for non-text categories.
9. Extended `SharySoftButton` with an optional content-color override and added a compact tag selector helper for the editor.

## Plan source

Primary plan: `[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`

Image-review follow-up: `[[docs/backlog/works/2026-07-04/inbox-image-review-field-screen-compaction.md]]`

## Viability/complexity per change

1. Single-row `Fields` header: medium. Required structural changes in the screen scaffold but remained local to one screen.
2. Compact field editor: medium. The densification changed defaults and selector placement, but preserved the existing persistence model for typed values.

## Design decisions

1. The new simple default is `Category.Text`, implemented as a first-class UI type that persists plain text without wrapping it in typed metadata.
2. Cloud readiness stays visible only through the inbox button, using icon color plus one short status word instead of a dedicated message strip.
3. The bottom `Add field` action is only shown while no selection dock is active to avoid overlapping the existing multi-select command dock.

## Validation plan

1. Run `./gradlew :app:compileDebugKotlin`.
2. Run `./gradlew :app:compileDebugAndroidTestKotlin`.

## Result summary

`Fields` is now visibly denser: one compact control row, no standalone cloud banner, overflow renamed to `Fields info`, and the primary add action moved to the bottom center. The add/update field sheet now defaults to text, exposes compact top-row type/tag selectors, removes the advanced expander, and uses lighter inline prompts for required/optional fields. Both Kotlin app compilation and Android-test source compilation passed.
