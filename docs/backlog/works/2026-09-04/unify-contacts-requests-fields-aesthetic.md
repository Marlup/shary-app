# Unify Contacts/Requests with the Fields aesthetic + Contact grouping tags

## Scope

Bring the newer Fields "revised prototype" treatment to Contacts and Requests,
and add a dedicated, seeded Contact grouping-tag feature inspired by Fields.
Fields already has the reference aesthetic (full-width search + overflow
toolbar, fixed sort + horizontal tag chips, labeled full-width idle bottom
buttons, shimmer skeleton).

## Change list

- Part 1: make `SharySectionToolbar` search full-width (weight 1f) beside the
  overflow action; replace Contacts/Requests icon-only idle bottom buttons with
  labeled full-width `SharySoftButton`s.
- Part 2a: add `tag: Tag` to `UserDomain`, `user.proto`, and mappers.
- Part 2b: dedicated seeded `ContactTagRepository` + `ContactTagViewModel` + DI.
- Part 2c: `UserRepository.updateTag`, `ContactSortByParameter`, `UserViewModel`
  tag filter + sort.
- Part 2d: `ContactsToolbar`, `UserCard` tag chip, `UsersScreen` wiring, editor
  tag selector, detail "Change tag".

## Plan source

[[docs/backlog/works/2026-07-15/fields-revised-prototype-implementation.md]]

## Design decisions

- Loading skeleton deliberately skipped for Contacts/Requests: their lists load
  synchronously from local DataStore (no progressive decryption like Fields), so
  a shimmer skeleton would be artificial. Existing empty-states remain.
- Contact tags are a dedicated, seeded vocabulary (Friends, Colleagues, Family,
  Vendor, Unknown) separate from Field tags, reusing the generic `Tag` type and
  `TagCodec`.
- `user.proto` gains `tag = 4`; existing contacts map to `Tag.Unknown` (no data
  migration needed).

## Result summary

Implementation completed.

- Part 1: `SharySectionToolbar` search is full-width; Contacts and Requests use
  labeled full-width `SharySoftButton`s in the idle bottom shelf.
- Part 2: `UserDomain.tag` + `user.proto.tag` + mappers; dedicated seeded
  `ContactTagRepository`/`ContactTagViewModel`; `UserRepository.updateTag`;
  `ContactSortByParameter` + `UserViewModel` filter/sort; `ContactsToolbar`,
  `UserCard` tag chip, editor tag selector, and detail "Change tag" quick-edit.

Validation passed:

- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest` (full suite)
- `:app:compileDebugAndroidTestKotlin`
- `:app:lintDebug`
- `:app:assembleDebug`

Added/updated tests: `ContactListPolicyTest`, `ContactTagRepositoryImplTest`,
`UserRepositoryImplTest` (tag round-trip + updateTag), and updated
`SharySectionControlsTest` for the full-width search toolbar.
