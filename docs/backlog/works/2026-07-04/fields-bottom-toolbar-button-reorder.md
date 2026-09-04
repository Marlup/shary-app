# Scope

Apply the follow-up `Fields` toolbar adjustment requested after the compact
single-row pass: move the cloud action to the left of the centered add action,
move the search lens to the right of it, and reduce the add button label to
`+`.

## Change list

1. Removed the top-row cloud inbox button from the `Fields` action row.
2. Removed the top-row search lens button from the `Fields` action row.
3. Replaced the single bottom `Add field` button with a three-control bottom
   cluster ordered as `Cloud | + | lens`.
4. Shortened the add action label from `Add field` to `+`.
5. Kept the existing cloud readiness feedback and search toggle behavior behind
   the relocated controls.

## Viability/complexity per change

1. Bottom action reordering: low. The screen already used a centered floating
   action slot, so the change stayed local to the `Fields` scaffold.
2. Search relocation: low. The button moved, but the existing `showSearch`
   state and inline search bar behavior remained unchanged.

## Design decisions

1. Kept the sort, tag filter, and overflow menu in the top compact row because
   they still behave like list controls rather than primary actions.
2. Preserved cloud disabled-state messaging so the relocated cloud control
   still explains why it cannot proceed when offline.
3. Renamed the relocated cloud action text to `Inbox` / `Offline` to keep the
   bottom cluster compact while preserving the existing readiness signal.

## Validation plan

1. Run `./gradlew :app:compileDebugKotlin`.
2. Run `./gradlew :app:compileDebugAndroidTestKotlin`.

## Result summary

`Fields` now places the cloud action to the left of the centered add button and
the search lens to the right, matching the requested bottom-toolbar order. The
top compact row no longer duplicates those actions, and the add action now uses
the shorter `+` label.

Validation completed with:

1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:compileDebugAndroidTestKotlin`

The first Android-test compile attempt failed only because both Gradle tasks
were launched in parallel; the sequential rerun passed.
