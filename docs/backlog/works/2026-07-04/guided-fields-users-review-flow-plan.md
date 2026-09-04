# Scope

Plan and evaluate a behavior change for the `Fields` and `Users` send flow so
first-time users get an explicit next step instead of having to infer that they
must switch screens before `Review` becomes usable.

## Current behavior evidence

1. `FieldsScreen` only shows the command dock when at least one field is
   selected, and its primary action is always labeled `Review`; it is enabled
   only when at least one cached user already exists.
   - Evidence: `app/src/main/java/com/shary/app/ui/screens/field/FieldScreen.kt`
     lines `513-554`.
2. `UsersScreen` uses the same `Review` label and enables it only when at least
   one user is selected and at least one payload is already cached.
   - Evidence: `app/src/main/java/com/shary/app/ui/screens/user/UserScreen.kt`
     lines `249-290`.
3. `SharyCommandDock` currently renders its action row only when the local
   `selectedCount > 0`.
   - Evidence: `app/src/main/java/com/shary/app/ui/components/SharyCommandDock.kt`
     lines `46-95`.

## Problem statement

The current model is technically coherent but weak as a first-use teaching
surface:

1. `Review` suggests immediate readiness even when the real missing step is
   cross-screen selection.
2. The app relies on the user discovering that selection is cached across
   screens.
3. The next required action is not stated explicitly.

## Proposed options

### Option A: Guided relabel with same dock visibility model

Keep the current command-dock visibility rule: the dock appears only after the
current screen has at least one local selection.

Behavior:

1. `FieldsScreen`
   - if `selectedFields > 0` and no cached user:
     - primary label: `Select Users`
     - primary enabled: `true`
     - primary action: navigate to `Users`
   - if `selectedFields > 0` and cached user exists:
     - primary label: `Review`
     - primary enabled: `true`
     - primary action: navigate to `SummaryField`
2. `UsersScreen`
   - if `selectedUsers > 0` and cached field payload exists:
     - primary label: `Review`
     - primary enabled: `true`
     - primary action: navigate to `SummaryField`
   - if `selectedUsers > 0` and no cached field payload exists:
     - primary label: `Select Fields`
     - primary enabled: `true`
     - primary action: navigate to `Fields`

Important limitation:

This option does **not** literally show a disabled `Review` button on the
destination screen before any local selection exists, because the current dock
does not render when local `selectedCount == 0`. It still delivers the main UX
gain: the originating screen tells the user the next step explicitly.

### Option B: Guided relabel plus cross-screen inactive dock state

Extend `SharyCommandDock` so it can render a context CTA even when the current
screen has no local selection but the opposite side is already cached.

Behavior:

1. All Option A label changes.
2. Destination screen can render a disabled primary action such as:
   - `Review` disabled while waiting for local selection
   - or helper text like `Select at least one user to continue`

Impact:

1. More explicit onboarding.
2. Larger behavioral and visual change because the dock stops being
   selection-only.
3. Requires new empty-selection dock rules and more testing.

### Option C: Guided relabel with content-type chooser on `Users`

Same as Option A on `Fields`, but do **not** hardcode `Select Fields` on
`Users`. Instead:

1. If no payload is cached and at least one user is selected:
   - primary label: `Select Content`
   - action: open a chooser or route intentionally to `Fields` / `Requests`
2. If field payload is cached:
   - primary label: `Review`
   - action: `SummaryField`
3. If request payload is cached:
   - primary label: `Review`
   - action: `SummaryRequest`

Reason:

`UsersScreen` is not purely symmetric with `FieldsScreen`; it currently serves
both field-send and request-send flows. A literal `Select Fields` label is only
fully correct if the product intentionally prioritizes field sending over
request sending.

## Recommendation

Recommend **Option A for `Fields`** and **Option C for `Users`**.

Why:

1. `Fields -> Users` is cleanly symmetric and low risk.
2. `Users -> Fields` is not truly symmetric in the current product because
   `Users` can lead to either `SummaryField` or `SummaryRequest`.
3. A hardcoded `Select Fields` CTA on `Users` would be simple, but it would
   encode a product bias that is not reflected in the current screen logic.

If the product owner explicitly wants to prioritize sending stored fields over
request creation, then a narrower **Option A on both screens** is still viable
and simpler.

## Worthiness evaluation

| Characteristic | Current flow | Option A | Option B | Option C | Verdict |
| --- | --- | --- | --- | --- | --- |
| ROI | Medium-low | High | Medium | High | A/C strongest |
| First-use clarity | Low-medium | High | Very high | High | A/C materially improve |
| Simplicity | High | High | Medium-low | Medium | A simplest |
| Robustness | Medium | Medium | High | High | C safer than forcing symmetry |
| Regression risk | Low | Low | Medium-high | Medium | A safest |
| Implementation cost | None | Low | Medium-high | Medium | A cheapest |
| Product truthfulness | Medium | High on `Fields`, medium on `Users` | High | High | C best overall |
| Test surface | Existing | Small | Larger | Medium | A easiest to validate |

## Comparative conclusion

The new guided-next-step approach is **worth doing**.

Why it is worthy:

1. It attacks a real comprehension problem at the exact interaction point where
   confusion appears.
2. It preserves the existing cached-selection model instead of replacing the
   send flow architecture.
3. The minimal version is inexpensive and likely to improve first-session task
   completion noticeably.

Why full symmetry is only partially worthy:

1. It is clean for `Fields`.
2. It is only conditionally correct for `Users`, because `Users` currently
   supports both field-send and request-send review paths.

## Recommended implementation plan

1. Introduce a small derived `SendFlowCtaState` per screen:
   - `label`
   - `enabled`
   - `target`
2. In `FieldsScreen`, compute:
   - `Select Users` when fields are selected and no cached user exists
   - `Review` when fields are selected and cached user exists
3. In `UsersScreen`, compute one of:
   - `Review` -> `SummaryField` when field payload cached
   - `Review` -> `SummaryRequest` when request payload cached
   - `Select Fields` or `Select Content` when no payload cached
4. Keep destructive actions and selection pill unchanged.
5. Keep the dock selection-only for the first implementation slice.
6. Add focused tests for the CTA-state computation and the click routing.

## Validation plan

1. Unit-test CTA derivation for both screens.
2. Add at least one Compose/UI-layer test or nearest-logic test for:
   - fields selected + no users => label `Select Users`
   - fields selected + cached users => label `Review`
   - users selected + no payload => label `Select Content` or `Select Fields`
   - users selected + cached fields => route to `SummaryField`
   - users selected + cached request payload => route to `SummaryRequest`
3. Compile:
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:compileDebugAndroidTestKotlin`

## Result summary

The guided-next-step concept is a good change with strong UX upside and low
implementation cost, but only the `Fields -> Users` path is truly symmetric in
today's product model. The best plan is a low-risk CTA relabel/reroute change
that explicitly guides the user to the missing screen while preserving the
existing review flow and cached-selection behavior.
