# Work Log - 2026-05-16

## Scope
Evaluation of proposed changes before implementation, with viability, complexity, risks, and design approach.

## Change-by-change impact

1. **Auto-detect typed value on add/update**
   - Viability: High.
   - Complexity: Medium.
   - Impact: `FieldUiManagement.kt` already has `FieldValueContract` parse/encode and typed draft state, so auto-detection can be added by introducing a lightweight detector from raw input (URL, JSON, list, file URI).  
   - Risk: false positives if forcing type silently.
   - Design: suggest value type automatically but keep manual override as source of truth.

2. **New type `link/url`**
   - Viability: High.
   - Complexity: Medium.
   - Impact: add a new `ValueTypeOption` (`LinkUrl`), persist as `kind=link_url`, and render link semantics in cards/detail dialog.
   - Risk: backward compatibility for existing encoded values.
   - Design: map legacy hints (`url`, `link`) to `link_url` in parser.

3. **Tag-filter icon not intuitive**
   - Viability: High.
   - Complexity: Low.
   - Impact: currently uses `FilterList`; replace with a tag metaphor icon.
   - Design: use `Icons.Default.LocalOffer` (tag symbol) and keep tooltip/content description `"Filter by tag"`.

4. **Sort icon not intuitive for KEY/TAG/DATE_ADDED**
   - Viability: High.
   - Complexity: Low.
   - Impact: `SortMenu` entry affordance can switch to clearer symbol.
   - Design: use `Icons.Default.SwapVert` or `Sort`; keep menu unchanged.

5. **Menu labels lowercase and better date naming**
   - Viability: High.
   - Complexity: Low.
   - Impact: label-only updates in enum/UI texts.
   - Design: `KEY -> Key`, `TAG -> Tag`, `DATE_ADDED -> Added at`.

6. **Copy-on-hold should trigger at threshold without waiting release + calm success sound**
   - Viability: High.
   - Complexity: Medium.
   - Impact: `FieldCard.kt` and `UserCard.kt` currently trigger only after release (`tryAwaitRelease` path).  
   - Design: switch to threshold-driven trigger inside `onPress` coroutine (fire once at `copyLongPressMillis`, cancel if release early). Keep snackbar unchanged. For sound, play one short `ToneGenerator` beep (`TONE_PROP_ACK`, low duration/volume) after successful copy.
   - Risk: avoid repeated trigger while still pressing.

7. **Card behavior split: toggle circle = select; rest of card = open detail dialog**
   - Viability: High.
   - Complexity: High.
   - Impact: current cards treat full surface tap as selection. Requires interaction refactor in `FieldCard`, `UserCard`, screen wiring, and new detail dialog composables.
   - Design:
     - `onToggleSelection` bound only to bottom-right ring.
     - `onCardTap` opens read-only detail dialog.
     - Detail content (field): title row with key + tag chip + typed-value chip, then value, then alias.
     - Apply same interaction contract to similar list screens (fields/users, and request cards where applicable).

8. **Selected-count pill too wide, place between Delete and Review; rename Review label**
   - Viability: High.
   - Complexity: Medium.
   - Impact: `SharyCommandDock` currently renders full-width `SelectionPill` in separate row.
   - Design:
     - Move compact selection indicator inline inside button row.
     - Replace text with short form like `"3 selected"` + compact clear icon button.
     - Primary action label: `"Review"` (remove `"& Send →"`), keep navigation target unchanged.
     - Reuse same pattern across screens using `SharyCommandDock`.

9. **Search input hidden by default behind lens button; highlighted when active; apply to similar screens**
   - Viability: High.
   - Complexity: Medium.
   - Impact: field/user screens render search bar always visible.
   - Design:
     - Add left-most search icon toggle in action row.
     - `showSearch` default false; when true show `SharySearchBar`.
     - Active visual state on icon (tint/container).
     - Reuse on other screens with search widgets.

## Overall delivery complexity
- Total: **Medium-High** (mostly UI interaction refactor, not architectural risk).
- Safe implementation order:
  1) label/icon/search toggles,
  2) copy-on-threshold + sound,
  3) command dock compaction,
  4) card interaction split + detail dialogs,
  5) typed-value auto-detection + `link_url`.

## Validation plan
- Unit tests for typed-value detection and `link_url` encode/parse compatibility.
- Compose UI tests for:
  - selection only through ring toggle,
  - card tap opens detail dialog,
  - search bar hidden by default and shown by toggle,
  - copy trigger occurs at hold threshold without requiring release.
- Manual check on field and user screens for action placement and compact selection indicator.
