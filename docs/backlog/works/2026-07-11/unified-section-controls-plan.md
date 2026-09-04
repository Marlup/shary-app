# Unified Section Controls Plan

## Summary

Unify the compact control hierarchy across Fields, Contacts, and Requests without changing navigation or domain contracts. Keep search permanently visible, use stable button geometry with ellipsized text, align persistent actions at the bottom right above navigation, and reuse the existing tag-creation workflow in the compact field editor.

## Implementation plan

1. Add shared section-toolbar and bottom-action-shelf components with an adaptive search width targeting 50% and bounded between 40% and 60% when space permits.
2. Make shared labeled buttons fixed-height and one-line ellipsized; clip search input and placeholder overflow.
3. Apply the toolbar to Fields, Contacts, and Requests; remove search toggles and filter Requests by sender, recipient, and field key.
4. Apply right-aligned bottom actions and the icon-only selection dock consistently across the three screens.
5. Stack fixed-width type and tag selectors in field editor headers and expose the existing create-tag dialog from the compact tag menu.
6. Add focused request-filtering and Compose control tests, then compile and run the focused emulator suite.

## Acceptance criteria

- Search remains visible with the placeholder `Search` on all three screens.
- Fields keeps search, sort, tag, and overflow in one non-wrapping row.
- Long button, selector, placeholder, and entered-search text cannot resize or escape their bounds.
- Persistent cloud/add/context actions are adjacent and right-aligned above the navigation tabs.
- The compact tag menu can create and immediately select a new tag through the existing persistence path.
- Request search is case-insensitive and does not leave an index-based selection pointing at a different filtered request.

## Suggested branch

`ui/unify-section-controls`. The implementation remains on the current dirty checkout unless the existing user-owned work is deliberately checkpointed first.
