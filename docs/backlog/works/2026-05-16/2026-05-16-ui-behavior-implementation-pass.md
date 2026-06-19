# Work Log - 2026-05-16 (Implementation Pass)

## Scope
Implement the approved UI/interaction and typed-value changes on Fields/Users shared flows.

## Changes implemented

1. **Typed value auto-detection on add/update**
   - Added save-time auto-detection in `FieldUiManagement.kt` for:
     - `link_url` (`http/https/ftp/mailto`),
     - `json` (object/array),
     - `list` (comma-separated values),
     - `file` (`content://` or `file://` URI).
   - Auto-detection runs only when typed mode was not manually touched in the dialog.

2. **New typed value kind `link_url`**
   - Added `ValueTypeOption.LinkUrl("link_url", "Link/URL")`.
   - Added compatibility mapping for legacy wire kinds `link` and `url`.
   - Extended strict validation allowlist in `FieldValueContract` for `link_url`.

3. **Filter/sort icon and labels UX**
   - Fields filter icon switched from generic filter list to tag icon (`LocalOffer`).
   - Sort affordance uses `SwapVert`.
   - Sort menu labels now user-facing labels (`Key`, `Tag`, `Added at`) instead of enum names.

4. **Copy-on-hold behavior**
   - `FieldCard` and `UserCard` now trigger copy as soon as hold threshold is reached (no release needed).
   - Added short calm success tone feedback (`CopyFeedback.playSuccessTone()`).

5. **Card interaction split**
   - Selection now toggles only via bottom-right circle toggle.
   - Tapping the rest of the card opens a detail dialog:
     - Field detail includes key + tag + typed-value chip, then value and alias.
     - User detail includes name and email.

6. **Selection indicator compaction in command dock**
   - Reworked shared `SharyCommandDock` + `SelectionPill`:
     - compact inline selection badge,
     - placed between destructive action and primary action.
   - Updated primary action labels in Fields/Users to `Review`.

7. **Search input visibility toggle**
   - Added left-most search icon toggle in Fields and Users top bar.
   - Search input hidden by default.
   - Search icon highlights when active.

## Files changed (high signal)
- `app/src/main/java/com/shary/app/ui/screens/field/FieldScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/user/UserScreen.kt`
- `app/src/main/java/com/shary/app/ui/components/FieldCard.kt`
- `app/src/main/java/com/shary/app/ui/components/UserCard.kt`
- `app/src/main/java/com/shary/app/ui/components/SharyCommandDock.kt`
- `app/src/main/java/com/shary/app/ui/components/SelectionPill.kt`
- `app/src/main/java/com/shary/app/ui/components/SharyButtons.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/components/SortMenu.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/components/FieldUiManagement.kt`
- `app/src/main/java/com/shary/app/core/domain/types/valueobjects/FieldValueContract.kt`
- `app/src/main/java/com/shary/app/core/domain/types/enums/SortByParameter.kt`
- New: `app/src/main/java/com/shary/app/ui/screens/field/components/FieldDetailDialog.kt`
- New: `app/src/main/java/com/shary/app/ui/screens/user/components/UserDetailDialog.kt`
- New: `app/src/main/java/com/shary/app/ui/utils/CopyFeedback.kt`

## Verification
- Compile check executed:
  - `./gradlew :app:compileDebugKotlin`
  - Result: **BUILD SUCCESSFUL**
