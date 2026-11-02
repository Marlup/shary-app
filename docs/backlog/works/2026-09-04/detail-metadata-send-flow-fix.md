# Detail-dialog metadata refinement + send-flow workflow fix

## Scope

Move the hand-written "Added + Tag" horizontal block from FieldDetailDialog
into core `SharyModalScaffold` primitives, improve label/value differentiation,
apply the same pattern to the Contact and Request detail dialogs, fix the
Contact-detail → Fields send flow (review CTA not shown), rename the visible
flow wording User → Contact, and turn the icon-only review button into a
labeled "Review & send" action.

## Change list

- `SharyModalScaffold.kt`: add `SharyModalMetaItem` (semi-bold muted label +
  value inside a light rounded `surfaceVariant` container) and
  `SharyModalMetaRow` (two-column row) as core shared components.
- `FieldDetailDialog`: Value/Alias use `SharyModalMetaItem`; Added + Tag use
  `SharyModalMetaRow`.
- `UserDetailDialog`: Email uses `SharyModalMetaItem`; Added + Group use
  `SharyModalMetaRow`.
- `RequestDetailDialog`: `RequestDetailValue` delegates to `SharyModalMetaItem`;
  Relative/Absolute time and Recipients/Per-recipient lists use the same
  treatment.
- `UserViewModel`: `cacheUsers` caches the passed list directly; new
  `persistSelectedUsers()` writes the cache from the synchronous
  `_selectedEmails` source.
- `UserScreen`: ON_STOP uses `persistSelectedUsers()` so the lagging derived
  `selectedUsers` flow cannot clobber a fresh contact-detail flow handoff.
- `ContactListPolicy`: add pure `resolveSelectedContacts` + JVM tests.
- `SendFlowCta`: destination `USERS` → `CONTACTS`; labels "Select Contacts" and
  "Review & send"; both docks render the review CTA as a labeled full-width
  button (`DockVisualStyle.FullWidth`).

## Design decisions

- Label/value differentiation via container + label styling (user choice).
- Review CTA labeled "Review & send" to match the Summary screen title (user
  choice).

## Validation

- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest` (full suite, incl. `SendFlowCtaTest`,
  `ContactListPolicyTest`)
- `:app:compileDebugAndroidTestKotlin` (incl. `SharyModalScaffoldTest` meta
  tests)
- `:app:lintDebug`

## Result summary

Implemented and validated. Root cause of the workflow bug: `UserScreen`
ON_STOP re-cached the lagging `combine`-derived `selectedUsers` (empty at that
moment) and clobbered the `cacheUsers(listOf(user))` handoff written by the
Contact detail flow action, so the Fields screen saw no cached contact and
offered "Select Users" instead of review. Fixed via
`persistSelectedUsers()` reading the synchronous `_selectedEmails` source.
