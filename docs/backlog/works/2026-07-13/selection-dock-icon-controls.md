# Selection Dock Icon Controls

## Scope

Standardize the deselect and delete controls used by the Fields, Contacts, and
Requests command docks.

## Change List

1. Replaced the count-based selection pill with a fixed-size `Deselect all`
   icon button.
2. Made delete a fixed destructive icon button in both full-width and
   icon-only dock layouts.
3. Kept the primary action as the only flexible-width control when its text is
   needed to guide the send flow.
4. Added Compose UI coverage for fixed 52dp deselect/delete buttons and the
   absence of selection-count text.

## Viability and Complexity

This is a low-risk shared-component change. All three screens use
`SharyCommandDock`, so the control semantics and geometry remain aligned
without duplicating screen-specific layout code.

## Validation

Passed:

```powershell
./gradlew :app:assembleDebugAndroidTest
adb shell am instrument -w -r -e class com.shary.app.ui.components.SharyCommandDockTest com.shary.app.test/androidx.test.runner.AndroidJUnitRunner
```

The connected-emulator test verifies fixed 52dp deselect/delete geometry in
both dock styles, clear-selection dispatch, and the absence of count text.

## Result Summary

The shared dock now provides a stable, icon-led selection-control layout to
Fields, Contacts, and Requests without selection-count text.
