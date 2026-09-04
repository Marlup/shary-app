# Compose UI guidance

## Scope and authority

This file applies to `app/src/main/java/com/shary/app/ui/`. Root `AGENTS.md` and
`app/AGENTS.md` remain authoritative.

## Read first

1. `docs/ai/CODEBASE_NAVIGATION.md#Feature-to-code shortcuts`
2. `docs/design/SHARY_ELEMENT_ACTION_MAP.md` for visible actions and guards
3. `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md` for runtime boundaries
4. the plan linked from `docs/backlog/works/works-index.md` when continuing
   saved work

## Ownership shortcuts

| Concern | Primary path |
|---|---|
| Shared cards, toolbars, docks, search, cloud state | `ui/components/` |
| Fields list and editors | `ui/screens/field/` |
| Contacts list and editors | `ui/screens/user/` |
| Requests, inbox review, and request details | `ui/screens/request/` |
| Matching | `ui/screens/utils/FieldMatchingDialog.kt` plus `utils/MatchingHistoryController.kt` |
| Field/request review and send | `ui/screens/summary/` |
| Top-level navigation | `ui/screens/home/utils/AppNavigator.kt` and `Screen.kt` |
| Login and sign-up | `ui/screens/login/` and `ui/screens/logup/` |
| Settings | `ui/screens/settings/` |
| Connector UI | `ui/connector/` |
| Theme tokens | `ui/theme/` |
| Preview-only catalog | `ui/preview/` |

The product entity is **Contact**, while legacy source paths and types still use
`user` and `User`. Search both terms before concluding that a flow is absent.

## Compose boundaries

- Resolve Hilt ViewModels and navigation at route/container boundaries. Leaf
  renderers accept immutable state and explicit callbacks.
- Hoist behavior that can be tested without Android into small policies,
  reducers, controllers, or value contracts and add JVM tests.
- Use stable domain IDs for lazy keys, selection, details, and navigation. Do
  not retain filtered-list indexes as identity.
- Prefer saveable stable identifiers for user-visible transient state; do not
  save mutable domain objects wholesale.
- Keep one source of truth for selection and workflow state. Do not mirror a
  ViewModel state into an unrelated global cache without an explicit boundary.
- Preserve shared Fields/Contacts/Requests controls when a change is intended to
  apply across primary sections.

## UX and accessibility guardrails

- Keep local work available while cloud actions are unavailable; show the
  specific disabled reason.
- Preserve established single-selection actions and reversible destructive
  behavior where practical.
- Use semantic click/select actions and at least 48dp interaction targets.
- Do not rely on color alone for selection, matching, status, or navigation.
- Keep primary actions reachable with large fonts, visible IME, landscape, and
  compact widths; avoid fixed screen-height fractions for complex editors.
- Review screens must disclose the actual recipient set and channel. Do not
  describe email as encrypted or delivered merely because a composer opened.
- Do not add speculative history, verification, or security claims that the
  model and transport cannot support.

## Test routing

- Pure logic: mirror the package under `app/src/test/java/com/shary/app/`.
- Compose rendering, semantics, and callbacks: mirror the package under
  `app/src/androidTest/java/com/shary/app/ui/`.
- Cross-app, launcher, permission, or system UI: use UI Automator and follow
  `docs/testing/android-native-ui-testing.md`.
- A renderer test must not require Hilt unless the test intentionally exercises
  a Hilt route/container.

Before deleting a suspected stale composable, verify call sites in Kotlin,
previews, tests, navigation, and reflection/string references. Keep dead-code
cleanup separate from correctness changes.
