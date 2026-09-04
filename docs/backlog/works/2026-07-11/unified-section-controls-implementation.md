# Scope

Implement the approved compact toolbar, stable button geometry, bottom action placement, cross-screen design translation, field metadata selector, and create-tag updates across Fields, Contacts, and Requests.

## Change list

1. Added shared `SharySectionToolbar` and `SharyBottomActionShelf` components.
2. Made labeled buttons fixed-height with one-line ellipsis and made search content clip within a fixed-height container.
3. Replaced conditional search controls with permanent `Search` fields on Fields and Contacts, and added permanent request search.
4. Kept Fields search, sort, tag, and overflow in one non-wrapping row with adaptive search allocation.
5. Moved persistent cloud/add/context controls to adjacent right-aligned icon buttons above navigation on all three screens.
6. Standardized selection docks on the existing icon-only mode and moved Contacts edit/duplicate actions into that contextual dock.
7. Added case-insensitive Requests filtering by sender, recipients, and field keys, including selection reset and truthful no-match feedback.
8. Stacked fixed-width field type and tag selectors and connected the compact tag menu to the existing create-tag dialog and view model.

## Plan source

Plan source: [[docs/backlog/works/2026-07-11/unified-section-controls-plan.md#Implementation plan]] -> steps 1-6

## Viability/complexity per change

1. Shared section controls: medium. The change affects three scaffolds but stays within UI composition and preserves existing screen state owners.
2. Request filtering: low. A pure helper covers matching; the screen clears index-based selection when the visible list changes.
3. Compact tag creation: low to medium. It reuses the established dialog and `TagViewModel` persistence flow while adding a test injection boundary.

## Design decisions

1. Search targets half the available toolbar width, shrinks toward 40% to preserve action touch targets, and never intentionally exceeds 60%.
2. Persistent actions use 52 dp icon buttons with semantic color and content descriptions; unavailable cloud actions stay visible and expose a specific reason.
3. Requests keeps its Sent/Received segmented selector below the compact toolbar because it changes the list view rather than executing an action.
4. Type and tag selectors share a fixed 132 dp width and stack vertically so long values ellipsize without moving the sheet title.

## Validation plan

1. Force a clean Kotlin source compile.
2. Run the focused request-filtering JVM tests.
3. Compile Android-test Kotlin sources.
4. Install debug app/test APKs and run the shared controls, compact tag selector, icon button, and navigation instrumentation tests.
5. Inspect a live screen when authenticated app state is available; the current emulator stops at Login and cannot expose the protected primary screens without credentials.

## Result summary

The three primary data screens now share a permanent-search toolbar, stable action geometry, right-aligned persistent controls, and icon-only contextual docks. Request search and compact tag creation have focused automated coverage. Fresh Kotlin compilation, the request unit suite, Android-test compilation, APK installation, and the focused emulator suite passed; the emulator reported `OK (5 tests)`.
