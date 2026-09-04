# Field Send-Flow Exit Crash Fix

## Scope

Fix the `Fields` screen crash that occurred when the last selected field was
cleared while the command dock was leaving composition.

## Change List

1. Removed the non-null assertion around the optional field-send CTA.
2. Render the command dock only while `fieldSendFlowCta` supplies a valid CTA.

## Viability and Complexity

This is a low-risk UI lifecycle fix. The existing CTA derivation deliberately
returns `null` without a field selection, so the screen must not require a
value during its transition out of the selected state.

## Design Decisions

The CTA derivation remains the single source of truth for both visibility and
contents of the dock. This avoids retaining stale destinations or labels after
the selection has been cleared.

## Validation

Passed:

```powershell
./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.screens.SendFlowCtaTest"
```

The task compiled the debug Kotlin sources and includes the focused
`noLocalSelectionHasNoCta` test for the null-CTA state. Manual device/emulator
verification remains: select and then clear the final field, including through
the delete action, and confirm the Fields screen stays open.

## Result Summary

The null transition no longer reaches `requireNotNull`; the ordinary empty
selection state now removes the command dock safely.
