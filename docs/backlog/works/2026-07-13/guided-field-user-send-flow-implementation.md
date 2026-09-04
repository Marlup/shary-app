# Guided Field and User Send Flow Implementation

## Scope

Implement the planned cross-screen CTA behavior for sending stored fields to
selected users. The existing review screen remains the final confirmation step;
the new CTAs only guide the user to the missing selection.

## Change List

1. Added a small, unit-testable `SendFlowCta` state derivation shared by
   `FieldsScreen` and `UsersScreen`.
2. Changed the Fields primary action to show `Select Users` and open Contacts
   whenever fields are selected but no recipient is cached.
3. Changed the Contacts primary action to show `Select Fields` and open Fields
   whenever users are selected but no field payload is cached.
4. Preserved `Review` routing to `SummaryField` after both field and user
   selections exist, and preserved the separate request-review route.
5. Cached the current selection immediately before cross-screen navigation so
   the next screen does not depend on lifecycle timing.
6. Added focused unit coverage for every CTA routing state.

## Plan Source

Plan source: [[docs/backlog/works/2026-07-04/guided-fields-users-review-flow-plan.md#Recommended implementation plan]] -> steps 1-6.

## Viability and Complexity

The change is low risk because it retains the existing selection cache and
summary screens. It adds only a pure state derivation plus explicit navigation
targets. The request flow remains distinct from the field-send flow.

## Design Decisions

- `Select Users` and `Select Fields` use the full-width dock treatment so the
  required next step is visible as text.
- Once both sides are selected, the dock returns to its existing compact
  icon-only `Review` action.
- Contacts hide edit and duplicate shortcuts only while the explicit
  `Select Fields` CTA is shown, avoiding a cramped multi-action dock. Those
  shortcuts remain available after the payload is selected.
- The CTA continues to say `Review`, not `Send`, after both selections exist:
  it opens the existing review-and-send confirmation flow rather than sending
  data immediately.

## Validation

Passed:

```powershell
./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.screens.SendFlowCtaTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

Manual verification to perform on a device/emulator:

1. Select one or more fields with no recipients selected and verify the visible
   `Select Users` CTA opens Contacts.
2. Select one or more contacts with no fields selected and verify the visible
   `Select Fields` CTA opens Fields.
3. Select both fields and contacts through either order and verify `Review`
   opens the existing field summary before send.
4. Start a request flow and verify selected contacts still open its request
   summary rather than Fields.

## Result Summary

The guided selection behavior is implemented and covered by focused unit tests.
The first-use flow now states the next required screen instead of exposing a
disabled review action.
